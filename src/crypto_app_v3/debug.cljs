(ns crypto-app-v3.debug)

;; Debug helper for sentiment calculation debugging
;; Toggle this at browser console: crypto_app_v3.debug.set_enabled_BANG_(false)
(defonce ^:private state (atom true))

(defn set-enabled! [v] (reset! state v))

(defn log
  "Unified console logger for debugging sentiment calculations"
  [& xs]
  (when @state
    (apply js/console.log "🐛" xs)))
