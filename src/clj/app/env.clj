(ns app.env
  (:require [clojure.spec.alpha :as s]
            [app.env-impl :as envi]))

(s/def ::env #{"DEV" "PROD"})

(def config-spec
  {:env {:default "PROD"
         :spec ::env}})

(defonce config (envi/mkconfig config-spec))

(defn dev? []
  (= (envi/env config :env) "DEV"))
