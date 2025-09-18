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
  ;; Debug: Log initialization
  (js/console.log "🚀 App initializing...")
  (js/console.log "🌍 Current URL:" js/window.location.href)
  (js/console.log "🌍 Domain:" js/window.location.hostname)
  (js/console.log "🌍 Protocol:" js/window.location.protocol)

  ;; Restore portfolio from localStorage
  (js/console.log "📂 Restoring portfolio from localStorage...")
  (state/restore-portfolio)
  (js/console.log "📂 Portfolio state after restore:" @state/portfolio-atom)

  ;; Start the app UI
  (mount-app)

  ;; Set up timeout handler for loading errors
  (effects/setup-timeout-handler)

  ;; Start data fetching and polling
  (effects/start-polling))

;; Auto-start when script loads
(js/console.log "🟢 CORE.CLJS LOADED - About to call init()")
(init)
(js/console.log "🟢 INIT() COMPLETED")
