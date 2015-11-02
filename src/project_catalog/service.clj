(ns project-catalog.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]

            [project-catalog.mongodb :as db]
            [project-catalog.project.api :as api]))


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

