(ns acme-catalog.core
  (:require [compojure.core :refer [defroutes ANY]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params]]
            [clojure.java.io :as io]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.core.keys :as ks]
            [acme-catalog.resources :as r]))


(defn bootstrap []
  (println "Doing whatever bootstrap I need, perhaps initialize connection pool ?"))


(defroutes app-routes
  (ANY "/products" [] r/products)
  (ANY "/products/:id" [id] (r/product id)))


(def auth-backend (jws-backend {:secret (ks/public-key (io/resource "auth_pubkey.pem"))
                                :token-name "Acme-Token"}))

(def app
  (-> app-routes
      (wrap-authentication auth-backend)
      wrap-keyword-params
      wrap-json-params))
