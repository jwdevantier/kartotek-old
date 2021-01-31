(ns app.core
  (:require [clojure-watch.core :refer [start-watch]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hawk.core :as h]
            [markdown-to-hiccup.core :as m]
            [clj-yaml.core :as yml]))

#_(def expost "/Users/jwd/repos/personal/11tyblog/posts/1-clj-intro.md")
(def expost "/home/pseud/repos/cljblog/src/posts")
; watch dir
; only act on *.md files
; translate input path, e.g. src/posts/deps/intro.md -> site/deps/intro/index.html


; example: (walk-dir "/home/pseud/repos" #".*\.py$")
(defn walk-dir
  "recursively walk through `dir` returning files matching `pattern`.

  "
  [dir pattern]
  (->> dir
       (io/file)
       (file-seq)
       (filter #(re-matches pattern (.getName %)))))

(defn md->hiccup [content]
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
          {:meta (->> meta-lines (string/join "\n") (yml/parse-string))
           :content (->> lines (drop (+ (count meta-lines) 2)) (string/join "\n"))})
        {:content content :meta {}})
      (update :content md->hiccup)))


(defn file-path [f]
  (if (instance? java.io.File f)
    (.toString f) f))

(defn file? [f]
  (instance? java.io.File f))

(defn ->file [f]
  (if (file? f) f (io/file f)))

(def file-sep (System/getProperty "file.separator"))

(defn path-abs?
  "true if `path` is an absolute path"
  [path]
  (string/starts-with? path file-sep))

(defn path-abs
  "get absolute path of `path`.
  "
  [path]
  (if (and (string? path) (path-abs? path))
    path (-> path (->file) (.getAbsolutePath))))

(defn path-split
  "split `path` into vector."
  [path]
  (-> path (string/split (re-pattern file-sep))))

(defn path-resolve
  "resolve `path` to absolute path string."
  [path]
  (let [v (if (vector? path) path (-> path (path-abs) (path-split)))
        ; on Unix, splitting a path by '/' (file-sep) yields a vector with a leading ""
        ; this must be preserved in the final output path for the path to remain absolute.
        prefix (if (= (first v) "") file-sep "")]
    (->> v
         (reduce (fn [acc e] (case e
                               "" acc
                               "." acc
                               ".." (pop acc)
                               (conj acc e))) [])
         (string/join file-sep)
         (str prefix))))

; should detect & remove `src-root` and replace with `dst-root`
; should translate "./x/y/z.md" to "./x/y/z/index.html"
(defn dest-path [path src-root dst-root])


(defn on-watch-event [watch-dir event fname]
  (println watch-dir event fname))

;; TODO: take root path as arg, use closure to embed root path
;;       in resolution of event path.
(defn watch [watch-path]
  (start-watch [{:path watch-path
                 :event-types [:create :modify :delete]
                 :callback (fn [event fname] (on-watch-event watch-path event fname))
                 :options {:recursive true}}]))

#_(let [watcher (atom nil)]
    (defn watch-begin []
      (when (nil? @watcher)
        (reset! watcher (watch "site/posts"))))
    (defn watch-end []
      (if-let [stop-watch @watcher]
        (do (stop-watch) (reset! watcher nil)))))

#_(let [watcher (atom nil)]
  (defn watch-begin))

#_(h/watch! [{:paths ["/tmp/hello"]
              :handler (fn [ctx e]
                         (println "event: " e)
                         (println "context: " ctx)
                         ctx)}])
