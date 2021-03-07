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

(let [meta-ul-attrs {:style {:display "inline-block" :list-style-type "none" :padding "0" :margin "0"}
                     :class ["inline-block" "text-x-blue"]}]
  (defn search-result
    "render single search result"
    [{:strs [id title description links tags]
      :or {title id links #{} tags #{}}
      :as entry}]
    (when id
      [:div
       {:style {:margin "0 0em 1.5em 0em"
                :padding-bottom ".5em"
                :border-bottom "1px dotted #7a7a7a"}
        :key id}
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

(defn search [results & {:keys [state] :or {state ::search-form}}]
  (let [cursor (state/cursor [state] {:focused? false
                                      :show? false
                                      :query ""
                                      :latest-queries []})]
    (fn []
      (let [{:keys [query]} @cursor]
        [:input {:type "text" :name "search-query"
                 :id "search-query" :placeholder "query..."
                 :class "w-full px-4 py-2 border-x-grey focus:border-x-grey-light border-solid border-2 bg-x-grey-dark w-full text-x-white focus:outline-none"
                 :auto-complete "off"
                 :value query
                 :on-change
                 (fn [event]
                   (let [query (.. event -target -value)]
                     (swap! cursor #(assoc % :query query))
                     (http/POST (str "/api/search/notes")
                       {:format :json
                        :params {"search-query" query}
                        :handler #(reset! results (get % "data"))
                        :error-handler #(js/console.error %)})))}]))))

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
