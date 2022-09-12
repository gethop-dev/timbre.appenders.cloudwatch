;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns dev.gethop.timbre.appenders.module.cloudwatch
  (:require [integrant.core :as ig]))

(defmethod ig/prep-key :dev.gethop.timbre.appenders.module/cloudwatch [_ options]
  (assoc options ::requires (ig/ref :duct.module/logging)))

(defmethod ig/init-key :dev.gethop.timbre.appenders.module/cloudwatch [_ options]
  (fn [config]
    ;; Because the original configuration added by duct.module/logging
    ;; uses the ^:displace tag for the appenders[1][2], we can't use the
    ;; usual duct.core/merge-configs function. We want to keep the
    ;; default appenders and add our own. This is impossible with the
    ;; ^:displace tag, so we simply assoc the values.
    ;;
    ;; [1] - https://github.com/duct-framework/module.logging/blob/0.5.0/src/duct/module/logging.clj#L12
    ;; [2] - https://github.com/weavejester/meta-merge#displace
    (-> config
        (assoc :dev.gethop.timbre.appenders.3rd-party/cloudwatch options)
        (assoc-in [:duct.logger/timbre :appenders :dev.gethop.timbre.appenders.3rd-party/cloudwatch]
                  (ig/ref :dev.gethop.timbre.appenders.3rd-party/cloudwatch)))))
