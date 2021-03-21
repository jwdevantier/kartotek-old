(ns app.system
  (:require [org.rssys.context.core :as context]
            [taoensso.timbre :as timbre]
            [app.server :as server]
            [app.filedb :as filedb]
            [app.state :refer [system]]
            [app.utils :refer [deep-merge]]))

(defn read-config
  "blend config with defaults"
  []
  (let [cfg (deep-merge
   ; defaults
             {:web {:port 8081}
              :db {:note-dir "notes"}}
             (try (read-string (try (slurp "config.edn")
                                    (catch Exception e
                                      (do (timbre/warn "config.edn not found, using default config...")
                                          "{}"))))
                  (catch Exception e
                    (do (timbre/error e "syntax error in config file")
                        {}))))]
    (timbre/info "using config:" cfg)
    cfg))

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
       (read-config))
     :stop-fn
     (fn [state-obj]
       (timbre/info "cfg @ time of stopping:")
       (timbre/info state-obj))}
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
     server/stop}]))
