(ns frontend.changelog
  (:require [goog.dom.xml :as xml]))


(defn attr-text [item attr]
  (.-textContent (xml/selectSingleNode item attr)))

(defn parse-item [item]
  {:title (attr-text item "title")
   :description (attr-text item "description")
   :link (attr-text item "link")
   :author (attr-text item "author")
   :pubDate (attr-text item "pubDate")
   :guid (attr-text item "guid")
   :type (attr-text item "type")
   :categories (map #(.-textContent %) (xml/selectNodes item "category"))})

(defn parse-changelog-document [xml-document-object]
  (let [items (xml/selectNodes xml-document-object "//item")]
    (map parse-item items)))
