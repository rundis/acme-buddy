(ns acme-auth.store
  (:require [clojure.java.jdbc :as jdbc]))



(defn add-user! [ds user]
  (jdbc/with-db-transaction [conn ds]
    (let [res (jdbc/insert! conn
                            :user
                            {:username (:username user) :password (:password user)})
          user-id ((keyword "scope_identity()") (first res))]
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

(defn find-user-by-username [conn username]
  (when-let [user
             (first
              (jdbc/query conn ["select * from user where username = ?" username]))]
    (assoc user :user-roles (find-user-roles conn (:id user)))))

(defn find-user-by-id [conn id]
  (when-let [user
             (first
              (jdbc/query conn ["select * from user where id = ?" id]))]
    (assoc user :user-roles (find-user-roles conn (:id user)))))



(defn add-refresh-token! [conn params]
  (jdbc/insert! conn :refresh_token params))

(defn invalidate-token!
  ([conn id]
   (jdbc/update! conn :refresh_token {:valid false} ["id = ?" id]))
  ([conn user-id issued]
   (jdbc/update! conn :refresh_token {:valid false} ["user_id = ? and issued = ?" user-id issued])))

(defn find-token-by-unq-key [conn user-id issued]
  (first
   (jdbc/query conn ["select * from refresh_token where user_id = ? and issued = ?" user-id issued])))
