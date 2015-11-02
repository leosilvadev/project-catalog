(ns project-catalog.mongodb
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.json]))


(defonce mongo-url (System/getenv "MONGO_CONNECTION"))

(defonce catalogs-coll "catalogs")

(defonce mongo-db
  (:db (mg/connect-via-uri (System/getenv "MONGO_CONNECTION"))))
