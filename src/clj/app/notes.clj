(ns app.notes
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
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

; (parse-md-doc (slurp file-path))
(defn parse-md-doc
  "Parse `content` into map {:content ... :meta ...}.

  Parses given string `content` into a map of two entries:
    * :content - the document markdown parsed into hiccup structures
    * :meta - document meta-data (expressed in yaml) parsed into a map of entries

  NOTE: if no meta-data is provided, `:meta` will return an empty map."
  [content]
  (-> (if (string/starts-with? content "---")
        (let [lines (string/split-lines content)
              meta-lines (take-while #(not (string/starts-with? % "---"))
                                     (drop 1 lines))]
          {:meta (-> (->> meta-lines (string/join "\n") (yml/parse-string))
                     (update :tags #(-> (map string/lower-case %)
                                        set)))
           :content (->> lines (drop (+ (count meta-lines) 2)) (string/join "\n"))})
        {:content content :meta {}})
      (update :content md->hiccup)))

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
