(ns project-catalog.project.api
  (:require [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [clj-http.client :as client]

            [io.pedestal.http :as bootstrap]
            [ring.util.response :as ring-resp]

            [monger.collection :as mc]
            [monger.json]

            [project-catalog.mongodb :as db]
            [project-catalog.project.helper :as helper]))





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
        ok (mc/insert-and-return db/mongo-db db/catalogs-coll (helper/monger-mapper incoming))]
    (-> (ring-resp/created "http://resource-for-my-created-item" (xml/emit-str (helper/xml-out ok)))
        (ring-resp/content-type "application/xml"))))


(defn get-project
  [request]
  (let [name (get-in request [:path-params :name])]
    (bootstrap/json-response (mc/find-maps db/mongo-db db/catalogs-coll {:name name}))))


(defn git-get [request]
  (bootstrap/json-response (git-search (get-in request [:query-params :q]))))
