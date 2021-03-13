(ns app.components.base)

(defn InputField [opts]
  [:input (merge {:type "text"
                  :class "w-full px-4 py-2 border-x-grey focus:border-x-grey-light border-solid border-2 bg-x-grey-dark w-full text-x-white focus:outline-none"
                  :auto-complete "off"
                  :name "search-query"
                  :id "search-query"
                  :placeholder "query..."} opts)])

(defn Tag
  ([label] (Tag {} label))
  ([opts label]
   [:div
    {:class "inline-flex rounded-full text-xs font-bold leading-sm uppercase px-3 py-1 bg-x-grey-light text-x-white"}
    [:a (merge {:style {:color "white"
                        :font-size ".9em"
                        :text-decoration "none"}} opts) label]]))

(defn SearchDialog [top body]
  [:div {:class "flex flex-col w-full"}
   [:div {:class ["flex flex-none"]}
    top]
   [:div
    {:class "flex flex-col flex-grow min-h-0 mt-4 overflow-y-scroll scrollbar-thin scrollbar-thumb-x-blue scrollbar-track-x-grey-dark"}
    body]])
