(ns crypto-app-v3.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [crypto-app-v3.events]
            [crypto-app-v3.subs]
            [crypto-app-v3.portfolio]
            [crypto-app-v3.views :as views]
            [crypto-app-v3.effects]))

;; Copy V2 initialization logic (small functions)

(defn log-startup []
  (js/console.log "🚀 V3 Re-frame Crypto Tracker Starting...")
  (js/console.log "🌍 Current URL:" js/window.location.href)
  (js/console.log "🌍 Domain:" js/window.location.hostname)
  (js/console.log "🌍 Protocol:" js/window.location.protocol))

(defn initialize-db []
  (rf/dispatch-sync [:initialize-db]))

(defn restore-portfolio []
  (js/console.log "📂 V3 Restoring portfolio from localStorage...")
  (rf/dispatch [:portfolio/initialize]))

(defn mount-app []
  (rdom/render [views/app-component] (.getElementById js/document "app")))

(defn setup-timeout-handler []
  (js/setTimeout
   (fn []
     (when @(rf/subscribe [:loading?])
       (rf/dispatch [:set-loading false])
       (rf/dispatch [:set-error "Timeout loading data"])))
   10000))

(defn start-data-fetching []
  (rf/dispatch [:fetch-exchange-rates])  ; Fetch exchange rates first
  (rf/dispatch [:fetch-crypto-data])
  (rf/dispatch [:start-auto-polling]))

(defn ^:export init []
  (log-startup)
  (initialize-db)
  (restore-portfolio)
  (mount-app)
  (setup-timeout-handler)
  (start-data-fetching)
  (js/console.log "🟢 V3 INIT() COMPLETED"))

;; Auto-start
(js/console.log "🟢 V3 CORE.CLJS LOADED - About to call init()")
(init)
