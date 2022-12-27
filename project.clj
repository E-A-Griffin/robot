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
  :plugins [[lein-npm "0.6.2"]
            [lein-clr "0.2.2"]]
  :source-paths ["src" "target/classes"]
  :npm {:dependencies [[source-map-support "0.4.0"]
                       [robotjs "0.6.0"]
                       [sleep "6.3.0"]
                       ["@napi-rs/clipboard" "1.0.1"]
                       [keycoder "1.1.1"]]}

  :clr {:cmd-templates  {:clj-dep   [["./target/clr/clj" %1]]
                         :clj-url   "https://sourceforge.net/projects/clojureclr/files/clojure-clr-1.10.0-Release-net4.6.1.zip/download"
                         :clj-zip   "clojure-clr-1.10.0-Release-net4.6.1.zip"
                         :curl      ["curl" "--insecure" "-f" "-L" "-o" %1 %2]
                         :nuget-ver [[*PATH "nuget"] "install" %1 "-Version" %2]
                         :unzip     ["unzip" "-d" %1 %2]}
        :nuget-any     ["nuget" "install" %1 "-Version" %2]
        :deps-cmds     [[:curl  :clj-zip :clj-url]
                        [:unzip "../clj" :clj-zip]
                        [:nuget-ver "SharpHook" "4.0.0"]]
        :main-cmd      [[*PATH "Clojure.Main.exe"]]
        :compile-cmd   [[*PATH "clj"] "-M" "rm_unused_runtimes.clj"]}

  :repl-options {:init-ns robot.core
                 :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
  :target-path "target")
