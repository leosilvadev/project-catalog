(ns project-catalog.project.api
  (:require [clojure.data.json :as json]
            [clojure.data.xml :as xml]

            [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor.helpers :refer [definterceptor defhandler]]
            [ring.util.response :as ring-resp]

            [clj-http.client :as client]

            [monger.collection :as mc]
            [monger.json]

            [project-catalog.mongodb :as db]))


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


(defn git-search [q]
  (let [result (client/get (format "https://api.github.com/search/repositories?q=%s+language:clojure" q)
            {:debug false
             :content-type :json
             :accept :json})]
      (json/read-str (result :body))))



(defn get-projects
  [request]
  (bootstrap/json-response
    (mc/find-maps db/mongo-db db/catalogs-coll)))


(defn add-project
  [request]
  (let [incoming (:json-params request)])
  (let [result (mc/insert-and-return db/mongo-db db/catalogs-coll (:json-params request))]
    (bootstrap/json-response {:id (:_id result)})))


(defn add-project-xml
  [request]
  (let [incoming (slurp (:body request))
        ok (mc/insert-and-return db/mongo-db db/catalogs-coll (monger-mapper incoming))]
    (-> (ring-resp/created "http://resource-for-my-created-item" (xml/emit-str (xml-out ok)))
        (ring-resp/content-type "application/xml"))))


(defn get-project
  [request]
  (let [name (get-in request [:path-params :name])]
    (bootstrap/json-response (mc/find-maps db/mongo-db db/catalogs-coll {:name name}))))


(defn git-get [request]
  (bootstrap/json-response (git-search (get-in request [:query-params :q]))))
