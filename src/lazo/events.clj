(ns lazo.events
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [lazo.git :as git]))


(defn extract-branch [git-ref]
  (str/replace git-ref #"refs/head/" ""))

(defmulti handle-event!
          (fn [{:keys [event]}]
            (:event/type event)))

(defmethod handle-event! :default [{:keys [event]}]
  (log/info :unknown-event (:event/type event)))

(defmethod handle-event! :push
  [{:keys [event config]}]
  (let [repo (get-in event [:repository :name])
        git-ref (get-in event [:ref])]
    (when-let [repo-config (->> (:repos config)
                                (filter (fn [r] (and (= (:main-repo r) repo)
                                                     (= (:main-branch r) (extract-branch ref)))))
                                (first))]
      (log/info "Detected push at main repo, main branch. Syncing module repo.")
      (git/sync-module {:message "foo"
                        :author  "Holger"
                        :email   "holger@nextjournal.com"
                        :sha     "1b6813a9bb9de1005f63104a2c1fd89bfea7f982"}
                       repo-config
                       config))

    )

  #_(when (and (= (env :server-repo))
               (= "refs/heads/master"))                     ;; We're only interested in push events to master in server repo
      (log/info "Received a push on master branch on server repo, syncing client-repo")
      (let [head-commit-sha (get-in event [:head_commit :id])
            original-author (get-in @app-state [:authors head-commit-sha])
            _ (when original-author (remove-author! head-commit-sha))
            commit-author (get-in event [:head_commit :author])
            author-name (or (:name original-author) (:name commit-author))
            author-email (or (:email original-author) (:email commit-author))
            message (get-in event [:head_commit :message])
            sanitized-message (str/trim (str/replace message #"\(#\d+\)" ""))]
        (log/info (format "Merging commit '%s' <%s> from %s <%s>" sanitized-message head-commit-sha author-name author-email))
        (log/info (sh/sh "sh" "-c" (format "./sync-client.sh %s %s %s '%s' '%s' %s"
                                           (env :server-repo)
                                           (env :client-repo)
                                           (env :client-folder)
                                           sanitized-message
                                           author-name
                                           author-email))))))

