(ns acme-webstore.roles)

(def acme-store-roles
  {:customer 10 :store-admin 11})

(defn any-granted? [req roles]
  (seq
   (clojure.set/intersection
    (set (map :role-id (-> req :auth-user :user-roles)))
    (set (vals (select-keys acme-store-roles roles))))))

