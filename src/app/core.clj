(ns app.core
  (:require [clojure-watch.core :refer [start-watch]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hawk.core :as h]
            [markdown-to-hiccup.core :as m]
            ))


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

#_(h/watch! [{:paths ["/tmp/hello"]
              :handler (fn [ctx e]
                         (println "event: " e)
                         (println "context: " ctx)
                         ctx)}])
