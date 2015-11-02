(ns project-catalog.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]

            [project-catalog.mongodb :as db]
            [project-catalog.project.api :as api]))



(def projects
  {:java {:name "Java Project" :deadline 20 :coders 5}
   :clojure {:name "Clojure Project" :deadline 30 :coder 3}
   :scala {:name "Scala Project" :deadline 28 :coder 4}
   :groovy {:name "Groovy Project" :deadline 29 :coder 3}})


(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))


(defroutes routes
  [[["/"
     ^:interceptors [(body-params/body-params) bootstrap/html-body token-check]
     ["/projects" {:get api/get-projects
                   :post api/add-project}]
     ["/projects-xml" {:post api/add-project-xml}]
     ["/see-also" {:get api/git-get}]
     ["/projects/:name" {:get api/get-project}]]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ;;::bootstrap/allowed-origins ["scheme://host:port"]
              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :immutant
              ;;::bootstrap/host "localhost"
              ::bootstrap/port 5000})

