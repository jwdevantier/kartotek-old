(ns app.system
  (:require [org.rssys.context.core :as context]
            [app.server :as server]
            [app.filedb :as filedb]))

;; https://github.com/redstarssystems/context
;; https://github.com/redstarssystems/context-demo/
(defonce system (atom nil))

(defn -build-system
  "build system"
  []
  (context/build-context
   system
   [{:id :cfg
     :config {:x 1}
     :start-deps #{}
     :start-fn
     (fn [cfg]
       (println ":cfg component starting")
       (println cfg)
       {:web {:port 8081}
        :db {:note-dir "/home/pseud/repos/cljblog/notes"}})
     :stop-fn
     (fn [state-obj]
       (println "stopping cfg")
       (println state-obj))}
    {:id :filedb
     :start-deps #{:cfg}
     :config
     (fn [ctx]
       (-> (context/get-component-value ctx :cfg)
           :state-obj
           :db))
     :start-fn filedb/start
     :stop-fn filedb/stop}
    {:id :web
     :start-deps #{:cfg}
     :config
     (fn [ctx]
       (-> (context/get-component-value ctx :cfg)
           :state-obj
           :web))
     :start-fn
     server/start
     :stop-fn
     server/stop
     }]))
