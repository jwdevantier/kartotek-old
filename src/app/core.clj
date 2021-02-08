(ns app.core
  (:gen-class)
  (:require [org.rssys.context.core :as ctx]
            [app.system :as sys]
            [app.state :refer [system]]
            [app.utils :refer [atexit]]))


(defn -main
  "program entrypoint"
  [& args]
  (sys/-build-system)
  (atexit (fn [] (ctx/stop-all system)))
  (ctx/start-all system))
