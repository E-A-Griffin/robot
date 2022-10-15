(defproject robot "0.2.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/Liverm0r/robot"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [org.clojure/clojurescript "1.11.60"]
                 [cider/cider-nrepl "0.28.5"]
                 [cider/piggieback "0.5.1"]
                 [cljs-await "1.0.2"]
                 [org.clojars.c4605/cljs.nodejs.shell "0.1.0"]
                 [binaryage/oops "0.7.2"]]
  :plugins [[lein-npm "0.6.2"]]
  :source-paths ["src" "target/classes"]
  :npm {:dependencies [[source-map-support "0.4.0"]
                       [robotjs "0.6.0"]
                       [sleep "6.3.0"]
                       ["@napi-rs/clipboard" "1.0.1"]
                       [keycoder "1.1.1"]]}

  :repl-options {:init-ns robot.core
                 :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
  :target-path "target")
