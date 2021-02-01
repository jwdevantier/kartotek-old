(ns user
  (:require [app.system :as sys]
            [org.rssys.context.core :as ctx]))

; TODO: write fns to start and stop system, and build if necessary


;; TODO: if not nil, stop and reset!
(defn system-rebuild []
  (reset! sys/system nil)
  (sys/-build-system))

(defn system-start []
  (when (nil? @sys/system)
    (system-rebuild))
  (ctx/start-all sys/system))

(defn hello-world [] (println "hullo"))
