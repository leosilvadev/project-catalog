(ns project-catalog.service
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

            [project-catalog.mongodb :as db]
            [project-catalog.project.api :as api]))



(defhandler token-check [request]
  (let [token (get-in request [:headers "x-catalog-token"])]
    (if (not (= token "1234"))
      (assoc (ring-resp/response {:body "access denied"}) :status 403))))

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



(defn auth0-token []
  (let [ret
        (client/post "https://jemez.auth0.com/oauth/token"
                     {:debug false
                      :content-type :json
                      :form-params {:client_id (System/getenv "AUTH0_CLIENT_ID")
                                    :client_secret (System/getenv "AUTH0_SECRET")
                                    :grant_type "client_credentials"}})]
    (json/read-str (ret :body))))


(defn auth0-connections [token]
  (let [ret
        (client/get "https://jemez.auth0.com/api/connections"
                    {:debug false
                     :content-type :json
                     :accept :json
                     :headers {"Authorization" (format "Bearer %s" token)}})]
    (ret :body)))



(auth0-connections ((auth0-token) "access_token"))


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

