(ns app.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [reagent.session :as session]
            [goog.dom :as gdom]
            [reitit.frontend :as reitit]
            [accountant.core :as accountant]
            [ajax.core :as http]))


; ---------------- router
(def router
  (reitit/router
   [["/" :index]
    ["/tags"
     ["" :tags-index]
     ["/:tag-id" :tag-results]]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

; ---------------- components

(defn one []
  [:p "hello from one"])

(defn two []
  [:p "hello from two"])

(defn three []
  [:p "hello from three"])


; ---------------- route -> page component
(defn page-for [route]
  (case route
    :index #'one
    :tags-index #'two
    :tag-results #'three))

; ---------------- page mounting component
(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      (js/console.warn (session/get :route))
      [:div
       [:header
        {:style {:position "fixed" :top 0 :left 0 :right 0
                 :background-color "#1573B9"
                 :color "white"}}
        [:div
         [:span "🔍"]
         [:span "Tags"]]
        "acknowledge me"]
       [page]])))

; ---------------- 

; ---------------- lifecycle
(defn -start []
  ; called after reload
  (rdom/render [current-page] (gdom/getElement "app")))

(defn ^:export init []
  ; called only once on app load
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (js/console.warn "path" path)
      (let [match (reitit/match-by-path router path)
            current-page (-> match :data :name)
            route-params (:path-params match)]
        (js/console.warn "ACCOUNTANT NAV-HANDLER RUNNING")
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (-start))

(defn -stop []
  ; called before any code is reloaded, see shadow-cljs :before-load
  (js/console.warn "reloading."))