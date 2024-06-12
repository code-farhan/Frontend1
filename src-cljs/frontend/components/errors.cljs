(ns frontend.components.errors
  (:require [frontend.utils :as utils :include-macros true]
            [frontend.utils.github :as gh-utils]
            [om.core :as om :include-macros true])
  (:require-macros [frontend.utils :refer [html]]))

(defn error-page [app owner]
  (reify
    om/IRender
    (render [_]
      (let [status (get-in app [:navigation-data :status])]
        (html
         [:div.page.error
          [:div.banner
           [:div.container
            [:h1 status]
            [:h3 (str (condp = status
                        401 "Login required"
                        404 "Page not found"
                        500 "Internal server error"
                        "Something unexpected happened"))]]]
          [:div.container
           (condp = status
             401 [:p
                  [:b [:a {:href (gh-utils/auth-url)}
                       "Login here"]]
                  " to view this page"]
             404 [:p "We're sorry, but that page doesn't exist"]
             500 [:p "We're sorry, but something broke"]
             "Something completely unexpected happened")]])))))
