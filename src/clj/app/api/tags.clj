(ns app.api.tags
  (:require [app.jsend :as jsend]
            [app.filedb :as db]))

(defn all-tags
  "return all tags and their number of child notes."
  [rq]
  (let [tag->num-entries (->> (db/tags->notes)
                              (map (fn [[k v]] [k (count v)]))
                              (sort-by (fn [[k _]] k))
                              (into {}))]
    (jsend/success tag->num-entries)))

(defn notes-list
  "return results of all notes using the tag"
  [rq]
  (let [tag (-> rq :path-params :tag)
        results (db/filter-db #(contains? (get % :tags #{}) tag))]
    (jsend/success results)))
