(ns acme-webstore.roles)

(def acme-store-roles
  {:customer 10 :store-admin 11})

(defn any-granted? [req roles]
  ((complement empty?)
   (clojure.set/intersection
    (into #{} (map #(:role-id %) (-> req :auth-user :user-roles)))
    (into #{} (vals (select-keys acme-store-roles roles))))))
