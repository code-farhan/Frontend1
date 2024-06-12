(ns frontend.components.dashboard
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
            [frontend.async :refer [put!]]
            [frontend.components.builds-table :as builds-table]
            [frontend.components.common :as common]
            [frontend.components.project.common :as project-common]
            [frontend.models.plan :as plan-model]
            [frontend.routes :as routes]
            [frontend.state :as state]
            [frontend.utils :as utils :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [frontend.utils :refer [html]]))

(defn show-trial-notice? [projects plan]
  (let [some-private-repos (some #(not (get-in % [:feature_flags :oss])) projects)]
    (and some-private-repos
         (plan-model/trial? plan)
         ;; We probably gave them a special deal, better not to bug them
         (> 20 (plan-model/days-left-in-trial plan)))))

(defn dashboard [data owner]
  (reify
    om/IDisplayName (display-name [_] "Dashboard")
    om/IRender
    (render [_]
      (let [builds (:recent-builds data)
            projects (get-in data state/projects-path)
            plan (get-in data state/project-plan-path)
            nav-ch (get-in data [:comms :nav])
            controls-ch (om/get-shared owner [:comms :controls])
            nav-data (:navigation-data data)
            page (js/parseInt (get-in nav-data [:query-params :page] 0))
            builds-per-page (:builds-per-page data)]
        (html
         (cond (nil? builds) [:div.loading-spinner common/spinner]
              (and (empty? builds)
                   projects
                   (empty? projects)) [:div
                                       [:h2 "You don't have any projects in CircleCI!"]
                                       [:p "Why don't you add a repository or two on the "
                                        [:a {:href (routes/v1-add-projects)} "Manage Projects page"] "?"]]
              :else
              [:div.dashboard
               (when (and plan (show-trial-notice? projects plan))
                 (om/build project-common/trial-notice plan))
               (om/build builds-table/builds-table builds {:opts {:show-actions? false
                                                                  :show-branch? (not (:branch nav-data))
                                                                  :show-project? (not (:repo nav-data))}})
               [:div.recent-builds-pager
                [:a
                 {:href (routes/v1-dashboard-path (assoc nav-data :page (max 0 (dec page))))
                  ;; no newer builds if you're on the first page
                  :class (when (zero? page) "disabled")}
                 [:i.fa.fa-long-arrow-left]
                 [:span " Newer builds"]]
                [:a
                 {:href (routes/v1-dashboard-path (assoc nav-data :page (inc page)))
                  ;; no older builds if you have less builds on the page than an
                  ;; API call returns
                  :class (when (> builds-per-page (count builds)) "disabled")}
                 [:span "Older builds "]
                 [:i.fa.fa-long-arrow-right]]]]))))))
