(ns rm-unused-runtimes
  "Script to remove unused runtimes based on user's OS"
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]))

;; OS predicates
(defn os?
  "True iff `s` includes `os-substr`"
  [os-substr s]
  (str/includes? (str/lower-case s) os-substr))

(def windows? (partial os? "win"))
(def mac? (partial os? "mac"))
(def linux? (partial os? "linux"))
(def unix? (partial os? "nix"))


;; Architecture predicates
(def arch? os?)

(def arm32? (partial arch? "arm"))

(defn arm64?
  [s]
  (and (arm32? s)
       (arch? "64" s)))

(def x86? (some-fn (partial arch? "i386")
                   (partial arch? "x86")))

(def x64? (partial arch? "amd64"))


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

(defn cp
  "Copy file at `src` into `dest`"
  [src dest]
  (spit dest (slurp src)))

(defn cp-native-code
  "Copies native code to proper directory based on `os` and `arch`"
  [os arch]
  (let [os+arch-path (str (case os
                            :win   "win"
                            :mac   "osx"
                            :linux "linux"
                            :unix  "linux")
                          "-"
                          (cond
                            (arm64? arch) "arm64"
                            (arm32? arch) "arm"
                            (x86? arch)   "x86"
                            (x64? arch)   "x64"))
        native-file (-> "target/clr/lib/SharpHook.4.0.0/runtimes/"
                        (str os+arch-path "/native/")
                        .listFiles
                        first)
        native-file-name (.getName native-file)
        native-file-path (.getPath native-file)]
    (cp (str "target/clr/lib/SharpHook.4.0.0/" native-file-name)
        native-file-path)))

(defn -main
  [& args]
  (println "args:" args)
  ;; Compile
  (println "Compilation results:"
   (if (->> args (remove nil?) count zero?)
     (sh/sh "Clojure.Compile.exe")
     (apply sh/sh "Clojure.Compile.exe" args)))
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
(-main *command-line-args*)
