;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns dev.gethop.timbre.appenders.3rd-party.util
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(defn ->map-kebab-case
  "Transforms all map keys to kebab-case keywords."
  [m]
  (cske/transform-keys csk/->kebab-case-keyword m))

(defn ->map-camel-case
  "Transforms all map keys to camelCase keywords."
  [m]
  (cske/transform-keys csk/->camelCaseKeyword m))
