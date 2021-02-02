(ns user
  (:require [org.rssys.context.core :as ctx]
            [app.state :refer [system]]
            [app.system :as sys]))

(defn system-stop! []
  (if (not (nil? @system))
    (do (println "stopping system...")
        (ctx/stop-all system))
    (println "system already stopped")))

(defn system-rebuild! []
  (if (not (nil? @system))
    (system-stop!)
    (do (reset! system nil)
        (sys/-build-system))))

(defn system-start! []
  (when (nil? @system)
    (system-rebuild!))
  (ctx/start-all system))
