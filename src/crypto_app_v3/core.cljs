(ns crypto-app-v3.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [crypto-app-v3.events]
            [crypto-app-v3.subs]
            [crypto-app-v3.views :as views]
            [crypto-app-v3.effects]))

(defn log-startup []
  (println "🚀 V3 Re-frame Crypto Tracker Starting...")
  (println "📊 Event-driven architecture loaded"))

(defn initialize-app []
  (rf/dispatch-sync [:initialize-db]))

(defn start-data-fetch []
  (rf/dispatch [:fetch-crypto-data]))

(defn mount-app []
  (rdom/render [views/app] (.getElementById js/document "app")))

(defn ^:export init []
  (log-startup)
  (initialize-app)
  (start-data-fetch)
  (mount-app))

;; Auto-start
(init)
