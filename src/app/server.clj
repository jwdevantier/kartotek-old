(ns app.server
  (:use [hiccup core form])
  (:require [ring.adapter.jetty :as ring]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            ;[hiccup.core :refer [html]]
            [app.core :as core]
            [app.notes :as notes]
            [app.filedb :as db]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]
            [clojure.string :as string]))

(defn note-search-form
  ""
  ([] (note-search-form nil))
  ([value]
   (form-to {:enctype "multipart/form-data"}
            [:post "/"]
            (text-field
             {:placeholder "type query here"}
             "search-query"
             value)
            (submit-button {:class "btn" :style "margin-left: .4em;"} "search"))))

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

(defn note-search-result
  "show single result"
  [entry]
  [:div {:style "margin-top: 1.5em; margin-bottom: 2em"}
   [:a {:href (str "/notes/" (:id entry))} (:title entry)]
   [:br]
   (when-let [d (get entry :description)]
     [:p {:style "margin-top: .2em; margin-bottom: .4em;"}d])
   [:div
    {:style "font-size: .9em;"}
    [:span {:style "color: #909090; font-size: -1;"} "links: "]
    [:ul
     {:style "display: inline-block; list-style-type: none; padding: 0; margin: 0"}
     (map (fn [note-id]
            [:li
             {:style "display: inline"}
             [:a {:style "margin-left: .2em; margin-right: .2em"
                  :href (str "/notes/" note-id)} note-id]]) (get entry :links #{}))]]

   [:div
    {:style "font-size: 0.9em;"}
    [:span {:style "color: #909090;"} "tags: "]
    [:ul
     {:style "display: inline-block; list-style-type: none; padding: 0; margin: 0"}
     (map (fn [tag]
            [:li
             {:style "display: inline;"}
             [:a {:style "margin-left: .2em; margin-right: .2em;"
                  :href (str "/tags/" tag)} tag]]) (get entry :tags #{}))]]])

(defn note-search-rq
  "handle search request, show results"
  [rq]
  (let [search-query (-> rq :params (get "search-query"))
        results (db/search search-query)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body
     (html [:div (note-search-form search-query)
            [:hr]
            (map note-search-result results)])}))


;; TODO: ZERO error-handling here..
;; (garbled contents, no file..)

(defn navbar
  []
  [:header
   [:h3 "Notes"]
   [:div {:class "search-form"} (note-search-form)]
   [:nav [:a {:href "/"} "Back"]]])

(defn layout-note
  [{:keys [content]}]
  [:div
   (navbar)
   [:article {:class "note"} content]])


(defn note-show
  "default"
  [rq]
  (let [id (-> rq :params :id)
        note (notes/parse-md-doc (slurp (str notes/dir "/" id)))]
    {:status 200
     :body (html (layout-note note))
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
