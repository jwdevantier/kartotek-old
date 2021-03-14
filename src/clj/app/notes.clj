(ns app.notes
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [markdown-to-hiccup.core :as m]
            [clj-yaml.core :as yml]
            [app.iou :as iou]))

(defn walk-dir
  "recursively walk through `dir` returning files matching `pattern`.

  "
  [dir pattern]
  (->> dir
       (io/file)
       (file-seq)
       (filter #(re-matches pattern (.getName %)))))

(defn ls
  "list directory"
  [dir]
  (.list (io/file dir)))

(defn notes-paths
  "return seq of paths to notes files."
  [dir]
  (->> (ls dir)
       (filter #(re-matches #".*\.md$" %))
       (map #(iou/path-join dir %))))

(defn md->hiccup
  "parse raw markdown text to hiccup data structure."
  [content]
  (-> content (m/md->hiccup) (m/component)))

(defn extract-links
  "extracts links to other documents from the document's hiccup AST."
  [note-dir hiccup]
  (letfn [(inner [links node]
            (if (not (vector? node))
              links
              (if (= (first node) :a)
                (conj links (get-in node [1 :href]))
                (reduce inner links node))))]
    (->> hiccup
         (inner #{})
         (filter #(.exists (java.io.File. note-dir %)))
         (into #{}))))

(defn -create-doc-meta-search-keys
  "Parse meta-section of doc entry to create search-friendly entries.

  Parses meta map and creates a set of keys used to satisfy search queries.
  This means the original representation is still displayed while we can
  transform the keys (namely lower-case strings) to make searches
  case-insensitive."
  [m]
  (merge m (->> m
                (map (fn [[k v]] [(keyword "search" (name k)) v]))
                (into {})
                (walk/postwalk #(if (string? %) (string/lower-case %) %)))))

(defn parse-md-doc
  "Parse `content` into map {:content ... :meta ...}.

  Parses given string `content` into a map of two entries:
    * :content - the document markdown parsed into hiccup structures
    * :meta - document meta-data (expressed in yaml) parsed into a map of entries

  NOTE: if no meta-data is provided, `:meta` will return an empty map."
  [note-dir fname]
  (let [content (slurp (java.io.File. note-dir fname))]
    (if (string/starts-with? content "---")
      (let [lines (string/split-lines content)
            meta-lines (take-while #(not (string/starts-with? % "---"))
                                   (drop 1 lines))
            content (->> lines
                         (drop (+ (count meta-lines) 2))
                         (string/join "\n")
                         md->hiccup)
            links (set (extract-links note-dir content))]
        {:meta (merge
              ; ensure all fields exist, even if no value was provided in the doc
                {:title "" :description "" :tags #{} :links #{}}
                (-> (->> meta-lines (string/join "\n") yml/parse-string)
                    (update :tags set)
                    (assoc :links links)
                    -create-doc-meta-search-keys))
         :content content})
      {:content (md->hiccup content) :meta {}})))
