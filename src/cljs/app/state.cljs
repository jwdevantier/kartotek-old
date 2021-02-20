(ns app.state
  (:require [reagent.core :as r]))

(defonce state (r/atom {}))

(defn cursor
  "acquire cursor and optionally set initial data."
  ([ks] (cursor keys nil))
  ([ks initial]
   (when (and (not= initial nil)
              (nil? (get-in @state ks)))
     (swap! state (fn [s]
                    (assoc-in s ks initial))))
   (let [c (r/cursor state ks)]
     (reset-meta! c {:cursor-path ks})
     c)))

(defn cursor-path
  "get cursor's path into state atom."
  [cursor]
  (-> cursor meta (get :cursor-path)))

; export fn to show state
(set! (. js/window -showState)
      (fn [] (js/console.warn @state)))
