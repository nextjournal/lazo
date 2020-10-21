(ns lazo.events
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [lazo.git :as git]))


(defn extract-branch [git-ref]
  (str/replace git-ref #"refs/heads/" ""))

(defmulti handle-event!
          (fn [{:keys [event]}]
            (:event/type event)))

(defmethod handle-event! :default [{:keys [event]}]
  (log/info :unknown-event (:event/type event)))

(defn commit-with-module? [repo-config {:keys [added removed modified] :as commit}]
  (some (fn [path]
          (str/starts-with? path (:main-module-folder repo-config)))
        (concat added removed modified)))

(defmethod handle-event! :push
  [{:keys [event config]}]
  (let [repo (get-in event [:repository :name])
        git-ref (get-in event [:ref])]
    (when-let [repo-configs (->> (:repos config)
                                 (filter (fn [r] (and (= (:main-repo r) repo)
                                                      (= (:main-branch r) (extract-branch git-ref))))))]
      (doseq [repo-config (drop 1 repo-configs)]
        (log/info (format "Detected push at '%s'. Syncing '%s'." (:main-repo repo-config) (:module-repo repo-config)))
        (let [commits (->> (:commits event)
                           (filter (partial commit-with-module? repo-config))
                           (map (fn [c] {:message (:message c)
                                         :author  (get-in c [:author :name])
                                         :email   (get-in c [:author :email])
                                         :sha     (:id c)})))]
          (when (seq commits)
            (log/info :commits commits)
            (git/sync-module! commits
                              repo-config
                              config)))))))