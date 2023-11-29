;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns dev.gethop.timbre.appenders.3rd-party.util
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [java-time :as jt]
            [java-time.temporal]))

(defn ->map-kebab-case
  "Transforms all map keys to kebab-case keywords."
  [m]
  (cske/transform-keys csk/->kebab-case-keyword m))

(defn ->map-camel-case
  "Transforms all map keys to camelCase keywords."
  [m]
  (cske/transform-keys csk/->camelCaseKeyword m))

(defmacro print-error
  "Prints an error with the received data."
  [invoke-result]
  (let [line-number (:line (meta &form))]
    `(let [timestamp# (jt/instant)
           formatted-result# (with-out-str (clojure.pprint/pprint ~invoke-result))]
       (println (format "%s ERROR [%s:%d] - %s" (str timestamp#) (ns-name ~*ns*) ~line-number formatted-result#)))))
