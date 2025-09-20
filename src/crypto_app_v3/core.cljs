(ns crypto-app-v3.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [crypto-app-v3.events]
            [crypto-app-v3.subs]
            [crypto-app-v3.portfolio]
            [crypto-app-v3.portfolio-atoms :as portfolio-atoms]
            [crypto-app-v3.views :as views]
            [crypto-app-v3.effects]))

;; Copy V2 initialization logic (small functions)

(def ^:const VERSION "v3.3.2-volume-currency-conversion")

(defn log-startup []
  (js/console.log "🚀 V3 Re-frame Crypto Tracker Starting...")
  (js/console.log "🔢 VERSION:" VERSION "- EDN Portfolio Persistence + Version Tracking")
  ;; Store and check version in localStorage
  (let [stored-version (js/localStorage.getItem "crypto-app-v3-version")]
    (js/console.log "📦 STORED VERSION:" stored-version)
    (js/console.log "🆕 CURRENT VERSION:" VERSION)
    (js/console.log "✅ VERSION UP-TO-DATE?" (= stored-version VERSION))
    (js/localStorage.setItem "crypto-app-v3-version" VERSION))
  (js/console.log "🌍 Current URL:" js/window.location.href)
  (js/console.log "🌍 Domain:" js/window.location.hostname)
  (js/console.log "🌍 Protocol:" js/window.location.protocol))

(defn initialize-db []
  (rf/dispatch-sync [:initialize-db]))

(defn restore-portfolio []
  (js/console.log "📂 V3 Restoring portfolio from localStorage...")
  (portfolio-atoms/restore-portfolio))

(defn restore-currency []
  (js/console.log "💱 V3 Restoring currency from localStorage...")
  (when-let [currency (js/localStorage.getItem "selected-currency")]
    (rf/dispatch-sync [:currency/set currency])))

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
  (restore-currency)
  (mount-app)
  (setup-timeout-handler)
  (start-data-fetching)
  (js/console.log "🟢 V3 INIT() COMPLETED"))

;; Auto-start
(js/console.log "🟢 V3 CORE.CLJS LOADED - About to call init()")
(init)
