(ns lazo.git
  (:require [clj-jgit.porcelain :as jgit]
            [me.raynes.fs :as fs]))

(defn initialize-repos [config]
  (doseq [{:keys [main-repo organization module-repo]} (:repos config)]
    (jgit/with-credentials {:login (:user config) :pw (:token config)}
      (jgit/git-clone (format "https://github.com/%s/%s.git" organization module-repo) :dir (str (:local-dir config) "/" module-repo))
      (jgit/git-clone (format "https://github.com/%s/%s.git" organization main-repo) :dir (str (:local-dir config) "/" main-repo)))))

(defn sync-module [{:keys [message author email sha] :as commit}
                   {:keys [main-repo main-branch main-module-folder module-repo module-branch] :as repo-config}
                   {:keys [user token local-dir] :as config}]
  (let [main-folder (str local-dir "/" main-repo)
        module-folder (str local-dir "/" module-repo)
        main (jgit/load-repo main-folder)
        module (jgit/load-repo module-folder)]
    (jgit/with-credentials {:login user :pw token}
      ;; resets local copy of server repo at sha
      (jgit/git-fetch main)
      (jgit/git-checkout main :name main-branch)
      (jgit/git-clean main :dirs? true :ignore? false)
      (jgit/git-reset main :mode :hard :ref sha)

      ;; resets local copy of client repo to latest master
      (jgit/git-fetch module)
      (jgit/git-checkout module :name module-branch)
      (jgit/git-clean module :dirs? true :ignore? false)
      (jgit/git-reset module :mode :hard :ref module-branch)

      ;; delete everything except .git folder in module repo
      (doseq [file (->> module-folder
                        (fs/list-dir)
                        (remove #(= ".git" (fs/name %))))]
        (fs/delete-dir file))

      ;; copy all contents of main-module-folder from main-repo into module-repo
      (fs/copy-dir-into (str main-folder "/" main-module-folder)
                        module-folder)

      ;; when there are changes to commit, commit and push them
      (when (some seq (vals (jgit/git-status module)))
        (jgit/git-add module ".")
        (jgit/git-commit module
                         message
                         :all? true
                         :author {:name author :email email}))

      (jgit/git-push module)))


  (comment

    (initialize-repos lazo.core/config)
    (sync-module {:message "foo"
                  :author  "Holger Amann"
                  :email   "holger@nextjournal.com"
                  :sha     "4fd1bff"}
                 {:organization       "test-org-integration"
                  :main-repo          "main-repo"
                  :main-branch        "master"
                  :main-module-folder "my-module"
                  :module-repo        "module-repo"
                  :module-branch      "master"}
                 lazo.core/config)
    ))