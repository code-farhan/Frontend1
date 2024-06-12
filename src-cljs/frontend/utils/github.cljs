(ns frontend.utils.github
  (:require [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]))

;; TODO convert CI.github
(defn auth-url [& {:keys [scope]
                   :or {scope ["user:email" "repo"]}}]
  (js/CI.github.authUrl (clj->js scope)))

;; TODO: We want the identicon to take over the if the gravatar is the default. There's no
;;       way to know if the gravatar_id is the default without passing in default=true
;;       query param, so we'd have to do that elsewhere.
(defn gravatar-url [{:keys [gravatar_id login size]
                     :or {size 200}}]
  (cond (not (string/blank? gravatar_id))
        (gstring/format "https://secure.gravatar.com/avatar/%s?s=%s" gravatar_id size)

        (not (string/blank? login))
        (gstring/format "https://identicons.github.com/%s.png" login)

        :else "https://secure.gravatar.com/avatar/00000000000000000000000000000000?s=#{size}"))
