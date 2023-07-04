(ns com.be.query
  (:require [com.brunobonacci.mulog :as u]
            [cognitect.anomalies :as anom]
            [com.be.schema :as s]
            [malli.transform :as mt]
            [malli.core :as mc]
            [malli.error :as me]))

(def strict-json-transformer
  (mt/transformer
   mt/strip-extra-keys-transformer
   mt/json-transformer))

(defn process-query [query-registry {:keys [api/query biff/malli-opts biff/db] :as context}]
  (let [query-name (keyword (:query/name query))
        handler (get query-registry query-name)
        query (-> (mc/decode query-name query @malli-opts strict-json-transformer)
                    (assoc :query/name query-name))
        context (assoc context :api/query query)]
    (if handler
      (if-let [_ (mc/validate query-name query @malli-opts)]
        (:result (handler context))
        {::anom/category ::anom/incorrect
         ::anom/message "Invalid Query: Failed Schema Validation"
         :error/explain (str (me/humanize (mc/explain query-name query @malli-opts)))})
      {::anom/category ::anom/not-found
       ::anom/message "Unknown Query"})))

;;;;;;;;;;;;;;
;;;; HTTP ;;;;
;;;;;;;;;;;;;;

(defn process-query-result-dispatch
  [result]
  (::anom/category result))

(defmulti process-query-result process-query-result-dispatch)

(defmethod process-query-result ::anom/incorrect
  [{:keys [::anom/message error/explain]}]
  {:status 400
   :body {:message message
          :explain explain}})

(defmethod process-query-result ::anom/not-found
  [{:keys [::anom/message]}]
  {:status 404
   :body {:message message}})

(defmethod process-query-result :default
  [{:keys [::anom/message]}]
  {:status 500
   :body {:message message}})

(defmethod process-query-result nil
  [result]
  {:status 200
   :headers {"content-type" "application/edn"}
   :body (str {:result result})})

(defn handle-query [query-registry {:keys [body-params] :as req}]
  (let [query (:query body-params)]
    (u/with-context {:api/query query}
      (u/trace ::process-query []
               (let [result (process-query query-registry (assoc req :api/query query))]
                 (when (s/anomaly? result)
                   (u/log ::anomaly :anomaly result))
                 (u/log ::process-query-result :result result)
                 (process-query-result result))))))