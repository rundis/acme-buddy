(ns acme-auth.bootstrap
  (:require [clojure.java.jdbc :as jdbc]
            [acme-auth.service :as service]))


(defn create-db [ds]
  (jdbc/with-db-connection [conn ds]
    (jdbc/db-do-commands conn
                         "create table user (
                           id integer auto_increment primary key,
                           username varchar(255) not null,
                           password varchar(255) not null)"

                         "create table application (
                           id integer auto_increment primary key,
                           name varchar(255) not null)"

                         "create table role (
                           id integer auto_increment primary key,
                           application_id integer not null,
                           name varchar(255) not null,

                           foreign key (application_id) references application (id))"

                         "create table user_role (
                           id integer auto_increment primary key,
                           role_id integer not null,
                           user_id integer not null,

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

  ;;(println (service/find-user ds "admin"))
  )
