(ns app.core
  (:require [clojure.string :as string]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [reagent.session :as session]
            [goog.dom :as gdom]
            [ajax.core :as http]
            [reitit.frontend :as reitit]
            [accountant.core :as accountant]
            ["hotkeys-js" :as hotkeys]))

(defn url [& paths]
  (string/join "/" paths))

(defonce state (r/atom {}))

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

(defn tabbed-component [state tabs]
  (reset! state {:active (-> tabs keys first)})
  (fn []
    (js/console.log "re-render -- active:" (:active @state))
    (let [active (:active @state)]
      [:div.tab
       [:div.tab-header
        {:style {:border-bottom "1px solid #000"
                 :padding-bottom ".3em"}}
        (for [[ndx title] (map-indexed vector (keys tabs))]
          [:span.tab-item
           {:style (merge {:border-bottom (if (= title active)
                                            "2px solid #1573B9" "")
                           :cursor "pointer"}
                          (when (not (= ndx 0))
                            {:margin-left ".4em"}))
            :on-click #(do (js/console.log "clicked")
                           (swap! state (fn [s] (assoc s :active title))))
            :key title} title])]
       [(get tabs active)]])))

(defn c-one []
  [:h3 "one"])

(defn c-two []
  [:h3 "two"])

(def tab-state (r/atom {}))

(defn hello-world []
  [:div
   [:h1 "hello, from react"]
   [:div
    [shared-state]
    [:button#fetch {:on-click (fn []
                                (http/GET (url "/api" "tags")))} "fetch"]
    [(tabbed-component tab-state {"one" c-one "two" c-two})]]])

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

(defn popup-show!
  ([component] (popup-show! component {}))
  ([component opts]
   (swap! state (fn [s] (update s :popup #(merge
                                           %
                                           (select-keys opts [:can-close?])
                                           {:component component
                                            :show? true}))))))

(defn popup-hide!
  []
  (swap! state (fn [s] (update s :popup #(assoc % :show? false)))))

(defn popup-show?
  []
  (get-in @state [:popup :show?] false))

(defn popup []
  (fn []
    (let [{:keys [component show? can-close?]
           :or {show? false
                can-close? #(constantly true)}} (get @state :popup)]
      [:div {:style {:position "fixed"
                     :top 0 :bottom 0 :right 0 :left 0
                     :background "rgba(0, 0, 0, 0.7)"
                     :transition "all 200ms ease-in-out"
                     :visibility (if show? "visible" "hidden")
                     :opacity (if show? 100 0)}}
       [:div {:style {:margin "70px auto"
                      :padding "20px"
                      :background "#fff"
                      :border-radius "5px"
                      :width "70%"
                      :position "relative"
                      :transition "all 2s ease-in-out"}}
        [:a {:style {:position "absolute"
                     :top ".2em" :right ".4em"
                     :transition "all 200ms"
                     :font-size "30px"
                     :font-weight "bold"
                     :text-decoration "none"
                     :color "#333"}
             :href "#"
             :on-click #(when (can-close?)
                          (swap! state (fn [s] (assoc-in s [:popup :show?] false))))} "×"]
        (when component [component])]])))

; ---------------- page mounting component


(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      (js/console.warn (session/get :route))
      [:div
       [(popup)]
       [:header
        {:style {:position "fixed" :top 0 :left 0 :right 0
                 :background-color "#1573B9"
                 :color "white"}}
        [:div
         [:span "🔍"]
         [:span "Tags"]]
        "acknowledge me"]
       [page]])))

; ---------------- hot reloading

; https://www.npmjs.com/package/hotkeys-js
(hotkeys "f4" (fn [event handler]
                (.preventDefault event)
                (if (= (. event -type) "keydown")
                  (js/console.log "f4 keydown")
                  (js/console.log "f4 keyup"))))

(hotkeys "ctrl+p" (fn [event handler]
                    (.preventDefault event)
                    (js/console.log "ctrl-p")
                    (popup-show! c-two)))

(hotkeys "escape" (fn [event handler]
                    (.preventDefault event)
                    (when (popup-show?)
                      (popup-hide!))
                    (js/console.log "esc")))

(defn -start []
  ; called after reload
  (rdom/render [current-page] (gdom/getElement "app"))
  (js/console.log @state)
  #_(swap! state (fn [s] (assoc s :popup {:component c-two
                                          :show? true}))))

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
