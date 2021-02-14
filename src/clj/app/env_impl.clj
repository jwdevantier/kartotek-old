(ns app.env-impl
  (:require [clojure.spec.alpha :as s]
            [clojure.edn :as edn]))

(defn mkconfig
  "create config value"
  [spec]
  (let [a (atom {})]
    (reset-meta! a {:spec spec})
    a))

(defn env-name
  "convert keyword to env name format"
  [kw]
  (-> kw name (clojure.string/replace "-" "_") clojure.string/upper-case))

; Implementation of a file/env config reader coercing values according to defined specs
;
; Written because `environ` is a bit too limited:
; * no coercion/validation - just returns string values
; * no ability to read in values from a file
; ... and `wrench`:
; * has an error rendering spec validation useless (conforms on a nil value...)
; * ~ no clearly defined order between env and cfg file when refreshing from both sources(?)

(defn kw-name
  "convert env var string to keyword"
  [env-name]
  (-> clojure.string/lower-case (clojure.string/replace "_" "-") keyword))

(defn- conf-conform
  "conform every entry of m to spec definition in spec for entry, if any"
  [spec m]
  (->> m
       (map (fn [[k v]]
              (let [k (if (keyword? k) k (kw-name k))
                    vspec (get-in spec [k :spec])]
                [k (if vspec (s/conform vspec v) v)])))
       (into {})))

(defn -conf-spec
  "retrieve config spec"
  [conf]
  (-> conf meta (get :spec)))

(defn -conf-refresh!
  "refresh configuration"
  [conf new-conf]
  (if-let [spec (-conf-spec conf)]
    (let [conformed (select-keys (conf-conform spec new-conf)
                                 (keys spec))
          errors (->> conformed
                      (filter (fn [[k v]] (s/invalid? v)))
                      (into {}))]
      (if (empty? errors)
        (do
          (swap! conf (fn [existing-conf]
                        (merge (->> spec
                                    (map (fn [[k v]] [k (get-in spec [k :default])]))
                                    (filter (fn [[k v]] (not (nil? v))))
                                    (into {}))
                               existing-conf conformed)))
          nil)
        {:error "one or more conf values failed to conform to given specs"
         :fields (keys errors)}))
    {:error "config has no associated `spec` entry in its metadata, use `mkconfig` to create config atom"}))

(defn -read-env!
  "read all entries from env matching a spec entry"
  [spec]
  (->> spec
       (map (fn [[k _]] [k (System/getProperty (env-name k))]))
       (filter (fn [[_ v]] (not (nil? v))))
       (into {})))

(defn -read-file!
  "read EDN-formatted config file"
  [spec fpath]
  (-> (edn/read-string (slurp fpath))
      (select-keys (keys spec))))

(defn refresh-from-env!
  [config]
  (let [spec (-conf-spec config)
        cfg (-read-env! spec)]
    (-conf-refresh! config cfg)))

(defn refresh-from-file!
  [config fpath]
  (let [spec (-conf-spec config)
        cfg (-read-file! spec fpath)]
    (-conf-refresh! config cfg)))

(defn env
  "get value from configuration"
  ([conf k] (get @conf k nil))
  ([conf k default] (get @conf k default)))
