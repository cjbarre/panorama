(ns com.be.feat.app
  (:require [clojure.string :as st]
            [malli.core :as mc]
            [malli.error :as me]
            [malli.transform :as mt]
            [cognitect.anomalies :as anom]
            [com.be.anomaly :as a]
            [com.be.db :as db]
            [com.be.time :as t]
            [com.be.schema :as s]
            [com.biffweb :as biff :refer [q lookup system submit-tx]]
            [com.be.command :as c]
            [com.be.query :as query]
            [com.brunobonacci.mulog :as u])
  (:import [java.time ZoneId ZonedDateTime]))

(def command-registry {})

(def query-registry {})

(def features
  {:api-routes ["/api"
                ["/command" {:post (partial #'c/handle-command command-registry)}]
                ["/query" {:post (partial #'query/handle-query query-registry)}]]})