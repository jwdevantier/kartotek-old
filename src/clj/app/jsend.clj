(ns app.jsend
  (:require [ring.util.response :refer [response]]))

; See https://github.com/omniti-labs/jsend for details.

(defn- with-json-content-type
  [headers]
  (assoc headers "Content-Type" "application/json; charset=utf-8"))

(defn- filter-nil-entries [m]
  (into {} (filter (comp (complement nil?) val) m)))

(defn success
  "send a JSEND-compliant success response"
  ([data] (success data {}))
  ([data {:keys [http-status headers] :or {http-status 200 headers {}}}]
   {:status http-status
    :headers (with-json-content-type headers)
    :body {"status" "success" "data" data}}))

(defn fail
  "send a JSEND-compliant failure response"
  ([data] (fail data {}))
  ([data {:keys [http-status headers] :or {http-status 500 headers {}}}]
   {:status http-status
    :headers (with-json-content-type headers)
    :body {"status" "fail" "data" data}}))

(defn error
  "send a JSEND-compliant error response"
  ([message] (error message {}))
  ([message {:keys [code data http-status headers]
             :or {http-status 500 headers {}}}]
   {:status http-status
    :headers (with-json-content-type headers)
    :body (-> {"status" "error" "message" message
               "code" code "data" data}
              filter-nil-entries)}))
