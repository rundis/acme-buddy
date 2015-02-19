(ns acme-auth.service
  (:require [acme-auth.store :as store]
            [buddy.hashers :as hs]
            [buddy.sign.generic :as sign]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as ks]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]))


;TODO : Validation, password policy etc..
(defn add-user! [ds user]
  (store/add-user! ds (update-in user [:password] #(hs/encrypt %))))


(defn auth-user [conn credentials]
  (let [user (store/find-user-by-username conn (:username credentials))
        unauthed [false {:message "Invalid username or password"}]]
    (if user
      (if (hs/check (:password credentials) (:password user))
        [true {:user (dissoc user :password)}]
        unauthed)
      unauthed)))

(defn- priv-key [auth-conf]
  (ks/private-key
   (io/resource (:privkey auth-conf))
   (:passphrase auth-conf)))

(defn- pub-key [auth-conf]
  (ks/public-key
   (io/resource (:pubkey auth-conf))))


(defn- unsign-token [auth-conf token]
  (jws/unsign token (pub-key auth-conf)))

(defn- make-auth-token [auth-conf user]
  (let [exp (-> (t/plus (t/now) (t/minutes 30)) (jws/to-timestamp))]
    (jws/sign {:user (dissoc user :password)}
              (priv-key auth-conf)
              {:alg :rs256 :exp exp})))

(defn- make-refresh-token! [conn auth-conf user]
  (let [iat (jws/to-timestamp (t/now))
        token (jws/sign {:user-id (:id user)}
                        (priv-key auth-conf)
                        {:alg :rs256 :iat iat :exp (-> (t/plus (t/now) (t/days 30)) (jws/to-timestamp))})]

    (store/add-refresh-token! conn {:user_id (:id user)
                                    :issued iat
                                    :token token})
    token))

(defn make-token-pair! [conn auth-conf user]
  {:token-pair {:auth-token (make-auth-token auth-conf user)
                :refresh-token (make-refresh-token! conn auth-conf user)}})


(defn create-auth-token [ds auth-conf credentials]
  (jdbc/with-db-transaction [conn ds]
    (let [[ok? res] (auth-user conn credentials)]
      (if ok?
        [true (make-token-pair! conn auth-conf (:user res))]
        [false res]))))

(defn refresh-auth-token [ds auth-conf refresh-token]
  (if-let [unsigned (unsign-token auth-conf refresh-token)]
    (jdbc/with-db-transaction [conn ds]
      (let [db-token-rec (store/find-token-by-unq-key conn (:user-id unsigned) (:iat unsigned))
            user (store/find-user-by-id conn (:user_id db-token-rec))]
        (if (:valid db-token-rec)
          (do
            (store/invalidate-token! conn (:id db-token-rec))
            [true (make-token-pair! conn auth-conf user)])
          [false {:message "Refresh token revoked/deleted or new refresh token already created"}])))
    [false {:message "Invalid or expired refresh token provided"}]))


(defn invalidate-refresh-token [ds auth-conf refresh-token]
  (let [unsigned (unsign-token auth-conf refresh-token)]
    (if unsigned
      (do
        (jdbc/with-db-transaction [conn ds]
          (store/invalidate-token! conn (:user-id unsigned) (:issued unsigned)))
        [true {:message "Invalidated succssfully"}])
      [false {:message "Invalid or expired refresh token provided"}])))
