(ns app.search
  (:require [emotion.core :refer [defstyled]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [ajax.core :as http]
            [goog.events.KeyCodes :as key]
            [accountant.core :as accountant]
            [app.state :as state]
            [app.components.base :refer [InputField SearchDialog]]
            [app.components.modal :as modal]
            [app.components.note :as note]
            [app.components.keys :refer [with-keys]]))

(defstyled -search-li :li
  {:padding "0 .9em .3em .9em"
   :&:hover {:background-color "green"}
   "&:nth-of-type(1)" {:padding-top ".3em"}
   :cursor "default"
   :user-select "none"})

(def search-li (r/adapt-react-class -search-li))

(let [meta-ul-attrs {:style {:display "inline-block" :list-style-type "none" :padding "0" :margin "0"}
                     :class ["inline-block" "text-x-blue"]}]
  (defn search-result
    "render single search result"
    [{:strs [id title description links tags active?]
      :or {title id links #{} tags #{} active? false}
      :as entry}]
    (when id
      [:div
       {:style {:padding ".5em"
                :border-bottom "1px dotted #7a7a7a"}
        :class (if active? ["bg-x-grey-dark" "border-x-blue" "border-l-4"] ["border-l-4" "border-transparent"])
        :key id
        ; react reference - fn run @ component lifecycle start.
        :ref (fn [el] (when (and el active?)
                        (. el scrollIntoView (clj->js {:behavior "smooth"
                                                       :block "nearest"
                                                       :inline "nearest"}))))}
       [:a {:class ["text-x-green"] :href (str "/notes/" id)} title]
       [:br]
       (when description
         [:p {:class ["text-x-white"]} description])
       [:div
        {:style {:font-size ".9em"}}
        [:span {:class ["text-x-grey-light"]} "links: "]
        (if (> (count links) 0)
          [:ul meta-ul-attrs (for [note-id links]
                               [:li {:style {:display "inline"} :key note-id}
                                [:a {:style {:margin "0em .2em"} :href (str "/notes/" note-id)} note-id]])]
          [:p {:class ["inline-block" "text-x-white"]} "-"])]
       [:div
        {:style {:font-size ".9em"}}
        [:span {:class ["text-x-grey-light"]} "tags: "]
        (if (> (count tags) 0)
          [:ul meta-ul-attrs (for [tag tags]
                               [:li {:style {:display "inline"} :key tag} [:a {:style {:margin "0em .2em"} :href (str "/tags/" tag)} tag]])]
          [:p {:class ["inline-block" "text-x-white"]} "-"])]])))

(defn dialog [on-close]
  (let [cursor (state/cursor [:note-search] {:query "" :results [] :selected-ndx 0})]
    (with-keys
      {key/UP
       #(do (. % preventDefault)
            (swap! cursor (fn [s] (assoc s :selected-ndx
                                         (max 0 (dec (get s :selected-ndx 0)))))))
       key/DOWN
       #(do (. % preventDefault)
            (swap! cursor (fn [s] (assoc s :selected-ndx (let [{:keys [results selected-ndx]} s]
                                                           (max 0 (min (-> results count dec) (inc selected-ndx))))))))
       key/ESC #(on-close)
       key/ENTER
       #(do (let [{:keys [results selected-ndx]} @cursor
                  selected-result (get results selected-ndx nil)]
              (. % preventDefault)
              (accountant/navigate! (str "/notes/" (get selected-result "id")))
              (modal/close!)))}
      (fn [on-close]
        [SearchDialog
         [InputField
          {:value (get @cursor :query "")
           :ref (fn [el] (when el (. el focus)))
           :on-change
           (fn [event]
             (let [query-value (.. event -target -value)]
               (swap! cursor #(assoc % :query query-value))
               (http/POST (str "/api/search/notes")
                 {:format :json
                  :params {"search-query" query-value}
                  :handler (fn [rsp] (swap! cursor (fn [s] (merge s {:results (get rsp "data" [])
                                                                     :selected-ndx 0}))))
                  :error-handler #(js/console.error %)})))}]
         (let [{:keys [results selected-ndx] :or {results [] selected-ndx 0}} @cursor]
           (map-indexed (fn [ndx item] (search-result (assoc item "active?" (= ndx selected-ndx)))) results))]))))

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
