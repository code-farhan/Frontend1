(ns frontend.components.changelog
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
            [clojure.string :as str]
            [frontend.async :refer [put!]]
            [frontend.components.about :as about]
            [frontend.components.common :as common]
            [frontend.components.shared :as shared]
            [frontend.datetime :as datetime]
            [frontend.state :as state]
            [frontend.stefon :as stefon]
            [frontend.utils :as utils :include-macros true]
            [om.core :as om :include-macros true])
  (:require-macros [frontend.utils :refer [defrender html]]))

(defn changelog [app owner]
  (reify
    om/IRender
    (render [_]
      (let [changelog-entries (get-in app state/changelog-path)
            team (about/team)]
        (html
         [:div.changelog.page
          [:div.banner
           [:div.container
            [:h1 "Changelog"]
            [:h3 "What's changed in CircleCI recently"]]]
          [:div.container.content
           [:div.entries
            (for [entry changelog-entries
                  :let [team-member (first (filter #(= (:author entry) (:github %)) team))]]
              [:div.entry {:id (-> (:pubDate entry) (str/replace ":" "") (str/replace "-" ""))}
               [:div.entry-main
                [:div.entry-content
                 [:h3.title (:title entry)]
                 [:p.description {:dangerouslySetInnerHTML #js {"__html" (:description entry)}}]]
                [:div.entry-info {:class (:type entry)}
                 [:strong (:type entry)]
                 (when team-member
                   (list " by "
                         [:a {:href (if (:twitter team-member)
                                      (str "https://twitter.com/" (:twitter team-member))
                                      (str "https://github.com/" (:github team-member)))}
                          (:name team-member)]
                         " on"))
                 [:a {:href (:link entry)}
                  " " (datetime/as-time-since (:pubDate entry))]]]
               [:div.entry-avatar
                (when team-member
                  [:img {:src (:img-path team-member)}])]])]]])))))
