;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns dev.gethop.timbre.appenders.3rd-party.cloudwatch
  (:require [clojure.core.async :as as]
            [cognitect.aws.client.api :as aws]
            [dev.gethop.timbre.appenders.3rd-party.util :as util]
            [diehard.core :as dh]
            [diehard.rate-limiter :as dh.rate-limiter]
            [integrant.core :as ig]
            [java-time :as time]
            [java-time.temporal]
            [jsonista.core :as json]))

(def ^:private ^:const cloudwatch-rate-limit
  "The limit of request that can be done in 1 second per log
  stream. Cloudwatch PutLogEvents API has a maximum of 5 batch
  requests per log stream per second[1].

  [1] - https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html"
  5)

(def ^:private ^:const cloudwatch-batch-limit
  "The limit of number of log entries the appender can send in one
  request. Cloudwatch PutLogEvents API has a limit of 10000 log
  events per batch request[1].

  [1] - https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html"
  10000)

(def ^:private ^:const timbre-log-levels
  [:trace :debug :info :warn :error :fatal :report])

(defn- log-event->api-event
  [{:keys [instant ?ns-str ?line ?file vargs]}]
  {:timestamp (-> instant time/instant time/to-millis-from-epoch)
   :message (json/write-value-as-string
             {:namespace ?ns-str
              :line ?line
              :file ?file
              :vargs vargs})})

(defn- create-log-stream
  [client log-group-name log-stream-name]
  (let [result (util/->map-kebab-case
                (aws/invoke client {:op :CreateLogStream
                                    :request {:logGroupName log-group-name
                                              :logStreamName log-stream-name}}))]
    (cond
      (not (contains? result :category))
      true

      (= (:type result) "ResourceAlreadyExistsException")
      true

      :else
      false)))

(defn- send-log-events*
  [{:keys [client log-group-name] :as config}
   stream-name log-events sequence-token]
  (let [api-events (->> log-events
                        (map log-event->api-event)
                        (sort-by :timestamp)
                        (vec))
        request (cond-> {:log-group-name log-group-name
                         :log-stream-name (name stream-name)
                         :log-events api-events}
                  sequence-token (assoc :sequence-token sequence-token)

                  true
                  util/->map-camel-case)
        {:keys [next-sequence-token type category expected-sequence-token]}
        (util/->map-kebab-case
         (aws/invoke client {:op :PutLogEvents
                             :request request}))]
    (cond
      (not category)
      next-sequence-token

      (= type "DataAlreadyAcceptedException")
      expected-sequence-token

      (= type "InvalidSequenceTokenException")
      (send-log-events* config stream-name log-events expected-sequence-token)

      (= type "ResourceNotFoundException")
      (when (create-log-stream client log-group-name stream-name)
        (send-log-events* config stream-name log-events nil))

      :else
      sequence-token)))

(defn- send-log-events
  [config rate-limiter log-level sequence-token log-events]
  (dh/with-rate-limiter rate-limiter
    (send-log-events* config
                      log-level
                      log-events
                      sequence-token)))

(defn- timed-take
  ([n t ch]
   (timed-take n t ch nil))
  ([n t ch buf-or-n]
   (let [out (as/chan buf-or-n)]
     (as/go
       (loop [cnt 0]
         (when (< cnt n)
           (let [[v _] (as/alts! [ch (as/timeout t)])]
             (when (not (nil? v))
               (as/>! out v)
               (recur (inc cnt))))))
       (as/close! out))
     out)))

(defn- timed-batch-take
  [in-ch batch-size batch-timeout]
  (->> (timed-take batch-size batch-timeout in-ch (as/buffer cloudwatch-batch-limit))
       (as/reduce conj [])))

(defn- batch-log-queue
  [cb in-ch kill-ch batch-size batch-timeout]
  (as/go-loop [sequence-token nil]
    (let [[values ch] (as/alts! [(timed-batch-take in-ch batch-size batch-timeout) kill-ch])]
      (when-not (= ch kill-ch)
        (if (seq values)
          (recur (cb sequence-token values))
          (recur sequence-token))))))

(defn- init-channel!
  [{:keys [batch-config] :as config} log-level]
  (let [rate-limiter (dh.rate-limiter/rate-limiter {:rate cloudwatch-rate-limit})
        in-ch (as/chan (as/buffer cloudwatch-batch-limit))
        kill-ch (as/chan)
        cb (partial send-log-events config rate-limiter log-level)]
    (batch-log-queue cb in-ch kill-ch (:size batch-config) (:timeout batch-config))
    {:in-ch in-ch
     :kill-ch kill-ch}))

(defn- add-all-channels!
  [config]
  (->> (reduce (fn [acc log-level]
                 (assoc acc log-level (init-channel! config log-level)))
               {}
               timbre-log-levels)
       (assoc config :channels)))

(defn- add-client
  [{:keys [client-config] :as config}]
  (let [client (aws/client (merge client-config {:api :logs}))]
    (-> config
        (assoc :client client)
        (dissoc :client-config))))

(defn- send-log-fn
  [{:keys [channels] :as _config}]
  (fn [log-event]
    (let [log-level (get log-event :level)
          channel (get-in channels [log-level :in-ch])]
      (as/put! channel (select-keys log-event [:instant :?line :vargs :?file :?ns-str])))))

(defn- log-appender
  [{:keys [appender-config] :as config}]
  (let [config (-> config
                   (add-client)
                   (add-all-channels!))
        min-level (get appender-config :min-level :info)]
    {:enabled? true
     :async? true
     :rate-limit nil
     :output-fn :inherit
     :min-level (if (string? min-level)
                  (keyword min-level)
                  min-level)
     :fn (send-log-fn config)
     ::config config}))

(defn- halt!
  [{:keys [channels]}]
  (doseq [[_ {:keys [in-ch kill-ch]}] channels]
    (as/put! kill-ch [])
    (as/close! in-ch)
    (as/close! kill-ch)))

(defmethod ig/init-key :dev.gethop.timbre.appenders.3rd-party/cloudwatch
  [_ config]
  (log-appender config))

(defmethod ig/suspend-key! :dev.gethop.timbre.appenders.3rd-party/cloudwatch
  [_ _])

(defmethod ig/resume-key :dev.gethop.timbre.appenders.3rd-party/cloudwatch
  [ig-key config old-config old-impl]
  (if (and old-impl (= config old-config))
    old-impl
    (do (ig/halt-key! ig-key old-impl)
        (ig/init-key ig-key config))))

(defmethod ig/halt-key! :dev.gethop.timbre.appenders.3rd-party/cloudwatch
  [_ {:keys [::config]}]
  (halt! config))
