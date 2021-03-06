(ns app.components.modal
  (:require [app.state :as state]))

(def initial-state {:stack '()})

(let [cursor (state/cursor [:modal] initial-state)]
  (defn current-component
    "get current component or nil."
    []
    (-> @cursor :stack first))

  (defn show!
    "give component to pop up"
    [component]
    (swap! cursor #(update % :stack conj component)))
  (defn close!
    "close top-most popup"
    []
    (swap! cursor #(update % :stack rest))))

(defn component [& {:keys [title body footer]}]
  (let [cursor (state/cursor [:modal] initial-state)]
    (fn []
      (let [{:keys [title body footer]} (current-component)
            hide? (nil? body)]
        [:div {:class ["modal" (when hide? "opacity-0") (when hide? "pointer-events-none") "fixed" "w-full" "h-full" "top-0" "left-0" "flex" "items-center" "justify-center"]}
         [:div {:class ["modal-overlay" "absolute" "w-full" "h-full" "bg-gray-900" "opacity-50"]}]
         [:div {:class ["modal-container" "bg-x-grey" "w-11/12" "md:max-w-md" "mx-auto" "rounded" "shadow-lg" "z-50" "overflow-y-auto"]}
          [:div {:class ["modal-content" "py-4" "text-left" "px-6"]}
           [:div {:class ["flex" "justify-between" "items-center" "pb-3"]}
            [:p {:class ["text-2xl" "font-bold"]} title]
            [:div {:class "modal-close cursor-pointer z-50"}
             [:svg {:class "fill-current text-x-white" :xmlns "http://www.w3.org/2000/svg" :width "18" :height "18" :viewBox "0 0 18 18"
                    :on-click (fn [_] (close!))}
              [:path {:d "M14.53 4.53l-1.06-1.06L9 7.94 4.53 3.47 3.47 4.53 7.94 9l-4.47 4.47 1.06 1.06L9 10.06l4.47 4.47 1.06-1.06L10.06 9z"}]]]]

           body

           (when footer
             [:div {:class "flex justify-end pt-2"} footer])]]]))))
