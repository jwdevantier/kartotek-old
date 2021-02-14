(ns app.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [goog.dom :as gdom]
            [ajax.core :as http]
            [clojure.string :as string]))

(defn url [& paths]
  (string/join "/" paths))

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

(defn hello-world []
  [:div
   [:h1 "hello, from react"]
   [:div
    [shared-state]
    [:button#fetch {:on-click (fn []
                                (http/GET (url "api" "tags")))} "fetch"]]])


(defn -start []
  ; called after reload
  (rdom/render [hello-world] (gdom/getElement "app")))

(defn ^:export init []
  ; called only once on app load
  (-start)
  (js/console.log {:hello "world!!"
                   :is-map true}))

(defn -stop []
  ; called before any code is reloaded, see shadow-cljs :before-load
  (js/console.warn "reloading."))
