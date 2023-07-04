(ns com.be.test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [cognitect.anomalies :as anom]
            [com.be.schema :refer [anomaly?]]
            [com.be.anomaly :as a]
            [com.be.feat.app :as app]))
