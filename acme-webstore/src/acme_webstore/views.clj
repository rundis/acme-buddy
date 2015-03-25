(ns acme-webstore.views
  (:require [hiccup.page :as page]
            [acme-webstore.roles :refer [any-granted?]]))


(defn include-page-styles [sources]
  (map #(page/include-css %) sources))


(defn- render-menu [req]
  (let [user (:auth-user req)]
    [:nav.menu
     [:div {:class "collapse navbar-collapse bs-navbar-collapse navbar-inverse"}
      [:ul.nav.navbar-nav
       [:li [:a {:href (if user "/dashboard" "/")} "Home"]]
       (when user
         (list [:li [:a {:href (str "/accounts/" (:id user))} "My account"]]
               [:li [:a {:href "/products"} "Products"]]))
       (when (any-granted? req [:store-admin])
         [:li [:a {:href "/accounts"} "Account listing"]])]
      [:ul.nav.navbar-nav.navbar-right
       (if user
         [:li [:a {:href "/logout"} "Logout"]]
         [:li [:a {:href "/login"} "Login"]])]]]))

(defn show-errors [errors]
  [:div {:class "alert alert-danger"}
   [:ul
    (map #(vec [:li %]) errors)]])


(defn layout [{title :title content :content req :request :as props}]
  (page/html5
   [:head
    (include-page-styles
     (concat [ "/css/bootstrap.min.css"
               "/css/main.css"]))
    [:title title]]
   [:div {:class "container"}
    (render-menu req)]
   [:body
    [:div.container
     [:h1 title]
     (when-let [errors (:errors props)] (show-errors errors))
     content]]))


(defn index [req]
  [:div
   [:p "Welcome to the Acme Corp Webstore"]])


(defn dashboard [req]
  [:div
   [:h3 (str "Greetings " (-> req :auth-user :username) " !")]
   [:p "Welcome to your personalized Acme Corp Webstore"]])


(defn input [attrs]
  [:div
    [:label {:for (:field attrs) :class "control-label"} (:label attrs)]
     [:input {:type (or (:type attrs) "text")
              :class "form-control"
              :id (:field attrs)
              :name (:field attrs)
              :placeholder (:label attrs)}]])

(defn login [req]
  [:div {:class "row"}
   [:div {:class "col-sm-9 col-lg-10"} [:p {} "Login to acme store to get all the benefits..."]]
   [:div {:class "col-sm-3 col-lg-2"}
    [:form {:role "form" :method "POST"}
     [:div {:class "form-group"} (input {:field "username" :label "Username"})]
     [:div {:class "form-group"} (input {:field "password" :label "Password" :type "password"})]
     [:div {:class "form-group"} [:button {:type "submit" :class "btn btn-default"} "Login"]]]]])


(defn account [req]
  [:div "Showing account info for logged in user here..."])


(defn accounts [req]
  [:div "Showing listing of accounts, only visible to store admins"])
