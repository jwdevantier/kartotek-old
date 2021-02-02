(ns app.server
  (:use [hiccup core form])
  (:require [ring.adapter.jetty :as ring]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            ;[hiccup.core :refer [html]]
            [app.core :as core]
            [app.notes :as notes]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]))


(defn note-search-form
  ""
  []
  (form-to {:enctype "multipart/form-data"}
    [:post "/"]
   (text-field "search-query")
   (submit-button {:class "btn" } "search")))

(defn note-index
  "default"
  [rq]
  (let [notes (->> (notes/notes-paths notes/dir)
                   (map (fn [fpath]
                          (let [name (-> fpath io/file .getName)]
                            [:li [:a {:href (str "/notes/" name)} name]]))))]
    (print notes)
    {:status 200
     :body (html [:div (note-search-form) [:ul notes]])
     :headers {"Content-Type" "text/html"}}))

(defn note-search-rq
  "handle search request, show results"
  [rq]
  (let [search-query (-> rq :params (get "search-query"))]
    {:status 200
      :body (html [:div (str "you searched for '" search-query "'")])
      :headers {"Content-Type" "text/html"}}))


;; TODO: ZERO error-handling here..
;; (garbled contents, no file..)
(defn note-show
  "default"
  [rq]
  (let [id (-> rq :params :id)
        note (notes/parse-md-doc (slurp (str notes/dir "/" id)))]
    {:status 200
     :body (html (:content note))
     :headers {"Content-Type" "text/html"}}))


(defroutes app-routes
  (GET "/" [] note-index)
  (POST "/" [] note-search-rq)
  (GET "/notes/:id" [rq id] note-show)
  (route/not-found "not found"))

(def preview-server
  (-> app-routes
      (wrap-reload)
      wrap-multipart-params
      (wrap-file "site")
      (wrap-content-type)
      (wrap-not-modified)))

(defn start
  "start server component"
  [{:keys [port]}]
  (println "starting server...")
  (ring/run-jetty #'preview-server {:port port :join? false}))

(defn stop
  "stop server component"
  [state-obj]
  (println "stopping server...")
  (.stop state-obj))
