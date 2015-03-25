(ns acme-webstore.catalog
  (:require [clj-http.client :as http]))


(defn get-from-catalog [path token]
  (http/get path {:headers {"Authorization" (str "Acme-Token " token)}}))

(defn get-products [req]
  (let [auth-token (-> req :session :token-pair :auth-token)
        resp (get-from-catalog "http://localhost:6003/products" auth-token)]
    (:body resp)))
