(ns acme-webstore.security
  (:require [acme-webstore.views :as views]
            [acme-webstore.roles :refer [any-granted?]]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.response :as response]
            [clj-http.client :as http]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as ks]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [clojure.java.io :as io]))


(defn show-login
  ([req] (show-login req nil))
  ([req errors]
   (views/layout {:request req
                  :title "Login"
                  :content (views/login req)
                  :errors errors})))

(defn post-to-auth [path params]
  (http/post (str "http://localhost:6001/" path)
             {:content-type :json
              :accept :json
              :throw-exceptions false
              :as :json
              :form-params params}))

(defn create-token [credentials]
  (post-to-auth "create-auth-token" credentials))

(defn refresh-auth-token [token]
  (let [resp (post-to-auth "refresh-auth-token" {:refresh-token token})]
    (if (= (:status resp) 201)
      [true (:body resp)]
      [false (:body resp)])))

(defn invalidate-refresh-token [token]
  (let [resp (post-to-auth "invalidate-refresh-token" {:refresh-token token})]
    (if (= (:status resp) 200)
      [true (:body resp)]
      [false (:body resp)])))


(defn do-login [req]
  (let [resp (create-token (-> req :params (select-keys [:username :password])))]
    (condp = (:status resp)
      201 (-> (response/redirect (get-in req [:query-params "m"] "/dashboard"))
              (assoc :session {:token-pair (-> resp :body :token-pair)}))
      401 (show-login req ["Invalid username or password"])
      {:status 500 :body "Something went pearshape when trying to authenticate"})))


(defn logout [req]
  (when-let [refresh-token (-> req :session :token-pair :refresh-token)]
    (let [[ok? res] (invalidate-refresh-token refresh-token)]
      (when-not ok?
        (println "Warning : Failed to invalidate refresh token, action should be taken..."))))
  (assoc (response/redirect "/") :session nil))

(defn wrap-auth-cookie [handler cookie-secret]
  (-> handler
      (wrap-session
       {:store (cookie-store {:key cookie-secret})
        :cookie-name "acme"
        :cookie-attrs {:max-age (* 60 60 24 30)}}))) ;; you should probably add :secure true to enforce https


(defn unsign-token [token]
  (jws/unsign token (ks/public-key (io/resource "auth_pubkey.pem"))))


(defn wrap-auth-token [handler]
  (fn [req]
    (let [auth-token (-> req :session :token-pair :auth-token)
          unsigned-auth (when auth-token (unsign-token auth-token))]
      (if unsigned-auth
        (handler (assoc req :auth-user (:user unsigned-auth)))
        (handler req)))))

(defn- handle-token-refresh [handler req refresh-token]
  (let [[ok? res] (refresh-auth-token refresh-token)
        user (:user (when ok? (unsign-token (-> res :token-pair :auth-token))))]
    (if user
      (-> (handler (assoc req :auth-user user))
          (assoc :session {:token-pair (:token-pair res)}))
      {:status 302
       :headers {"Location " (str "/login?m=" (:uri req))}})))

(defn wrap-authentication [handler]
  (fn [req]
    (if (:auth-user req)
      (handler req)
      (let [refresh-token (-> req :session :token-pair :refresh-token)]
        (if refresh-token
          (handle-token-refresh handler req refresh-token)
          {:status 302
           :headers {"Location " (str "/login?m=" (:uri req))}})))))

(defn wrap-restrict-by-roles [handler roles]
  (fn [req]
    (if (any-granted? req roles)
      (handler req)
      {:status 401 :body "You are not authorized for this feature"})))


(def redirect-whitelist
  [#"http://localhost:6002/.*"])

(defn wrap-authorized-redirects [handler]
  (fn [req]
    (let [resp (handler req)
          loc (get-in resp [:headers "Location"])]
      (if (and loc (not (some #(re-matches % loc) redirect-whitelist)))
        (do
            ;; (log/warning "Possible redirect attack: " loc)
            (assoc-in resp [:headers "Location"] "/"))
        resp))))
