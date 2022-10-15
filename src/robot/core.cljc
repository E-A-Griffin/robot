(ns robot.core
  #?(:clj
     (:gen-class))
  (:require [clojure.string :as str]
            #?(:cljs [oops.core :refer [oget]])
            #?(:cljs [cljs.pprint :as pprint]))
  #?(:clj
     (:import (java.awt MouseInfo Robot Toolkit)
              (java.awt.datatransfer Clipboard DataFlavor
                                     StringSelection Transferable)
              (java.awt.event InputEvent KeyEvent))))

;; TODO: Figure out what to do about keys in java.awt.Robot but not in RobotJS
(defn key->key-event
  "takes in a keyword `kw` and returns it's respective key-event"
  [kw]
  (->
   (case kw
    :cmd #?(:clj "KeyEvent/VK_META"
            :cljs "command")
    :esc #?(:clj "KeyEvent/VK_ESCAPE"
            :cljs "escape")
    :back #?(:clj "KeyEvent/VK_BACK_SPACE"
             :cljs "backspace")
    :bq #?(:clj "KeyEvent/VK_BACK_QUOTE"
           :cljs nil)
    :quote #?(:clj "KeyEvent/VK_QUOTE"
              :cljs "'")
    :caps #?(:clj "KeyEvent/VK_CAPS_LOCK"
             :cljs nil)
    :ctrl #?(:clj "KeyEvent/VK_CONTROL"
             :cljs "control")
    #?(:clj
       (->> kw
            name
            .toUpperCase
            (str "KeyEvent/VK_"))
       :cljs
       (let [upper? (fn [s] (string? (re-find #"\b[A-Z]\b" s)))
             shift-char? (fn [s] (string?
                                  (#{"~" "!" "@" "#" "$" "%" "^" "&" "*" "("
                                     ")" "_" "+" "{" "}" "|" ":" "\"" "<" ">"
                                     "?"} s)))
             kw-name (name kw)]
         (cond
           ((some-fn upper? shift-char?) kw-name) [kw-name "shift"]
           :else kw-name))))
   #?(:clj (->> (re-find #"KeyEvent\/VK_[A-Z0-9\_]+")
                symbol
                eval)
      :cljs identity)))

;; FIXME:
(comment (def ^{:private true} key-events-map
  {:a     KeyEvent/VK_A :b KeyEvent/VK_B :c KeyEvent/VK_C :d KeyEvent/VK_D :e KeyEvent/VK_E
   :f     KeyEvent/VK_F :g KeyEvent/VK_G :h KeyEvent/VK_H :i KeyEvent/VK_I :j KeyEvent/VK_J
   :k     KeyEvent/VK_K :l KeyEvent/VK_L :m KeyEvent/VK_M :n KeyEvent/VK_N :o KeyEvent/VK_O
   :p     KeyEvent/VK_P :q KeyEvent/VK_Q :r KeyEvent/VK_R :s KeyEvent/VK_S :t KeyEvent/VK_T
   :u     KeyEvent/VK_U :v KeyEvent/VK_V :w KeyEvent/VK_W :x KeyEvent/VK_X :y KeyEvent/VK_Y
   :z     KeyEvent/VK_Z
   :1     KeyEvent/VK_1 :2 KeyEvent/VK_2 :3 KeyEvent/VK_3 :4 KeyEvent/VK_4 :5 KeyEvent/VK_5
   :6     KeyEvent/VK_6 :7 KeyEvent/VK_7 :8 KeyEvent/VK_8 :9 KeyEvent/VK_9 :0 KeyEvent/VK_0
   :cmd   KeyEvent/VK_META :meta KeyEvent/VK_META
   :shift KeyEvent/VK_SHIFT
   :alt   KeyEvent/VK_ALT
   :esc   KeyEvent/VK_ESCAPE
   :enter KeyEvent/VK_ENTER
   :back  KeyEvent/VK_BACK_SPACE
   :bq    KeyEvent/VK_BACK_QUOTE                            ; back quote
   :quote KeyEvent/VK_QUOTE
   :tab   KeyEvent/VK_TAB
   :caps  KeyEvent/VK_CAPS_LOCK
   :ctrl  KeyEvent/VK_CONTROL
   :space KeyEvent/VK_SPACE
   :f1    KeyEvent/VK_F1 :f2 KeyEvent/VK_F2 :f3 KeyEvent/VK_F3 :f4 KeyEvent/VK_F4
   :f5    KeyEvent/VK_F5 :f6 KeyEvent/VK_F6 :f7 KeyEvent/VK_F7 :f8 KeyEvent/VK_F8
   :f9    KeyEvent/VK_F9 :f10 KeyEvent/VK_F10 :f11 KeyEvent/VK_F11 :f12 KeyEvent/VK_F12
   :left  KeyEvent/VK_LEFT :right KeyEvent/VK_R :up KeyEvent/VK_UP :down KeyEvent/VK_DOWN}))

;; KEYBOARD-API
#?(:clj (def ^Robot robot (Robot.))
   :cljs (def robot (js/require "robotjs")))

#?(:cljs (def ^:private sleep-module (js/require "sleep")))

#?(:cljs (def ^:private keycoder (js/require "keycoder")))

(defn sleep [millis]
  #?(:clj (.delay robot millis)
     :cljs (.msleep sleep-module millis)))

(defn- keys->key-events [keys]
  (map key->key-event keys))

(defn- delay!
  "`delay` is in milliseconds"
  [robot delay]
  #?(:clj (.delay robot delay)
     :cljs (.setKeyboardDelay robot delay)))

