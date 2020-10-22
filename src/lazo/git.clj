(ns lazo.git
  (:require [clj-jgit.porcelain :as jgit]
            [me.raynes.fs :as fs]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn initialize-repo! [organization repo branch config]
  (let [dir (str (:local-dir config) "/" repo)]
    (when-not (fs/exists? dir)
      (log/info (format "Repo not found, initializing '%s'" repo))
      (jgit/git-clone (format "https://github.com/%s/%s.git" organization repo) :branch branch :dir dir))
    (-> (jgit/load-repo dir)
        (jgit/git-config-load)
        (jgit/git-config-set "user.name" (:user config))
        (jgit/git-config-set "user.email" (:email config))
        (jgit/git-config-save))))

(defn initialize-repos! [config]
  (log/info "Initializing repos")
  (doseq [{:keys [main-repo organization main-branch module-branch module-repo]} (:repos config)]
    (jgit/with-credentials {:login (:user config) :pw (:token config)}
      (initialize-repo! organization module-repo module-branch config)
      (initialize-repo! organization main-repo main-branch config))))


(defn generate-final-commit-message [commits co-authors]
  (let [merge? (str/starts-with? (:message (last commits)) "Merge pull request")
        description (->> (map :message commits)
                         (remove #(str/starts-with? % "Merge pull request"))
                         (map #(str "* " %))
                         (str/join "\n"))
        title (if merge?
                (last (str/split-lines (:message (last commits))))
                "Update module")
        co-authors-text (->> co-authors
                             (map (fn [co-author]
                                    (format "Co-authored-by: %s <%s>" (:name co-author) (:email co-author))))
                             (str/join "\n"))]
    (cond-> (str title "\n\n")
            (>= (count commits) 1) (str description)
            (seq co-authors) (str "\n\n" co-authors-text))))


(defn compute-authors [commits]
  (let [main-author {:name (:author (last commits))
                     :email (:email (last commits))}
        co-authors (->> commits
                     (map (fn [c] {:name  (:author c)
                                   :email (:email c)}))
                     (filter (fn [{:keys [email]}]
                               (not= email (:email main-author)))))]
    {:main-author main-author
     :co-authors co-authors}))

(defn sync-module! [commits
                   {:keys [main-repo main-branch main-module-folder module-repo module-branch] :as _repo-config}
                   {:keys [user token local-dir] :as _config}]
  (let [main-folder (str local-dir "/" main-repo)
        module-folder (str local-dir "/" module-repo)
        main (jgit/load-repo main-folder)
        module (jgit/load-repo module-folder)
        authors (compute-authors commits)
        final-commit-message (generate-final-commit-message commits (:co-authors authors))
        {:keys [sha]} (last commits)]
    (jgit/with-credentials {:login user :pw token}
      ;; resets local copy of client repo to latest master
      (jgit/git-fetch module)
      (jgit/git-checkout module :name module-branch)
      (jgit/git-clean module :dirs? true :ignore? false)
      (jgit/git-reset module :mode :hard :ref module-branch)

      ;; resets local copy of server repo at sha
      (jgit/git-fetch main)
      (jgit/git-checkout main :name main-branch)
      (jgit/git-clean main :dirs? true :ignore? false)
      (jgit/git-reset main :mode :hard :ref sha)

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
        (log/info :committing final-commit-message)
        (jgit/git-commit module
                         final-commit-message
                         :all? true
                         :author {:name (get-in authors [:main-author :name])
                                  :email (get-in authors [:main-author :email])}))
      (log/info :pushing-changes)
      (jgit/git-push module)))


  (comment

    (initialize-repos! lazo.core/config)
    (sync-module! [{:message asdfasdf, :author "Holger Amann", :email "holger@nextjournal.com", :sha "97cbd7955c414f96cbb398ca6259248153dd4d90"}]
                  {:organization       "test-org-integration"
                  :main-repo          "main-repo"
                  :main-branch        "master"
                  :main-module-folder "my-module"
                  :module-repo        "module-repo"
                  :module-branch      "master"}
                  lazo.core/config))

  (generate-final-commit-message [{:message "Foo"}
                                  {:message "bar"}
                                  {:message "Merge pull request #2 from test-org-integration/pr2\n\nAdd what.txt"}]
                                 [{:name "Foo" :email "foo@gmail.com"}
                                  {:name "Bar" :email "bar@gmail.com"}])
  )

