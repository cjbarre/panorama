(ns com.be.schema
  (:require [malli.core :as m]
            [cognitect.anomalies :as anom]
            [com.be.time :refer [time-zone-ids]]))

(defn anomaly? [x] (when (::anom/category x) x))

(def schema
  {})

(def features
  {:schema schema})
