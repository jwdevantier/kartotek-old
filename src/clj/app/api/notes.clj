(ns app.api.notes
  (:require [clojure.pprint :as pp]
            [ring.util.response :refer [response]]
            [ring.util.request :refer [body-string]]
            [hiccup.core :as hc]
            [app.jsend :as jsend]
            [app.state :as state]
            [app.notes :as notes]
            [app.filedb :as db]))

(letfn [(mksection [container elems]
          ; constructs an element, [[~@container ~@elems]] where elems
          ; is now the subset of non-nil elements.
          ; The double-vector wrapping is needed to retain a single element
          ; when finally concat'ing all elements together in the body of -note->hiccup
          (let [elems (filter (complement nil?) elems)]
            (if (empty? elems)
              nil
              [(->> elems (concat container) vec)])))]
  (defn -note->hiccup
    "create hiccup structure of note's contents and metadata."
    [{content :content {:keys [title description tags links]} :meta}]
    (let [doc-title (let [e (get content 2)]
                      (when (= :h1 (first e)) (some #(and (string? %) %) e)))
          title (or doc-title title)
          [before after] (split-at 2 content)]
      (vec (concat before
                   (mksection [:header] [[:h1 (str title "!")]
                                         (when description
                                           [:p {:class "description"} description])])
                   (if doc-title (drop 1 after) after)
                   (mksection [:footer] [(when tags
                                           [:div {:class "tags"}
                                            [:span "Tags: "]
                                            [:ul
                                             (for [t tags] [:li [:a {:href (str "/tags/" t)} t]])]])

                                         (when (not (empty? links))
                                           [:div {:class "links"}
                                            [:span "Links: "]
                                            [:ul
                                             (for [l links] [:li [:a {:href (str "/notes/" l)} l]])]])]))))))

(defn note-show
  "return rendered note HTML"
  [rq]
  (let [id (-> rq :path-params :note-id)
        note-dir (state/get-notes-path)
        note (notes/parse-md-doc note-dir id)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> note -note->hiccup hc/html)}))
