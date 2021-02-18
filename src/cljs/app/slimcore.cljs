(ns app.slimcore
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [goog.dom :as gdom]
            [emotion.core :refer [defstyled]]
            [app.localstorage :as localstorage]))

(defonce -mounted-components (atom {}))
(defonce state (r/atom {}))

(defstyled -search-li :li
  {:padding "0 .9em .3em .9em"
   :&:hover {:background-color "green"}
   "&:nth-of-type(1)" {:padding-top ".3em"}
   :cursor "default"
   :user-select "none"})

(def search-li (r/adapt-react-class -search-li))

(defn search []
  (let [update! (fn [f & args] (swap! state (fn [s] 
                                              (apply update s :search-form f args))))]
    (fn []
      (let [{:keys [focused? show? query]
             :or {focused? false show? false query ""}} (get @state :search-form)]
        [:div
         [:form {}
          [:input {:type "text" :name "search-query"
                   :id "search-query" :placeholder "query..."
                   :on-focus
                   (fn [_]
                     (update! #(merge % {:focused? true :show? true})))
                   :on-blur
                   (fn [_]
                     (update! #(assoc % :focused? false)))
                   :on-mouse-enter
                   (fn [_]
                     (update! #(assoc % :show? focused?)))
                   :auto-complete "off"
                   :value query
                   :on-change
                   (fn [event]
                     (update! #(assoc % :query (.. event -target -value))))}]
          [:input {:type "submit" :class "btn"
                   :style {:margin-left ".4em"}
                   :value "🔍 Search!"
                   :on-click
                   (fn [e]
                     (.preventDefault e)
                     (js/console.warn "submit btn hit"))}]]
         (when show?
           [:div {:style {:position "fixed"
                          :background-color "#222"
                          :color "white"}
                  :on-mouse-leave
                  (fn [_]
                    (update! #(assoc % :show? false)))}
            [:ul {:style {:list-style "none"
                          :margin "0"
                          :padding "0"}}
             [search-li {:on-click #(js/console.warn "BOOM")} "t:blockchain!"]
             [search-li "matrix"]
             [search-li "t:programming -t:php"]
             [search-li "r:hello.md -t:programming"]
             [search-li "meget langt list item, liiiie hæææærrr"]]])]))))

(defn get-component [component-name]
  (case component-name "search" #'search))

; expose function `mount` to mount a component in the HTML DOM
(set! (.. js/window -mount)
      (fn [dom-id component-name]
        (let [existing (get @-mounted-components dom-id)]
          (println (str "mounting component '" component-name "' at DOM element '" dom-id "'."))
          (when (not (= existing component-name))
            (rdom/render [(get-component component-name)] (gdom/getElement dom-id))
            (swap! -mounted-components #(assoc % dom-id component-name))))))

; export fn to show state
(set! (.. js/window -showState)
      (fn [] (js/console.warn @state)))

(defn -start []
  ; (re-)mount all components
  (doseq [[dom-id component-name] @-mounted-components]
    (rdom/render [(get-component component-name)] (gdom/getElement dom-id))))

(defn ^:export init []
  (-start))

(defn -stop []
  (js/console.warn "reloading...!"))