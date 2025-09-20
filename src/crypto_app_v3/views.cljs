(ns crypto-app-v3.views
  (:require [re-frame.core :as rf]))

;; Small, focused view components

(defn loading-spinner []
  [:div {:class "inline-block w-10 h-10 border-3 border-gray-700 border-t-neon-green rounded-full animate-spin mb-5"}])

(defn loading-view []
  [:div {:class "text-center text-gray-400 text-xl py-24"}
   [loading-spinner]
   [:div "Loading market data..."]])

(defn error-view [error]
  [:div {:class "bg-neon-red/10 border border-neon-red/20 text-neon-red p-5 rounded-xl my-5 text-center"}
   "Failed to load market data: " error
   [:br]
   "Retrying in 30 seconds..."])

(defn app []
  (let [loading? @(rf/subscribe [:loading?])
        error @(rf/subscribe [:error-message])]
    [:div
     (cond
       error [error-view error]
       loading? [loading-view]
       :else [:div "V3 Data will appear here..."])]))
