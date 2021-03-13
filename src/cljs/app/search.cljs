(ns app.search
  (:require [emotion.core :refer [defstyled]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [ajax.core :as http]
            [goog.events :as gevents]
            [goog.events.KeyCodes :as key]
            [accountant.core :as accountant]
            [app.state :as state]
            [app.components.modal :as modal]
            [app.components.note :as note])
  (:import [goog.events EventType KeyHandler]))

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

(defn key-listener!
  "
  NOTE: returns function to remove event listener"
  ([kmap] (key-listener! kmap js/document))
  ([kmap elem]
   (let [key-handler (KeyHandler. elem)
         on-key-press #(when-let [f (get kmap (.. % -keyCode))] (f %))
         ^EventType et (. KeyHandler -EventType)]
     (gevents/listen key-handler (. et -KEY) on-key-press)
     #(gevents/unlisten key-handler (. et -KEY) on-key-press))))

; TODO: extract key-mgmt logic into custom container class - move along key-listener! to other file
(defn dialog [on-close]
  (let [cursor (state/cursor [:note-search] {:query "" :results [] :selected-ndx 0})
        on-key-up
        #(do (js/console.log "UP")
             (. % preventDefault)
             (swap! cursor (fn [s] (assoc s :selected-ndx
                                          (max 0 (dec (get s :selected-ndx 0)))))))
        on-key-down
        #(do (js/console.log "DOWN")
             (. % preventDefault)
             (swap! cursor (fn [s] (assoc s :selected-ndx (let [{:keys [results selected-ndx]} s]
                                                            (max 0 (min (-> results count dec) (inc selected-ndx))))))))
        on-esc
        #(on-close)
        on-enter
        #(do (js/console.log "ENTER")
             (let [{:keys [results selected-ndx]} @cursor
                   selected-result (get results selected-ndx nil)]
               (accountant/navigate! (str "/notes/" (get selected-result "id")))
               (modal/close!)))]
    (r/create-class
     {:display-name "search-dialog"
      :reagent-render
      (fn [on-close]
        [:div {:class "flex flex-col w-full"}
     ; search field
         [:div {:class ["flex flex-none"]}
          [:input {:type "text" :name "search-query"
                   :id "search-query" :placeholder "query..."
                   :class "w-full px-4 py-2 border-x-grey focus:border-x-grey-light border-solid border-2 bg-x-grey-dark w-full text-x-white focus:outline-none"
                   :auto-complete "off"
                   :value (get @cursor :query "")
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
                          :error-handler #(js/console.error %)})))}]]
     ; search results
         [:div {:class "flex flex-col flex-grow min-h-0 mt-4 overflow-y-scroll scrollbar-thin scrollbar-thumb-x-blue scrollbar-track-x-grey-dark"}
          (let [{:keys [results selected-ndx] :or {results [] selected-ndx 0}} @cursor]
            (map-indexed (fn [ndx item] (search-result (assoc item "active?" (= ndx selected-ndx)))) results))]])
      :component-did-mount
      (fn search-dialog-did-mount [this]
        (let [e (rdom/dom-node this)]
          (set! (. this -listener) (key-listener! {key/UP on-key-up
                                                   key/DOWN on-key-down
                                                   key/ESC on-esc
                                                   key/ENTER on-enter}))))
      :component-will-unmount
      (fn search-dialog-will-unmount [this]
        (. this listener))})))

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
