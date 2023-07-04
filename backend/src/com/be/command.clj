(ns com.be.command
  (:require [malli.core :as mc]
            [com.biffweb :as biff :refer [submit-tx]]
            [malli.transform :as mt]
            [cognitect.anomalies :as anom]
            [malli.error :as me]
            [com.be.schema :as s]
            [com.be.anomaly :as a]
            [com.brunobonacci.mulog :as u]
            [com.be.time :as t]))

(defn with-fn
  [handler key f]
  (fn [context]
    (handler (assoc context key f))))

;;;;;;;;;;;;;;
;;;; Core ;;;;
;;;;;;;;;;;;;;

(def strict-json-transformer
  (mt/transformer
   mt/strip-extra-keys-transformer
   mt/json-transformer))

(defn process-command [command-registry {:keys [api/command biff/malli-opts] :as context}]
  (let [command-name (keyword (:command/name command))
        handler (get command-registry command-name)
        command (-> (mc/decode command-name command @malli-opts strict-json-transformer)
                    (assoc :command/name command-name))
        context (assoc context
                       :api/command command
                       :time/now t/now)]
    (if handler
      (if-let [_ (mc/validate command-name command @malli-opts)]
        (let [{:keys [result tx post-tx-side-fx]} (handler context)
              tx-result (try (when tx (submit-tx context (map #(assoc % :event/timestamp (t/now)) tx)))
                             (catch Throwable t {::anom/category ::anom/conflict
                                                 ::anom/message "Transaction failed"
                                                 :error/throwable t}))
              _ (u/log ::tx-result :result tx-result)
              _ (try (when post-tx-side-fx (post-tx-side-fx))
                     (catch Throwable t {::anom/category ::anom/fault
                                         ::anom/message "Post transaction side effects failed."
                                         :error/throwable t}))]
          (if (s/anomaly? tx-result) tx-result result))
        {::anom/category ::anom/incorrect
         ::anom/message "Invalid Command: Failed Schema Validation"
         :error/explain (str (me/humanize (mc/explain command-name command @malli-opts)))})
      {::anom/category ::anom/not-found
       ::anom/message "Unknown Command"})))

;;;;;;;;;;;;;;
;;;; HTTP ;;;;
;;;;;;;;;;;;;;

(defn process-command-result-dispatch
  [result]
  (::anom/category result))

(defmulti process-command-result process-command-result-dispatch)

(defmethod process-command-result ::anom/incorrect
  [{:keys [::anom/message error/explain]}]
  {:status 400
   :body {:message message
          :explain explain}})

(defmethod process-command-result ::anom/not-found
  [{:keys [::anom/message]}]
  {:status 404
   :body {:message message}})

(defmethod process-command-result ::anom/forbidden
  [{:keys [::anom/message]}]
  {:status 409
   :body {:message message}})

(defmethod process-command-result ::anom/conflict
  [{:keys [::anom/message]}]
  {:status 403
   :body {:message message}})

(defmethod process-command-result :default
  [{:keys [::anom/message]}]
  {:status 500
   :body {:message message}})

(defmethod process-command-result nil
  [result]
  {:status 200
   :headers {"content-type" "application/edn"}
   :body (str {:result result})})

(defn handle-command [command-registry {:keys [body-params] :as req}]
  (let [command (-> (:command body-params)
                    (assoc :command/timestamp (t/now)))]
    (u/with-context {:api/command command}
      (u/trace ::process-command []
               (let [result (process-command command-registry (assoc req :api/command command))]
                 (when (s/anomaly? result)
                   (u/log ::anomaly :anomaly result))
                 (process-command-result result))))))