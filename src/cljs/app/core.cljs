(ns app.core)

(defn ^:export init []
  (js/console.log {:hello "world"
                   :is-map true}))
