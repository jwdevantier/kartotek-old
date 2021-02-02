(ns app.filedb
  (:require [hawk.core :as h]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [app.notes :as notes]
            [app.system :as sys]
            [org.rssys.context.core :as ctx]))

; TODO: refac to make this part work - might need to take ctx args and return a watcher fn
;       if modify: assoc in -build-db-doc-entry
;       if delete: dissoc entry
(defn -watch-notes-dirs
  ""
  [db note-dirs]
  (h/watch! [{:paths note-dirs
              :filter
              (fn [_ {:keys [file kind]}]
                (and (string/ends-with? (.getName file) ".md")
                     (or (and (= kind :modify)
                              (.isFile file))
                         (= kind :delete))))
              :handler
              (fn [ctx event]

                (println "Ctx:" ctx)
                (println "event:" event))}]))

(defn -build-db-doc-entry [fpath]
  (let [doc (notes/parse-md-doc (slurp fpath))
        links (notes/extract-links (:content doc))]
    (assoc (:meta doc) :links links)))

(defn -build-db [dir]
  (reduce (fn [db fpath]
            (assoc db (-> fpath io/file (.getName))
                   (-build-db-doc-entry fpath)))
          {} (notes/notes-paths dir)))

(defn filter-db
  "Returns a lazy sequence of the items in the database
  for which `pred` returns logical true."
  [pred]
  (let [db (-> sys/system
               (ctx/get-component :filedb)
               :state-obj
               :db
               deref)]
    (filter pred (for [[fname entry] db] (assoc entry :id fname)))))

(defn start
  "start file database"
  [{:keys [note-dir]}]
  (let [db (atom (-build-db note-dir))]
    (println "starting file database...")
    ; scan all dirs, populate
    {:db db
     :watcher (-watch-notes-dirs db [note-dir])}))

(defn stop
  "stop file database"
  [{:keys [db watcher]}]
  (println "stopping file database...")
  (h/stop! watcher)
  (reset! db nil))
