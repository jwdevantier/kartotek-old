(ns app.components.keys
  (:require [goog.events :as gevents]
            [reagent.core :as r]
            [reagent.dom :as rdom])
  (:import [goog.events EventType KeyHandler]))

(defn -key-listener!
  "
  NOTE: returns function to remove event listener"
  ([kmap] (-key-listener! kmap js/document))
  ([kmap elem]
   (let [key-handler (KeyHandler. elem)
         on-key-press #(when-let [f (get kmap (.. % -keyCode))] (f %))
         ^EventType et (. KeyHandler -EventType)]
     (gevents/listen key-handler (. et -KEY) on-key-press)
     #(gevents/unlisten key-handler (. et -KEY) on-key-press))))

(defn with-keys [keys component]
  (r/create-class
   {:display-name "with-keys"
    :reagent-render
    component
    :component-did-mount
    (fn search-dialog-did-mount [this]
      (let [e (rdom/dom-node this)]
        (set! (. this -listener) (-key-listener! keys))))
    :component-will-unmount
    (fn search-dialog-will-unmount [this]
      (. this listener))}))
