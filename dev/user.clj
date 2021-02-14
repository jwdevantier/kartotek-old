(ns user
  (:require [org.rssys.context.core :as ctx]
            [app.state :refer [system]]
            [app.system :as sys]
            [app.env :as env]
            [app.env-impl :as envi]))

; if you update the ENV then reload server.clj

(defonce cfg-initialized? (atom false))

(defn cfg-load!
  "load configuration"
  []
  (println "load configuration...")
  (envi/refresh-from-file! env/config "dev-config.edn")
  (envi/refresh-from-env! env/config)
  true)

(defn cfg-reload!
  "reload configuration"
  []
  (reset! cfg-initialized? false)
  (cfg-load!))

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
  (swap! cfg-initialized?
         #(if %1 %1 (cfg-load!)))
  (ctx/start-all system))
