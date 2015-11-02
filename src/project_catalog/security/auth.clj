(ns project-catalog.security.auth
  (:require [clojure.data.json :as json]
            [clojure.data.xml :as xml]

            [clj-http.client :as client]

            [io.pedestal.interceptor.helpers :refer [definterceptor defhandler]]
            [ring.util.response :as ring-resp]))


(defhandler token-check [request]
  (let [token (get-in request [:headers "x-catalog-token"])]
    (if (not (= token "1234"))
      (assoc (ring-resp/response {:body "access denied"}) :status 403))))


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