(defn- key-toggle!
  "`key-event` is a number, string, or a 2-element vector of a string and a
  modifier (cljs-only). `up?` is true if key is being pressed down, and false
  otherwise"
  [robot key-event up?]
  #?(:clj (if up? (.keyRelease robot key-event) (.keyPress robot key-event))
     :cljs (let [up-or-down (if up? "up" "down")]
             (if (vector? key-event)
               (.keyToggle robot (first key-event) up-or-down
                           (second key-event))
               (.keyToggle robot key-event up-or-down)))))

(defn- mouse-toggle!
  "`mouse-button` is either \"left\", \"right\", or \"middle\". `up?` is true
  if key is being pressed down, and false otherwise"
  [robot mouse-button up?]
  (let [mouse-event #?(:clj
                       (case mouse-button
                         "left" InputEvent/BUTTON1_DOWN_MASK
                         "middle" InputEvent/BUTTON2_DOWN_MASK
                         "right" InputEvent/BUTTON3_DOWN_MASK
                         (throw (ex-info "Invalid button event passed"
                                         {:mouse-button mouse-button})))
                       :cljs mouse-button)]
        #?(:clj (if up?
                  (.mouseRelease robot mouse-event)
                  (.mousePress robot mouse-event))
           :cljs (if up?
                   (.keyToggle robot mouse-event "up")
                   (.keyToggle robot mouse-event "down")))))

(defn type! [key & [delay]]
  (let [key (if (number? key) key (key->key-event key))
        delay (or delay 40)]
    (doto robot
      (delay! delay)
      (key-toggle! key false)
      (key-toggle! key true)
      ;; Reset keyboard delay
      #?(:cljs (.setKeyboardDelay 10)))))

(defn hot-keys! "takes seq of ints (KeyEvent) or :keys"
  [keys & [delay-between-press delay-before-release]]
  (let [keys (if (number? (first keys)) keys (keys->key-events keys))]
    (doseq [key keys]
      (doto robot
        (key-toggle! key false)
        (delay! (or delay-between-press 10))))
    (delay! robot (or delay-before-release 100))
    (doseq [key (reverse keys)] (key-toggle! robot key true))))

(defn type-text! [^String s & [delay-before-press delay-before-release]]
  (let [delay-before-press (or delay-before-press 70)
        delay-before-release (or delay-before-release 0)]
  #?(:clj (doseq [byte (.getBytes s)
                  :let [code (int byte)
                        code (if (< 96 code 123) (- code 32) code)]]
            (doto robot
              (delay! delay-before-press)
              (key-toggle! code false)
              (delay! delay-before-release )
              (key-toggle! code true)))
     :cljs (doseq [cur-key s]
             (doto robot
               (delay! delay-before-press)
               (key-toggle! cur-key false)
               (delay! delay-before-release)
               (key-toggle! cur-key true))))))

;; MOUSE

(defn mouse-click! [& [delay]]
  (doto robot
    (mouse-toggle! "left" false)
    (delay! (or delay 70))
    (mouse-toggle! "left" true)))

(defn mouse-pos "returns mouse position [x, y]" []
  #?(:clj (let [mouse-info (.. MouseInfo getPointerInfo getLocation)]
            [(. mouse-info x) (. mouse-info y)])
     :cljs (let [mouse-pos (. robot getMousePos)]
             [(oget mouse-pos :x) (oget mouse-pos :y)])))

(defn mouse-move!
  ([[x y]] (mouse-move! x y))
  ([x y] #?(:clj (.mouseMove robot x y)
            :cljs (.moveMouse robot x y))))

(defn scroll! [i]
  #?(:clj (.mouseWheel ^Robot robot i)
     :cljs (.scrollMouse robot 0 i)))

(defn pixel-color [x y] (.getPixelColor robot x y))

;; CLIPBOARD

#?(:clj  (def ^Clipboard clipboard
           (.. Toolkit getDefaultToolkit getSystemClipboard))
   :cljs (def clipboard (let [clipboard-class (-> "@napi-rs/clipboard"
                                                  js/require
                                                  (oget :Clipboard))]
                          (new clipboard-class))))

(defn clipboard-put! [^String s]
  #?(:clj  (.setContents clipboard (StringSelection. s) nil)
     :cljs (.setText clipboard s)))

(defn clipboard-get-string "returns string from buffer or nil" []
  #?(:clj
     (let [^Transferable content (.getContents clipboard nil)
           has-text              (and (some? content)
                                      (.isDataFlavorSupported
                                       content
                                       DataFlavor/stringFlavor))]
       (when has-text
         (try
           (.getTransferData content DataFlavor/stringFlavor)
           (catch Exception e (.printStackTrace e)))))
     :cljs (.getText clipboard)))

;; INFO

(defn get-key-name [i]
  #?(:clj  (KeyEvent/getKeyText i)
     :cljs (try (let [keycode-key (. keycoder fromKeyCode i)]
                  (if (first (.-names keycode-key))
                    (first (.-names keycode-key))
                    (.-character keycode-key)))
                (catch js/Error _
                  (str "Unknown keyCode: 0x" (pprint/cl-format nil "~x" i))))))

;; NOTE: If using CIDER for a [[cljs]] REPL, at time of writing, the output
;; for [[(sorted-map ...)]] is printed unsorted, an easy fix for a sorted
;; view of keyboard is [[(prn (get-my-keyboard))]] or
;; [[(seq (get-my-keyboard))]]
(defn get-my-keyboard []
  (into (sorted-map)
        ;; [[cljs]] considerably slower than [[clj]] so limit range based on
        ;; target
        (for [i #?(:clj  (range 100000)
                   :cljs (range 3000))
              :let [text (get-key-name i)]
              :when (not (str/starts-with? text "Unknown keyCode:"))]
          [i text])))
