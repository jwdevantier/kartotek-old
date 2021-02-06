(ns app.server
  (:use [hiccup core form])
  (:require [ring.adapter.jetty :as ring]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.java.io :as io]
            ;[hiccup.core :refer [html]]
            [hiccup.page :as hp]
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
            (submit-button {:class "btn" :style "margin-left: .4em;"} "🔍 Search"))))

(defn page
  ([content] (page {} content))
  ([opts content]
   {:status (get opts :status 200)
    :headers {"Content-Type" "text/html"}
    :body (hp/html5 [:head
                     [:meta {:charset "utf-8"}]
                     (hp/include-css "/reset.css")
                     (hp/include-css "/style.css")
                     (hp/include-css "/dracula.css")
                     (hp/include-js "/highlight.pack.js")
                     [:script "hljs.initHighlightingOnLoad();"]]
                [:body content])}))

(defn navbar
  []
  [:header
   [:h3 "Notes"]
   [:div {:class "search-form"} (note-search-form)]
   [:nav
    [:a {:href "/"} "Back"]
    [:a {:href "#"} "Search Help"]
    [:a {:href "#"} "Tag Index"]
    [:a {:href "#"} "Saved Searches"]]])

#_(defn note-index
  "default"
  [rq]
  (let [notes (->> (notes/notes-paths notes/dir)
                   (map (fn [fpath]
                          (let [name (-> fpath io/file .getName)]
                            [:li [:a {:href (str "/notes/" name)} name]]))))]
    (page [:div (navbar)
           [:ul notes]])))

(defn note-index
  "default"
  [rq]
  (let [tag->num-entries (->> (db/tags->notes)
                              (map (fn [[k v]] [k (count v)]))
                              (sort-by (fn [[k v]] k)))]
    (page [:div (navbar)
           [:ul {:class "tag-list"}
            (map (fn [[tag count]]
                   [:li [:div [:a {:href (str "/tags/" tag)} (str tag " - " count)]]]) tag->num-entries)]])))

(defn note-search-result
  "show single result"
  [entry]
  [:div {:class "search-result"}
   [:a {:href (str "/notes/" (:id entry))} (:title entry)]
   [:br]
   (when-let [desc (get entry :description)]
     [:p {:class "description"} desc])
   [:div
    {:class "metadata"}
    [:span {:class "label"} "links: "]
    [:ul
     (map (fn [note-id]
            [:li
             [:a {:href (str "/notes/" note-id)} note-id]]) (get entry :links #{}))]]

   [:div
    {:class "metadata"}
    [:span {:class "label"} "tags: "]
    [:ul
     (map (fn [tag]
            [:li
             [:a {:href (str "/tags/" tag)} tag]]) (get entry :tags #{}))]]])

(defn note-search-rq
  "handle search request, show results"
  [rq]
  (let [search-query (-> rq :params (get "search-query"))
        results (db/search search-query)]
    (page [:div (navbar)
           (map note-search-result results)])))

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
    (page (layout-note note))))

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
