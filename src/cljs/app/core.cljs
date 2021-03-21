(ns app.core
  (:require [clojure.string :as string]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [emotion.core :refer [defstyled]]
            [reagent.session :as session]
            [goog.dom :as gdom]
            [goog.events.KeyCodes :as key]
            [reitit.frontend :as reitit]
            [accountant.core :as accountant]
            [ajax.core :as http]
            [app.state :as state]
            [app.search :as search]
            [app.components.base :refer [InputField SearchDialog Tag]]
            [app.components.keys :refer [with-keys with-shortcuts]]
            [app.components.modal :as modal]
            [app.components.note :as note]))


; ---------------- router


(def router
  (reitit/router
   [["/" :index]
    ["/notes"
     ["" :undefined1]
     ["/:note-id" :note-show]]
    ["/tags"
     ["" :tags-index]
     ["/:tag" :tag-results]]
    ["/search"
     ["/help" :search-help]]]))

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
           {:style {:margin-left "2em"}}
           [:input {:type "text"
                    :placeholder "type to filter tags..."
                    :value filter-value
                    :on-change #(swap! s (fn [m] (assoc m :filter-value (.. % -target -value))))}]
           [:ul {:style {:column-count "4" :padding "0" :list-style-type "none"}}
            (let [filter-tags (filter (complement empty?) (string/split filter-value #"\s+"))]
              (for [[tag count] data]
                (when (or (empty? filter-tags) (some #(string/includes? tag %) filter-tags))
                  [:li {:key tag
                        :style {:padding-bottom ".9em"}} [Tag {:href (str "/tags/" tag)} (str tag " - " count)]])))]]

          [:div "fetching results..."])))))

(defn note-show []
  (let [note-id (:note-id (route-params))
        note (r/atom nil)]
    (http/GET (str "/api/notes/" note-id)
      {:handler #(reset! note %)
       :error-handler #(js/console.warn %)})
    (fn []
      (if-let [html @note]
        [note/render html]
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

; ---------------- route -> page component

(defn page-for [route]
  (case route
    :index #'search/help
    :tags-index #'tags-index
    :tag-results #'tag-results
    :note-show #'note-show
    :search-help #'search/help))

(defstyled -nav-item :li
  {:display "inline"
   ":not(:first-child)" {:margin-left "2em"}})

(def nav-item (r/adapt-react-class -nav-item))

(defn tags-dialog [on-close]
  (let [s (state/cursor [:tags-page] {:data {} :filter-value ""})]

    (http/GET "/api/tags"
      {:handler #(swap! s (fn [m] (assoc m :data (get % "data"))))
       :error-handler #(do (swap! s (fn [m] (assoc m :data {})))
                           (js/console.error "failed to fetch tags->notes map: " %))})
    (with-keys
      {key/ESC #(on-close)}
      (fn []
        (let [{:keys [data filter-value]} @s]
          [SearchDialog
           [InputField {:value filter-value
                        :ref (fn [el] (when el (. el focus)))
                        :on-change
                        #(swap! s (fn [m] (assoc m :filter-value (.. % -target -value))))}]
           [:div {:class "mb-4"}
            [:ul {:style {:padding "0" :list-style-type "none"}
                  :class "grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4"}
             (let [filter-tags (filter (complement empty?) (string/split filter-value #"\s+"))]
               (for [[tag count] data]
                 (when (or (empty? filter-tags) (some #(string/includes? tag %) filter-tags))
                   [:li {:key tag
                         :style {:padding-bottom ".9em"}}
                    [Tag {:href (str "/tags/" tag)} (str tag " - " count)]])))]]])))))

; ---------------- page mounting component

(defn Shortcut [key label]
  [:span {:class ""}
   [:span {:class "text-x-green"} key]
   [:span " - " label]])

; TODO: shortcuts are actually bound to js/document, cannot work around this limitation.
(defn current-page []
  (with-shortcuts
    {"ctrl+x s"
     (fn [e]
       (when (nil? (modal/current-component))
         (modal/show! {:title "search notes" :body (search/dialog #(modal/close!))})))

     "ctrl+x t"
     (fn [e]
       (when (nil? (modal/current-component))
         (modal/show! {:title "search tags" :body (tags-dialog #(modal/close!))})))
     "ctrl+x h"
     (fn [e]
       (when (nil? (modal/current-component))
         (accountant/navigate! "/search/help")))}

    (fn []
      (let [page (:current-page (session/get :route))]
        [:div
         [modal/component]
         [:div {:class "pt-4"} [page]]

         [:footer
          {:style {:position "fixed" :bottom 0 :left 0 :right 0}
           :class ["bg-x-grey-dark" "text-white" "font-bold" "px-1" "py-1"]}
          [:p
           [:span {:class "pr-2 text-x-green"} "ctrl-x"]
           [Shortcut "s" "search"]
           [:span {:class "px-2 text-x-grey-light"} "|"]
           [Shortcut "t" "tags"]
           [:span {:class "px-2 text-x-grey-light"} "|"]
           [Shortcut "h" "help"]]]]))))

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
        (modal/close!) ; close modal on page transition
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
