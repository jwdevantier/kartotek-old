(ns app.api.notes
  (:require [clojure.pprint :as pp]
            [ring.util.response :refer [response]]
            [ring.util.request :refer [body-string]]
            [hiccup.core :as hc]
            [app.jsend :as jsend]
            [app.state :as state]
            [app.notes :as notes]
            [app.filedb :as db]))

(defn note-show
  "return rendered note HTML"
  [rq]
  (let [id (-> rq :path-params :note-id)
        note (-> (state/get-config)
                 (get-in [:db :note-dir])
                 (java.io.File. id)
                 slurp
                 notes/parse-md-doc)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (hc/html (:content note))}))
