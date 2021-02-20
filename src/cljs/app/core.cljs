(ns app.core
  (:require [clojure.string :as string]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [emotion.core :refer [defstyled]]
            [reagent.session :as session]
            [goog.dom :as gdom]
            [reitit.frontend :as reitit]
            [accountant.core :as accountant]
            [ajax.core :as http]
            [app.state :as state]
            [app.search :as search]))


; ---------------- router


(def router
  (reitit/router
   [["/" :index]
    ["/notes"
     ["" :undefined1]
     ["/:note-id" :note-show]]
    ["/tags"
     ["" :tags-index]
     ["/:tag" :tag-results]]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(defn route-params
  "get route path parameters (if any)."
  []
  (-> :route session/get (get :route-params {})))

(defn url
  "construct url"
  [& paths]
  (string/join "/" paths))

; ---------------- components
(defn tags-index []
  (let [s (state/cursor [:tags-page] {:data {} :filter-value ""})]
    (http/GET "/api/tags"
      {:handler #(swap! s (fn [m] (assoc m :data (get % "data"))))
       :error-handler #(do (swap! s (fn [m] (assoc m :data {})))
                           (js/console.error "failed to fetch tags->notes map: " %))})
    (fn []
      (let [{:keys [data filter-value]} @s]
        (if data
          [:div
           [:input {:type "text"
                    :placeholder "type to filter tags..."
                    :value filter-value
                    :on-change #(swap! s (fn [m] (assoc m :filter-value (.. % -target -value))))}]
           [:ul {:style {:column-count "4" :padding "0 1em" :list-style-type "none"}}
            (let [filter-tags (filter (complement empty?) (string/split filter-value #"\s+"))]
              (for [[tag count] data]
                (when (or (empty? filter-tags) (some #(string/includes? tag %) filter-tags))
                  [:li {:key tag
                        :style {:padding-bottom ".9em"}} [:div
                                   {:style {:display "inline-block"
                                            :background-color "#d9d9d9"
                                            :border ".15em solid #b4b4b4"
                                            :padding ".2em 1em"
                                            :font-weight "700"}}
                                   [:a {:style {:color "#1d1d1d"
                                                :font-size ".9em"
                                                :text-decoration "none"}
                                        :href (str "/tags/" tag)} (str tag " - " count)]]])))]]

          [:div "fetching results..."])))))

(defn note-show []
  (let [note-id (:note-id (route-params))
        note (r/atom nil)]
    (http/GET (str "/api/notes/" note-id)
      {:handler #(reset! note %)
       :error-handler #(js/console.warn %)})
    (fn []
      (if-let [html @note]
        [:div {:dangerouslySetInnerHTML {:__html html}}]
        [:p "no note content"]))))

(defn tag-results []
  (let [tag (:tag (route-params))
        results (r/atom [])]
    (http/GET (url "/api/tags" tag)
      {:handler #(reset! results (get % "data"))})
    (fn []
      [:div
       (if (empty? @results)
         [:p (str "no notes tagged with '" tag "'...")]
         (map search/search-result @results))])))

(let [search-results (state/cursor [:search-results] [])]
  (defn search-page []
    (fn []
      [:div
       [:div {:style {:margin-left "2em" :margin-bottom "1em"}} [search/search search-results]]
       (when @search-results
         (map search/search-result @search-results))])))

; ---------------- route -> page component

(defn page-for [route]
  (case route
    :index #'search-page
    :tags-index #'tags-index
    :tag-results #'tag-results
    :note-show #'note-show))

(defstyled -nav-item :li
  {:display "inline"
   ":not(:first-child)" {:margin-left "2em"}})

(def nav-item (r/adapt-react-class -nav-item))

; ---------------- page mounting component
(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      (js/console.warn (session/get :route))
      [:div
       [:header
        {:style {:position "fixed" :top 0 :left 0 :right 0
                 :background-color "white"
                 :border-bottom "1px solid #000"
                 :color "#1d1d1d"}}
        [:div
         [:ul {:style {:display "inline-block"
                       :list-style-type "none"}}
          [nav-item [:a {:href "/"} "Search"]]
          [nav-item [:a {:href ""} "Search Help"]]
          [nav-item [:a {:href "/tags"} "Tags"]]]]]
       [:div {:style {:padding-top "4.5em"}}]
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
