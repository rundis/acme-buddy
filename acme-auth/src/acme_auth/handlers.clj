(ns acme-auth.handlers
  (:require [acme-auth.service :as service]))


(defn create-auth-token [req]
  (let [[ok? res] (service/create-auth-token (:datasource req)
                                           (:auth-conf req)
                                           (:params req))]
    (if ok?
      {:status 201 :body res}
      {:status 401 :body res})))
