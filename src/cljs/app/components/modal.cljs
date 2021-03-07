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
        [:div {:class ["modal" "flex" "flex-col" (when hide? "opacity-0") (when hide? "pointer-events-none") "fixed" "w-full" "h-full" "top-0" "left-0" "items-start" "justify-center"]}
         [:div {:class "modal-overlay absolute w-full h-full bg-gray-900 opacity-0"}]
         [:div {:class "flex flex-col w-full h-full justify-start py-10"}
          [:div {:class ["modal-container" "flex" "flex-col" "min-h-0" "relative" "w-10/12" "bg-x-grey" "mx-auto" "border-2" "border-x-grey-light" "z-50"]}
           [:div {:class "modal-close cursor-pointer z-50 flex justify-end pr-1 pt-1 absolute top-0 right-0"}
            [:svg {:class "fill-current text-x-white hover:text-x-yellow" :xmlns "http://www.w3.org/2000/svg" :width "18" :height "18" :viewBox "0 0 18 18"
                   :on-click (fn [_] (close!))}
             [:path {:d "M14.53 4.53l-1.06-1.06L9 7.94 4.53 3.47 3.47 4.53 7.94 9l-4.47 4.47 1.06 1.06L9 10.06l4.47 4.47 1.06-1.06L10.06 9z"}]]]
           [:p {:class "text-sm font-bold text-x-white justify-center flex"} title]
           [:div {:class ["modal-content" "flex" "min-h-0" "py-4" "text-left" "px-6" ]}

            (if (fn? body) [body] body)

            (when footer
              [:div {:class "flex-none justify-end pt-2"} footer])]]]]))))
