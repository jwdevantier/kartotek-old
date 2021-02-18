(ns app.filedb
  (:require [hawk.core :as h]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [org.rssys.context.core :as ctx]
            [instaparse.core :as insta]
            [taoensso.timbre :as timbre]
            [app.notes :as notes]
            [app.state :refer [system get-notes-path]]))


(defn filter-db
  "Returns a lazy sequence of the items in the database
  for which `pred` returns logical true."
  [pred]
  (let [db (-> system
               (ctx/get-component :filedb)
               :state-obj
               :db
               deref)]
    (filter pred (for [[fname entry] db] (assoc entry :id fname)))))

(defn tags->notes
  "extract a map of tag -> set-of-docs entries."
  []
  (let [doc-entries (->> (filter-db (fn [_] true))
                         (map #(select-keys % [:id :tags]))
                         (map (fn [{:keys [id tags]}]
                                (reduce (fn [r tag]
                                          (update r tag (fn [docs] (conj (or docs #{}) id)))) {} tags))))]
    (apply merge-with clojure.set/union doc-entries)))

(defn -build-db-doc-entry [fpath]
  (try
    (let [doc (notes/parse-md-doc (slurp fpath))
            links (notes/extract-links (get-notes-path) (:content doc))]
        ; ensure all fields exist, even if no value was provided in doc meta data
        (merge {:title "" :description "" :tags #{} :links #{}}
            (assoc (:meta doc) :links links)))
    (catch Exception e
      (timbre/warn (str "failed to load document '" fpath "': " (.getMessage e)))
         nil)))

(defn -build-db [dir]
  (reduce (fn [db fpath]
            (assoc db (-> fpath io/file (.getName))
                   (-build-db-doc-entry fpath)))
          {} (notes/notes-paths dir)))

(defn -watch-notes-dirs
  "watch notes directory, updating entries in `db` atom as appropriate."
  [db note-dirs]
  (h/watch! [{:paths note-dirs
              :filter
              (fn [_ {:keys [file kind]}]
                (and (string/ends-with? (.getName file) ".md")
                     (or (and (= kind :modify)
                              (.isFile file))
                         (= kind :delete))))
              :handler
              (fn [_ {:keys [file kind]}]
                (swap! db (fn [v] (if (= kind :delete)
                                    (dissoc v (.getName file))
                                    (assoc v (.getName file)
                                           (-build-db-doc-entry file))))))}]))

;; Parser definition
;; -----------------
;;
;; Example queries:
;; hello world
;;   - show notes whose titles contain 'hello' and 'world'
;;
;; "hello world"
;;   - show notes whose title contains the phrase 'hello world'
;;
;; hello -world
;;   - show notes whose title contains the word 'hello' but NOT 'world'
;;
;; t:programming r:hello.md
;;   - show notes tagged 'programming' which is related to 'hello.md'
;;
;; t:programming -react
;;   - show notes tagged 'programming' where 'react' is NOT part of the title
;;
;;
;; related: doc X is related to doc Y iff. X contains one or more links to Y
;;          - dox X is related to Y if X is among Y's backlinks!
(def parse-query
  (insta/parser
   "<Q> = (TERM <WS>)* TERM
    TERM = NOT? ((T | R) <':'> STR | STR)
    T = 't' | 'tag'
    R = 'r' | 'related'
    NOT = <'-'>
    QUOTE = '\"'
    STR = <QUOTE> #'[^\"]+' <QUOTE> | #'[^:^\"^\\s^-][^:^\"^\\s]*'
    WS = ' '+
    "))

(defn -query-ast->filter
  "translate query AST into a valid filter predicate function"
  [ast]
  (let [parse-term-inner (fn [[b c :as term]]
                           (case (count term)
                             ; [[:STR "something"]]
                             1 (let [[_ search-string] b]
                                 #(string/includes? (-> % :title) search-string))
                             ; [[:R] [:STR "something"]]
                             ; [[:T] [:STR "something"]]
                             2 (let [[modifier] b
                                     [_ search-string] c]
                                 (case modifier
                                   :T #(contains? (:tags %) search-string)
                                   :R #(contains? (:links %) search-string)))))
        parse-term (fn [[_ & t]]
                     (if (= [:NOT] (first t))
                       (complement (parse-term-inner (rest t)))
                       (parse-term-inner t)))]
    (let [preds (map parse-term ast)]
      (fn [entry]
        (every? (fn [pred] (pred entry)) preds)))))

(defn search
  "return results for search"
  [query]
  (when-not (empty? query)
    (-> query parse-query -query-ast->filter filter-db)))

(defn start
  "start file database"
  [{:keys [note-dir]}]
  (let [db (atom (-build-db note-dir))]
    (println (str "starting file database (notes dir: " note-dir ")"))
    ; scan all dirs, populate
    {:db db
     :watcher (-watch-notes-dirs db [note-dir])}))

(defn stop
  "stop file database"
  [{:keys [db watcher]}]
  (println "stopping file database...")
  (h/stop! watcher)
  (reset! db nil))
