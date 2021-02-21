(ns app.search
  (:require [emotion.core :refer [defstyled]]
            [reagent.core :as r]
            [ajax.core :as http]
            [app.state :as state]
            [app.components.note :as note]))

(defstyled -search-li :li
  {:padding "0 .9em .3em .9em"
   :&:hover {:background-color "green"}
   "&:nth-of-type(1)" {:padding-top ".3em"}
   :cursor "default"
   :user-select "none"})

(def search-li (r/adapt-react-class -search-li))

(let [meta-ul-attrs {:style {:display "inline-block" :list-style-type "none" :padding "0" :margin "0"}}]
  (defn search-result
    "render single search result"
    [{:strs [id title description links tags]
      :or {title id links #{} tags #{}}
      :as entry}]
    (when id
      [:div
       {:style {:margin "0 2em 1.5em 2em"
                :padding-bottom ".5em"
                :border-bottom "1px dotted #7a7a7a"}
        :key id}
       [:a {:href (str "/notes/" id)} title]
       [:br]
       (when description
         [:p {:class "description"} description])
       [:div
        {:style {:font-size ".9em"}}
        [:span {:style {:color "#909090"}} "links: "]
        [:ul meta-ul-attrs (for [note-id links]
                             [:li {:style {:display "inline"} :key note-id} [:a {:style {:margin "0em .2em"} :href (str "/notes/" note-id)} note-id]])]]
       [:div
        {:style {:font-size ".9em"}}
        [:span {:style {:color "#909090"}} "tags: "]
        [:ul meta-ul-attrs (for [tag tags]
                             [:li {:style {:display "inline"} :key tag} [:a {:style {:margin "0em .2em"} :href (str "/tags/" tag)} tag]])]]])))

; TODO
; 1 - add btn to save a given query (whole purpose for this madness)
; 2 - limit recent queries list to some fixed number of entries
; 3 - ??? show saved queries in list? - if so, also allow to rm them

(defn search [results & {:keys [state] :or {state ::search-form}}]
  (let [cursor (state/cursor [state] {:focused? false
                                      :show? false
                                      :query ""
                                      :latest-queries []})]
    (fn []
      (let [{:keys [focused? show? query latest-queries]} @cursor]
        [:div
         [:form {:style {:margin-bottom "0"}}
          [:input {:type "text" :name "search-query"
                   :id "search-query" :placeholder "query..."
                   :style {:width "20em"}
                   ;:on-focus
                   ;(fn [_]
                   ;  (swap! cursor #(merge % {:focused? true :show? true})))
                   ;:on-blur
                   ;(fn [_]
                   ;  (swap! cursor #(assoc % :focused? false)))
                   ;:on-mouse-enter
                   ;(fn [_]
                   ;  (swap! cursor #(assoc % :show? focused?)))
                   :auto-complete "off"
                   :value query
                   :on-change
                   (fn [event]
                     (let [query (.. event -target -value)]
                       (swap! cursor #(assoc % :query query))
                       (http/POST (str "/api/search/notes")
                         {:format :json
                          :params {"search-query" query}
                          :handler #(do (js/console.warn "RQ OK")
                                        (js/console.warn %)
                                        (reset! results (get % "data")))
                          :error-handler #(js/console.error %)})))}]

          [:input {:type "submit" :class "btn"
                   :style {:margin-left ".4em"}
                   :value "🔍 Search!"
                   :on-click
                   (fn [e]
                     (.preventDefault e)
                     #_(swap! cursor
                              (fn [{:keys [query latest-queries] :as s}]
                                (if (some #(= % query) latest-queries)
                                  s (merge s {:latest-queries
                                              (conj (if (>= (count latest-queries) 5)
                                                      (into [] (drop 1 latest-queries))
                                                      latest-queries) query)}))))
                     #_(reset! results nil)
                     #_(js/console.warn "sending query '" query "'..."))}]]
         (when (and show? latest-queries)
           [:div {:style {:position "fixed"
                          :background-color "#222"
                          :color "white"}
                  :on-mouse-leave
                  (fn [_]
                    (swap! cursor #(assoc % :show? false)))}
            [:ul {:style {:list-style "none"
                          :margin "0"
                          :padding "0"}}
             (for [query (reverse latest-queries)]
               [search-li
                {:on-click (fn [_] (swap! cursor (fn [s] (merge s {:query query
                                                                   :show? false}))))
                 :key query}
                query])
             #_[search-li {:on-click #(js/console.warn "BOOM")} "t:blockchain!"]
             #_[search-li "matrix"]
             #_[search-li "t:programming -t:php"]
             #_[search-li "r:hello.md -t:programming"]
             #_[search-li "meget langt list item, liiiie hæææærrr"]]])]))))

(defn help
  "help page"
  []
  (let [rsp (r/atom nil)]
    (fn []
      (http/GET "/api/search/notes/help"
        {:handler #(reset! rsp %)})
      (if-let [html @rsp]
        [note/render html]
        #_[:article {:class "note" :dangerouslySetInnerHTML {:__html html}}]
        [:p "fetching help page..."]))))
