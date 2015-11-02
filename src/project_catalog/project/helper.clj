(ns project-catalog.project.helper
  (:require [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [monger.json]))

(defn get-by-tag [proj-map-in tname]
  (->> proj-map-in
       :content
       (filter #(= (:tag %) tname))
       first
       :content
       first))


(defn xml-out [known-map]
  (xml/element :project {}
    (xml/element :_id {} (.toString (:_id known-map)))
    (xml/element :proj-name {} (:proj-name known-map))
    (xml/element :name {} (:name known-map))
    (xml/element :framework {} (:framework known-map))
    (xml/element :repo {} (:repo known-map))
    (xml/element :language {} (:language known-map))))


(defn monger-mapper [xmlstring]
  "take a raw xml string, and map a known structure into a simple map"
  (let [proj-xml (xml/parse-str xmlstring)]
    {
       :proj-name (get-by-tag proj-xml :proj-name)
       :name (get-by-tag proj-xml :name)
       :framework (get-by-tag proj-xml :framework)
       :language (get-by-tag proj-xml :language)
       :repo (get-by-tag proj-xml :repo)
    }))
