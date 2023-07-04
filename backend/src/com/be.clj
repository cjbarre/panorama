(ns com.be
  (:require [com.biffweb :as biff]
            [com.be.feat.app :as app]
            [com.be.schema :as schema]
            [com.be.plugins.mulog :as mulog]
            [clojure.test :as test]
            [clojure.tools.logging :as log]
            [malli.core :as malc]
            [malli.registry :as malr]
            [nrepl.cmdline :as nrepl-cmd]
            [com.biffweb.impl.middleware :as bmd]
            [muuntaja.middleware :as muuntaja]
            [ring.middleware.defaults :as rd]
            [ring.middleware.cors :as cors]))

(def features
  [app/features
   schema/features])

(def routes [["" {:middleware [biff/wrap-site-defaults]}
              (keep :routes features)]
             ["" {:middleware [biff/wrap-api-defaults]}
              (keep :api-routes features)]])

(defn wrap-print
  [handler]
  (fn [context] (clojure.pprint/pprint context) (handler context)))

(def handler (-> (biff/reitit-handler {:routes routes})
                 biff/wrap-base-defaults))

(defn on-save [sys]
  (biff/add-libs)
  (biff/eval-files! sys)
  (test/run-all-tests #"com.be.test.*"))

(def malli-opts
  {:registry (malr/composite-registry
              malc/default-registry
              (apply biff/safe-merge
                     (keep :schema features)))})

(def components
  [biff/use-config
   biff/use-secrets
   biff/use-xt
   biff/use-queues
   biff/use-tx-listener
   biff/use-wrap-ctx
   biff/use-jetty
   biff/use-chime
   (biff/use-when
    :com.be/enable-beholder
    biff/use-beholder)
   mulog/use-mulog])

(defn start []
  (let [ctx (biff/start-system
             {:com.be/chat-clients (atom #{})
              :biff/features #'features
              :biff/after-refresh `start
              :biff/handler #'handler
              :biff/malli-opts #'malli-opts
              :biff.beholder/on-save #'on-save
              :biff.xtdb/tx-fns biff/tx-fns
              :biff/components components})]
    (log/info "Go to" (:biff/base-url ctx))))

(defn -main [& args]
  (start)
  (apply nrepl-cmd/-main args))
