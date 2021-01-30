(ns app.server
  (:require [ring.adapter.jetty :as ring]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]))

(defn welcome
  "default"
  [rq]
  {:status 200
   :body "Hello, world"
   :headers {"Content-Type" "text/plain"}})

(defroutes app-routes
  (GET "/" [] welcome)
  (route/not-found "not found"))

(def preview-server
  (-> app-routes
      (wrap-reload)
      (wrap-file "site")
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server (atom nil))

(defn start
  "start development server"
  []
  (swap! server (fn [s] (if (nil? s)
                          (do (println "starting server")
                              (ring/run-jetty #'preview-server {:port 8079
                                                                :join? false}))
                          (do (println "server already started")
                              s)))))

(defn stop
  "stop development server"
  []
  (swap! server (fn [s] (when s (do (println "stopping server...")
                                    (.stop s)
                                    nil)))))
