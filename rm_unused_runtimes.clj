(ns rm-unused-runtimes
  "Script to remove unused runtimes based on user's OS"
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]))

(defn os?
  "True iff `s` starts with `os-substr`"
  [os-substr s]
  (str/includes? (str/lower-case s) os-substr))

(def windows? (partial os? "win"))
(def mac? (partial os? "mac"))
(def linux? (partial os? "linux"))
(def unix? (partial os? "nix"))

(defn non-empty-dir?
  "True iff `dir` is a path to a non empty directory"
  [dir]
  (let [dir-file (io/file dir)]
    (and (.isDirectory dir-file)
         (-> dir-file .listFiles count pos-int?))))

(defn rm-dir
  "Recursively delete all files in a directory with path `dir`"
  [dir]
  (doseq [file (-> dir io/file .listFiles)]
    (if (non-empty-dir? file)
      (rm-dir file)
      (do (println "deleting" file)
          (io/delete-file file))))
  (println "deleting" dir)
  (try (io/delete-file dir)
       (catch java.io.IOException _
         (println "directory already deleted/does not exist"))))

(defn -main
  [& args]
  (println "args:" args)
  ;; Compile
  (println (apply sh/sh "Clojure.Compile.exe" args))
  ;; Remove unnecessary dependencies to avoid assembly errors
  (let [os-name (System/getProperty "os.name")
        subdir "target/clr/lib/SharpHook.4.0.0/runtimes/"
        architectures ["arm" "arm64" "x86" "x64"]
        windows-dirs (map (partial str subdir "win-") architectures)
        mac-dirs (map (partial str subdir "osx-") architectures)
        linux-dirs (map (partial str subdir "linux-") architectures)]
    (cond
      (windows? os-name) (doseq [dir (concat linux-dirs mac-dirs)] (rm-dir dir))
      (mac? os-name) (doseq [dir (concat windows-dirs linux-dirs)] (rm-dir dir))
      (or (linux? os-name)
          (unix? os-name)) (doseq [dir (concat windows-dirs mac-dirs)]
                             (rm-dir dir)))))
(-main)
