(ns app.filedb
  (:require [hawk.core :as h]))


(defn -watch-notes-dirs
  ""
  [db & note-dirs]
  (h/watch! [{:paths note-dirs
              :handler (fn [ctx event]
                         )}]))

; TODO: now, parse doc and assoc something into DB.. What shape shall it take ?

{:tags {"one" ["hello.md" "else.md"]
        "programming" ["c.md" "vim.md"]}
 ; backlinks - entries linking to this entry
 :backlinks {"vim.md" ["editors.md" "c.md"]}
 ; raw meta-data
 :meta {}
 ; hash
 :hash {}}

(defn start
  "start file database"
  [{:keys [note-dirs]}]
  (let [db (atom {})]
    (println "starting file database...")
    ; scan all dirs, populate
    {:db db
    :watcher (-watch-notes-dirs db note-dirs)}))

(defn stop
  "stop file database"
  [{:keys [db watcher]}]
  (println "stopping file database...")
  (h/stop! watcher)
  (reset! db nil))
