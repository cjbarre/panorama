(ns com.be.tasks
  (:require
   [babashka.fs :as fs]
   [babashka.tasks :refer [shell]]
   [com.biffweb.tasks :refer [shell-some css windows? config]]))

(defn server [target-server & args]
  (apply shell "ssh" (str "root@" target-server) args))

(defn logs
  "Tails the server's application logs."
  [& [env n-lines]]
  (let [{:biff.tasks/keys [server-staging server-prod]} @config
        target-server (case env "staging" server-staging "prod" server-prod)]
    (server target-server "journalctl" "-u" "app" "-f" "-n" (or n-lines "300"))))

(defn deploy
  "Deploys the app via `git push`.

  Copies config.edn to the server, deploys code via `git push`, and
  restarts the app process on the server (via git push hook). You must set up a
  server first. See https://biffweb.com/docs/reference/production/."
  [& [env]]
  (let [{:biff.tasks/keys [server-staging server-prod deploy-to deploy-from deploy-cmd-staging deploy-cmd-prod]} @config
        server (case env "staging" server-staging "prod" server-prod)
        deploy-cmd (case env "staging" deploy-cmd-staging "prod" deploy-cmd-prod)] 
    (if (windows?)
      (shell-some "scp"
                  "config.edn"
                  (when (fs/exists? "secrets.env") "secrets.env")
                  (str "app@" server ":backend/"))
      (do
        (fs/set-posix-file-permissions "config.edn" "rw-------")
        (when (fs/exists? "secrets.env")
          (fs/set-posix-file-permissions "secrets.env" "rw-------"))
        (shell-some "rsync" "-a" "--relative"
                    "config.edn"
                    (when (fs/exists? "secrets.env") "secrets.env") 
                    (str "app@" server ":backend/"))))
    (time (if deploy-cmd
            (apply shell deploy-cmd)
            ;; For backwards compatibility
            (shell "git" "push" deploy-to deploy-from)))))

(defn hello
  "An example of a custom task. See ./tasks/"
  []
  (println "Hello there."))
