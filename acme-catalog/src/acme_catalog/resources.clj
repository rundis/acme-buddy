(ns acme-catalog.resources
  (:require [liberator.core :refer [defresource by-method]]
            [liberator.representation :refer [ring-response]]
            [buddy.auth :refer [authenticated?]]))


(def acme-catalog-roles
  {:customer 41 :catalog-admin 40})

(defn any-granted? [ctx roles]
  (seq
   (clojure.set/intersection
    (set (map :role-id (-> ctx :request :identity :user :user-roles)))
    (set (vals (select-keys acme-catalog-roles roles))))))

(defn secured-resource [m]
  {:authorized?    #(authenticated? (:request %))
   :allowed?       (fn [ctx]
                     (let [default-auth? (any-granted? ctx (keys acme-catalog-roles))]
                       (if-let [auth-fn (:allowed? m)]
                         (and default-auth? (auth-fn ctx))
                         default-auth?)))})



(defresource products
  (secured-resource {:allowed? (by-method {:get true
                                           :post #(any-granted? % [:catalog-admin])})})
  :available-media-types ["application/json"]
  :allowed-methods       [:get :post]
  :handle-ok             (fn [ctx] "List of products coming your way honey"))


(defresource product [id]
  (secured-resource {:allowed? (by-method {:get true
                                           :delete #(any-granted? % [:catalog-admin])
                                           :put #(any-granted? % [:catalog-admin])})})
  :available-media-types ["application/json"]
  :allowed-methods       [:get :put :delete]
  :handle-ok             (fn [ctx]
                           (if (and (= "99" id) (not (any-granted? ctx [:catalog-admin])))
                             (ring-response {:status 403 :headers {} :body "Only admins can access product 99"})
                             "A single product returned")))


