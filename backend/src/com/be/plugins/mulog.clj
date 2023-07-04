(ns com.be.plugins.mulog
  (:require [com.brunobonacci.mulog :as u]))

(defonce started (atom false))

(defn use-mulog
  [sys]
  (when (not @started)
    (u/start-publisher! {:type :console-json
                         :pretty? true}))
  (swap! started not)
  sys)