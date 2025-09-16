(ns crypto-app.core
  (:require [reagent.dom :as rdom]
            [crypto-app.views :as views]
            [crypto-app.effects :as effects]
            [crypto-app.state :as state]))

;; Main application coordination

(defn mount-app []
  "Mounts the Reagent app to the DOM"
  (rdom/render [views/app-component] (js/document.getElementById "app")))

(defn init []
  "Initializes the application"
  ;; Restore portfolio from localStorage
  (state/restore-portfolio)
  
  ;; Start the app UI
  (mount-app)

  ;; Set up timeout handler for loading errors
  (effects/setup-timeout-handler)

  ;; Start data fetching and polling
  (effects/start-polling))

;; Auto-start when script loads
(init)
