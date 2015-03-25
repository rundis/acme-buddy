(ns acme-webstore.web
  (:require [acme-webstore.security :as sec]
            [acme-webstore.views :as views]
            [acme-webstore.catalog :as catalog]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.absolute-redirects :refer [wrap-absolute-redirects]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))


(defn show-index [req]
  (views/layout {:request req :title "Welcome" :content (views/index req)}))

(defn show-dashboard [req]
  (views/layout {:request req :title "Dashboard" :content (views/dashboard req)}))

(defn show-account [req]
  (views/layout {:request req :title "Account" :content (views/account req)}))

(defn show-accounts [req]
  (views/layout {:request req :title "Account listing" :content (views/accounts req)}))

(defn show-products [req]
  (views/layout {:request req :title "Products" :content (catalog/get-products req)}))


(defroutes public-routes
  (route/resources "/")
  (GET "/" []       show-index)
  (GET "/login" []  sec/show-login)
  (POST "/login" [] sec/do-login)
  (GET "/logout" [] sec/logout))


(defroutes secured-routes
  (GET "/accounts/:id" [] show-account)
  (GET "/accounts" []     (sec/wrap-restrict-by-roles show-accounts [:store-admin]))
  (GET "/products" []     show-products)
  (GET "/dashboard" []    show-dashboard))


(defroutes app-routes
  (-> public-routes
      sec/wrap-auth-token)
  (-> secured-routes
      sec/wrap-authentication
      sec/wrap-auth-token))

(def app (-> app-routes
             wrap-keyword-params
             wrap-params
             wrap-absolute-redirects
             sec/wrap-authorized-redirects
             (sec/wrap-auth-cookie "SoSecret12345678")))
