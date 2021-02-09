(ns app.state
  (:require [org.rssys.context.core :as ctx]))

(defonce system (atom nil))

(defn get-config
  "read config"
  []
  (-> system (ctx/get-component :cfg) (get :state-obj nil)))
