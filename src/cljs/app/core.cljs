(ns app.core
  (:require [clojure.string :as string]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [reagent.session :as session]
            [goog.dom :as gdom]
            [ajax.core :as http]
            [reitit.frontend :as reitit]
            [accountant.core :as accountant]
            ))

(defn url [& paths]
  (string/join "/" paths))

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
(defn atom-input [value]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn shared-state []
  (let [val (r/atom "foo")]
    (fn []
      [:div
       [:p " the value is now: " @val]
       [:p "Change it here: " [atom-input val]]])))

(defn hello-world []
  [:div
   [:h1 "hello, from react"]
   [:div
    [shared-state]
    [:button#fetch {:on-click (fn []
                                (http/GET (url "/api" "tags")))} "fetch"]]])

(defn tags-index []
  [:h1 "tags index"])

(defn tag-results []
  [:h1 "tag results"])

; ---------------- route -> page component
(defn page-for [route]
  (case route
    :index #'hello-world
    :tags-index #'tags-index
    :tag-results #'tag-results))

; ---------------- page mounting component
(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      (js/console.warn (session/get :route))
      [:div
       [:header "hello"]
       [page]
       ])))

; ---------------- hot reloading

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
  (-start)
  #_(js/console.log {:hello "world!!"
                   :is-map true}))

(defn -stop []
  ; called before any code is reloaded, see shadow-cljs :before-load
  (js/console.warn "reloading."))
