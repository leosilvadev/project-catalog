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

            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.json]))



(def raw-proj-string "<project>
    <proj-name>xml test project</proj-name>
    <name>Important Project</name>
    <language>Clojure</language>
    <repo>test</repo>
  </project>")

(def proj-xml (xml/parse-str raw-proj-string))

(:content proj-xml)

(defn get-by-tag [proj-map-in tname]
  (->> proj-map-in
       :content
       (filter #(= (:tag %) tname))
       first
       :content
       first))


(get-by-tag proj-xml :name)

(defn git-search [q]
  (let [result (client/get (format "https://api.github.com/search/repositories?q=%s+language:clojure" q)
            {:debug false
             :content-type :json
             :accept :json})]
      (json/read-str (result :body))))

(defn git-get [request]
  (bootstrap/json-response (git-search (get-in request [:query-params :q]))))



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



(defonce mongo-url (System/getenv "MONGO_CONNECTION"))

(defonce catalogs-coll "catalogs")

(defonce mongo-db
  (:db (mg/connect-via-uri (System/getenv "MONGO_CONNECTION"))))

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

(defn get-projects
  [request]
  (bootstrap/json-response
    (mc/find-maps mongo-db catalogs-coll)))


(defn add-project
  [request]
  (let [incoming (:json-params request)])
  (let [result (mc/insert-and-return mongo-db catalogs-coll (:json-params request))]
    (bootstrap/json-response {:id (:_id result)})))


(defn xml-out [known-map]
  (xml/element :project {}
    (xml/element :_id {} (.toString (:_id known-map)))
    (xml/element :proj-name {} (:proj-name known-map))
    (xml/element :name {} (:name known-map))
    (xml/element :framework {} (:framework known-map))
    (xml/element :repo {} (:repo known-map))
    (xml/element :language {} (:language known-map))))


(defn add-project-xml
  [request]
  (let [incoming (slurp (:body request))
        ok (mc/insert-and-return mongo-db catalogs-coll (monger-mapper incoming))]
    (-> (ring-resp/created "http://resource-for-my-created-item" (xml/emit-str (xml-out ok)))
        (ring-resp/content-type "application/xml"))))

(defn get-project
  [request]
  (let [name (get-in request [:path-params :name])]
    (bootstrap/json-response (mc/find-maps mongo-db catalogs-coll {:name name}))))


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
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/"
     ^:interceptors [(body-params/body-params) bootstrap/html-body token-check]
     ["/projects" {:get get-projects
                   :post add-project}]
     ["/projects-xml" {:post add-project-xml}]
     ["/see-also" {:get git-get}]
     ["/projects/:name" {:get get-project}]
     ["/about" {:get about-page}]]]])

;; Consumed by project-catalog.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :immutant
              ;;::bootstrap/host "localhost"
              ::bootstrap/port 5000})

