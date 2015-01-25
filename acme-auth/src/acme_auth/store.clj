(ns acme-auth.store
  (:require [clojure.java.jdbc :as jdbc]
            [buddy.hashers :as hs]))



(defn add-user! [ds user]
  (jdbc/with-db-transaction [conn ds]
    (let [res (jdbc/insert! conn
                            :user
                            [:username :password]
                            [(:username user) (:password user)])
          user-id (first res)]
      (doseq [ur (:user-roles user)]
        (jdbc/insert! conn
                      :user_role
                      [:user_id :role_id]
                      [user-id (:role-id ur)])))))


(defn- find-user-roles [conn user-id]
  (map (fn [row] {:role-id (:id row) :application-id (:application_id row)})
       (jdbc/query conn ["select r.id, r.application_id
                         from role r
                         inner join user_role ur on r.id = ur.role_id
                         where ur.user_id = ?" user-id])))

(defn find-user [ds username]
  (jdbc/with-db-connection [conn ds]
    (when-let [user
          (first
           (jdbc/query conn ["select * from user where username = ?" username]))]
      (assoc user :user-roles (find-user-roles conn (:id user))))))
