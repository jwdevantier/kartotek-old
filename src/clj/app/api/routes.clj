(ns app.api.routes
  (:require [app.api.tags :as tags]
            [app.api.notes :as notes]
            [app.api.search :as search]))

(def routes
  [["/api/tags"
    [""
     {:get {:summary "get all tags and the number of times they are used"
            :handler tags/all-tags}}]
    ["/:tag"
     {:get {:summary "get all notes using tag"
            :handler tags/notes-list}}]]
   ["/api/search"
    ["/notes"
     {:post {:summary "retrieve notes matching search query"
             :handler search/query}}]
    ["/notes/help"
     {:get {:summary "retrieve help on searching for notes"
             :handler search/help}}]]
   ["/api/notes"
    ["/:note-id"
     {:get {:summary "return rendered note"
            :handler notes/note-show}}]]])
