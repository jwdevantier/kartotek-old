(ns user
  (:require [app.system :as sys]
            [org.rssys.context.core :as ctx]))


(defn system-rebuild! []
  (if (not (nil? @sys/system))
    (system-stop)
    (do (reset! sys/system nil)
        (sys/-build-system))))

(defn system-start! []
  (when (nil? @sys/system)
    (system-rebuild!))
  (ctx/start-all sys/system))

(defn system-stop! []
  (if (not (nil? @sys/system))
    (do (println "stopping system...")
        (ctx/stop-all sys/system))
    (println "system already stopped")))

