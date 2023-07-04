(ns com.be.schema
  (:require [malli.core :as m]
            [cognitect.anomalies :as anom]
            [com.be.time :refer [time-zone-ids]]))

(defn anomaly? [x] (when (::anom/category x) x))

(def schema
  {:user/id :uuid
   :user/email :string
   :user/joined-at inst?
   :user [:map {:closed true}
          [:xt/id :user/id]
          :user/email
          :user/joined-at]

   :msg/id :uuid
   :msg/user :user/id
   :msg/text :string
   :msg/sent-at inst?
   :msg [:map {:closed true}
         [:xt/id :msg/id]
         :msg/user
         :msg/text
         :msg/sent-at]})

(def features
  {:schema schema})
