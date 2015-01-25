(ns acme-auth.bootstrap
  (:require [clojure.java.jdbc :as jdbc]
            [acme-auth.service :as service]))


(defn create-db [ds]
  (jdbc/with-db-connection [conn ds]
    (jdbc/db-do-commands conn
                         "create table user (
                           id int identity primary key,
                           username varchar(255) not null,
                           password varchar(255) not null)"

                         "create table application (
                           id int primary key,
                           name varchar(255) not null)"

                         "create table role (
                           id int identity primary key,
                           application_id int not null,
                           name varchar(255) not null,

                           foreign key (application_id) references application (id))"

                         "create table user_role (
                           id int identity primary key,
                           role_id int not null,
                           user_id int not null,

                           foreign key (role_id) references role (id),
                           foreign key (user_id) references user (id))")))


(defn seed [ds]
  (jdbc/with-db-transaction [conn ds]
    (jdbc/insert! conn :application
                  [:id :name]
                  [10 "acme-webstore"]
                  [20 "acme-crm"]
                  [30 "acme-admin"])
    (jdbc/insert! conn :role
                  [:id :application_id :name]
                  [10 10 "customer"]
                  [11 10 "store-admin"]
                  [20 20 "customer-support"]
                  [21 20 "accounting"]
                  [30 30 "sysadmin"]))

  ;; add a couple of users for testing/demo
  (service/add-user! ds {:username "test"
                         :password "secret"
                         :user-roles [{:role-id 10}]})
  (service/add-user! ds {:username "admin"
                         :password "secret"
                         :user-roles [{:role-id 11}]})

  (println (service/find-user ds "test")))
