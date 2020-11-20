(ns app.server
  (:require [ring.adapter.jetty :as ring]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]))

(defroutes routes
           (route/not-found "not found"))

(def preview-server
  (-> routes
      (wrap-file "site")
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server (ring/run-jetty #'preview-server {:port 8089 :join? false}))

(defn start
  "start development server"
  []
  (.start server))

(defn stop
  "stop development server"
  []
  (.stop server))
