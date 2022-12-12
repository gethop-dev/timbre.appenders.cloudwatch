(defproject dev.gethop/timbre.appenders.cloudwatch "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.async "1.5.648"]
                 [diehard "0.11.6"]
                 [integrant "0.8.0"]
                 [com.cognitect.aws/api "0.8.630"]
                 [com.cognitect.aws/endpoints "1.1.12.358"]
                 [com.cognitect.aws/logs "825.2.1262.0"]
                 [clojure.java-time "1.1.0"]
                 [metosin/jsonista "0.3.7"]
                 [camel-snake-kebab "0.4.3"]]
  :repl-options {:init-ns dev.gethop.timbre.appenders.3rd-party.cloudwatch}
  :deploy-repositories [["snapshots" {:sign-releases false
                                      :url "https://clojars.org/repo"
                                      :username :env/CLOJARS_USERNAME
                                      :password :env/CLOJARS_PASSWORD}]
                        ["releases" {:sign-releases false
                                     :url "https://clojars.org/repo"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD}]]
  :profiles
  {:dev [:project/dev :profiles/dev]
   :repl {:repl-options {:host "0.0.0.0"
                         :port 4002}}
   :profiles/dev {}
   :project/dev {:plugins [[jonase/eastwood "1.3.0"]
                           [lein-cljfmt "0.9.0"]]
                 :eastwood {:linters [:all]
                            :source-paths ["src"]
                            :test-paths ["test"]
                            :exclude-linters [:keyword-typos
                                              :boxed-math
                                              :non-clojure-file
                                              :unused-namespaces
                                              :performance]
                            :debug [:progress :time]}}})
