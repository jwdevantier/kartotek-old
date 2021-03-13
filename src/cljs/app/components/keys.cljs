(ns app.components.keys
  (:require [goog.events :as gevents]
            [reagent.core :as r]
            [reagent.dom :as rdom])
  (:import [goog.events EventType KeyHandler]
           [goog.ui KeyboardShortcutHandler]))

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
    (fn with-keys-did-mount [this]
      (let [e (rdom/dom-node this)]
        ; TODO: missed binding listener to actual elem.
        (set! (. this -listener) (-key-listener! keys e))))
    :component-will-unmount
    (fn with-keys-will-unmount [this]
      (. this listener))}))

(defn -kb-shortcuts!
  ([shortcuts] (-kb-shortcuts! shortcuts js/document))
  ([shortcuts elem]
   (let [s (KeyboardShortcutHandler. elem)
         handler
         (fn [e]
           (let [identifier (. e -identifier)]
             ((get shortcuts identifier) identifier)))]
     (doseq [[keys handler] shortcuts]
       (.registerShortcut s keys keys))
     (gevents/listen s KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED handler)
     #(gevents/unlisten s KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED handler))))

(defn with-shortcuts [shortcuts-map component]
  (let [!listen-handle (atom nil)]
    (r/create-class
     {:display-name "with-kb-shortcuts"
      :reagent-render component
      :component-did-mount
      (fn with-shortcuts-did-mount [this]
      ; TODO: bound globally
        (reset! !listen-handle (-kb-shortcuts! shortcuts-map)))
      :component-will-unmount
      (fn with-shortcuts-will-unmount [this]
        (when-let [hndl @!listen-handle] (hndl)))})))
