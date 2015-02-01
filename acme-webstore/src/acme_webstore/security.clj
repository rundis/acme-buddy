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

(defn create-token [req]
  (http/post "http://localhost:6001/create-auth-token"
             {:content-type :json
              :accept :json
              :throw-exceptions false
              :as :json
              :form-params (select-keys (:params req) [:username :password])}))

(defn do-login [req]
  (let [resp (create-token req)]
    (condp = (:status resp)
      201 (-> (response/redirect (if-let [m (get-in req [:query-params "m"])] m "/dashboard"))
              (assoc :session {:token (-> resp :body :token)}))
      401 (show-login req ["Invalid username or password"])
      {:status 500 :body "Something went pearshape when trying to authenticate"})))


(defn logout [req]
  (assoc (response/redirect "/") :session nil))

(defn wrap-auth-cookie [handler cookie-secret]
  (-> handler
      (wrap-session
       {:store (cookie-store {:key cookie-secret})
        :cookie-name "acme"
        :cookie-attrs {:max-age (* 60 60 24)}}))) ;; you should probably add :secure true to enforce https


(defn unsign-token [token]
  (jws/unsign token (ks/public-key (io/resource "auth_pubkey.pem")) {:alg :rs256}))


(defn wrap-auth-token [handler]
  (fn [req]
    (let [user (:user (when-let [token (-> req :session :token)]
                   (unsign-token token)))]
      (handler (assoc req :auth-user user)))))

(defn wrap-authentication [handler]
  (fn [req]
    (if (:auth-user req)
      (handler req)
      {:status 302
       :headers {"Location " (str "/login?m=" (:uri req))}})))


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
      (if loc
        (if (some #(re-matches % loc) redirect-whitelist)
          resp
          (do
            ;; (log/warning "Possible redirect attack: " loc)
            (assoc-in resp [:headers "Location"] "/")))
        resp))))

