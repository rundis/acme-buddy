(ns acme-auth.datasource
  (:require [hikari-cp.core :refer :all]))


(def datasource-options {:adapter "h2"
                         :url     "jdbc:h2:mem:test"})


(defn get-ds []
  (defonce ds (make-datasource datasource-options))
  {:datasource ds})
