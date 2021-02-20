(ns app.server
  (:use [hiccup core form])
  (:require [ring.adapter.jetty :as jetty]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [ring.util.response :refer [file-response resource-response]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.file :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.not-modified :refer :all]
            [ring.util.response :refer [response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.ring :as rr]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.middleware.parameters :as parameters]
            [hiccup.page :as hp]
            [app.notes :as notes]
            [app.filedb :as db]
            [app.state :as state]
            [app.env :as env]
            [app.api.routes :as api]))

(defn note-search-form
  ""
  ([] (note-search-form nil))
  ([value]
   (form-to {:enctype "multipart/form-data"}
            [:post "/old/search"]
            (text-field
             {:placeholder "type query here"}
             "search-query"
             value)
            (submit-button {:class "btn" :style "margin-left: .4em;"} "🔍 Search"))))

(defn navbar
  []
  [:header
   [:h3 "Notes"]
   [:div {:id "search-form" :class "search-form"}]
   [:nav
    [:a {:href "/old/"} "Tag Index"]
    [:a {:href "/old/search/help"} "Search Help"]
    [:a {:href "#"} "Saved Searches"]]])

(defn page
  ([content] (page {} content))
  ([{:keys [status navbar?] :or {status 200 navbar? true}} content]
   {:status status
    :headers {"Content-Type" "text/html"}
    :body (hp/html5 [:head
                     [:meta {:charset "utf-8"}]
                     (hp/include-css "/assets/reset.css")
                     (hp/include-css "/assets/style.css")
                     (hp/include-css "/assets/highlight.theme.css")
                     (hp/include-js "/assets/highlight.pack.js")
                     [:script {:type "text/javascript"} "hljs.initHighlightingOnLoad();"]]
                    [:body (when navbar? (navbar)) content (hp/include-js "/js/main.js")
                     [:script {:type "text/javascript"} "mount('search-form', 'search');"]])}))

(defn tag-index
  "show 'index' sorted tags"
  [rq]
  (let [tag->num-entries (->> (db/tags->notes)
                              (map (fn [[k v]] [k (count v)]))
                              (sort-by (fn [[k v]] k)))]
    (pp/pprint rq)
    (page [:div
           [:ul {:class "tag-list"}
            (map (fn [[tag count]]
                   [:li [:div [:a {:href (str "/old/tags/" tag)} (str tag " - " count)]]]) tag->num-entries)]])))

(defn note-search-result
  "show single result"
  [{:keys [id title description] :as entry}]
  [:div {:class "search-result"}
   [:a {:href (str "/old/notes/" (:id entry))} (if (empty? title) id title)]
   [:br]
   (when-let [desc (get entry :description)]
     [:p {:class "description"} desc])
   [:div
    {:class "metadata"}
    [:span {:class "label"} "links: "]
    [:ul
     (map (fn [note-id]
            [:li
             [:a {:href (str "/old/notes/" note-id)} note-id]]) (get entry :links #{}))]]

   [:div
    {:class "metadata"}
    [:span {:class "label"} "tags: "]
    [:ul
     (map (fn [tag]
            [:li
             [:a {:href (str "/old/tags/" tag)} tag]]) (get entry :tags #{}))]]])

(defn search-rq
  "handle search request, show results"
  [rq]
  (let [search-query (-> rq :params (get "search-query"))
        results (db/search search-query)]
    (page [:div
           (if (empty? results)
             [:p "no results found for your query..."]
             (map note-search-result results))])))

(defn tag-show-docs
  "show list of results for given tag"
  [rq]
  (let [tag (-> rq :path-params :tag)
        results (db/filter-db (fn [e] (contains? (get e :tags #{}) tag)))]
    (page [:div
           [:h2 (str "Showing documents tagged '" tag "'...")]
           (map note-search-result results)])))

(defn layout-note
  [{:keys [content]}]
  [:div
   [:article {:class "note"} content]])

(defn note-show
  "default"
  [rq]
  (let [id (-> rq :path-params :id)
        note (-> (state/get-config)
                 (get-in [:db :note-dir])
                 (java.io.File. id)
                 slurp
                 notes/parse-md-doc)]
    (page (layout-note note))))

(defn search-help
  "show page explaining search syntax"
  [rq]
  (page [:div [:article {:class "note"}
               (notes/md->hiccup (slurp (io/resource "search-help.md")))]]))

(defn file-or-resource-route
  ""
  ([root-dir] (file-or-resource-route root-dir {}))
  ([root-dir {:keys [label] :or {label :item}}]
   (fn [rq]
     (let [item (-> rq :path-params (get label))]
       (or (file-response item {:root root-dir})
           (resource-response (->> item
                                   (java.io.File. root-dir)
                                   .getPath)))))))

(defn asset-main-js [rq]
  (or (file-response "main.js" {:root "assets/js"})
      (resource-response "assets/js/main.js")))

(defn default-handler [rq]
  {:status 404
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body {:error {:message "route not found"}}})

(def routes-swagger
  [["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "Kartotek API"
                            :description "Kartotek system API"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/api/docs/*"
    {:get (swagger-ui/create-swagger-ui-handler)}]

   ["/assets/"
    [":file"
     {:get {:no-doc true
            :handler (file-or-resource-route "assets" {:label :file})}}]]])

(defn get-index-page [rq]
  (-> (or (file-response "index.html" {:root "assets"})
          (resource-response (.getPath (java.io.File. "assets" "index.html"))))
                ; honestly no idea why I have to set this, middleware should've handled it...
      (assoc-in [:headers "Content-type"] "text/html")))

(def routes-app
  [["/"
    {:get {:no-doc true
           :handler get-index-page}}]
   ["/notes/:id"
    {:get {:no-doc true
           :handler get-index-page}}]
   ["/tags"
    {:get {:no-doc true
           :handler get-index-page}}]
   ["/tags/:id"
    {:get {:no-doc true
           :handler get-index-page}}]
   ["/js/:file"
    {:get {:no-doc true
           :handler (file-or-resource-route "assets/js" {:label :file})}}]
   ["/old/"
    [""
     {:get {:no-doc true
            :handler tag-index}}]
    ["search"
     {:post {:no-doc true
             :handler search-rq}}]
    ["search/help"
     {:get {:no-doc true
            :handler search-help}}]
    ["notes/:id"
     {:get {:no-doc true
            :handler note-show}}]
    ["tags/"
     {:get {:no-doc true
            :handler tag-index}}]
    ["tags/:tag"
     {:get {:no-doc true
            :handler tag-show-docs}}]]])

(def routes-dev
  [["/js/cljs-runtime/:file"
    {:get {:no-doc true
           :handler (file-or-resource-route "assets/js/cljs-runtime" {:label :file})}}]])

(def routes-all
  (into [] (concat routes-swagger routes-app (when (env/dev?)
                                               routes-dev) api/routes)))

(def app-routes
  (rr/ring-handler
   (rr/router
    routes-all
    ;opts
    {:data {:middleware [swagger/swagger-feature
                         parameters/parameters-middleware]}})
   default-handler))

(def preview-server
  (-> app-routes
      (wrap-cors :access-control-allow-origin [#"http://localhost(.*)?"]
                 :access-control-allow-methods [:get :put :patch :post :delete])
      wrap-reload
      wrap-json-response
      wrap-json-body
      wrap-multipart-params
      wrap-content-type
      wrap-not-modified))

(defn start
  "start server component"
  [{:keys [port]}]
  (println "starting server...")
  (jetty/run-jetty #'preview-server {:port port :join? false}))

(defn stop
  "stop server component"
  [state-obj]
  (println "stopping server...")
  (.stop state-obj))
