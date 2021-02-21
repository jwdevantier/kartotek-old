(ns app.components.note
  (:require [reagent.core :as r]
            ["react-dom" :as react-dom]
            ["highlight.js" :as hjs]))

(defn render
  "Render note's raw HTML. Notably ensures syntax highlighting is applied."
  [html-str]
  ; A few tricks
  ; 1) in 'article', we attach a 'ref' function which provides us the element's reference in the DOM
  ; 2) 'hl-code' uses this reference as the root of its search for all elements matching the CSS
  ;    selector 'pre code' - we then loop through this JS array using forEach, calling highlight.js'
  ;    'highlightBlock' on each element.
  ; 3) we use a form-3 component to ensure highlighting is applied after the component has mounted
  ;    and if it is updated.
  ;    NOTE: the latter should not be necessary, dangerouslySetInnerHTML should mean the HTML
  ;          contents are NEVER altered by React, ever.
  (let [root-el (r/atom nil)
        highlight-block (fn [node] (. hjs highlightBlock (node)))
        hl-code (fn []
                  (. (. @root-el querySelectorAll "pre code") forEach #(. hjs highlightBlock %)))]
    (r/create-class
     {:display-name "note"
      :component-did-mount
      #(hl-code)
      :component-did-update
      #(hl-code)
      :reagent-render
      (fn [html-str]
        [:article {:ref #(reset! root-el %)
                   :class "note" :dangerouslySetInnerHTML {:__html html-str}}])})))
