(ns app.api.search
  (:require [app.jsend :as jsend]
            [app.filedb :as db]
            [hiccup.core :as hc]
            [clojure.java.io :as io]
            [app.notes :as notes]
            [taoensso.timbre :as timbre]))

(defn query
  "return results of search request."
  [rq]
  (let [search-query (get-in rq [:body "search-query"])]
    (try
      (jsend/success (db/search search-query))
      (catch Exception e
        (timbre/warnf e "error parsing search query '%s'" search-query)
        (jsend/fail {})))))

(defn help
  "return HTML-formatted search help"
  [rq]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (-> "search-help.md" io/resource slurp notes/md->hiccup hc/html)})
