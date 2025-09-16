(ns crypto-app.state
  (:require [reagent.core :as r]))

;; Fine-grained state atoms for selective updates
(def prices-atom (r/atom {}))
(def price-keys-atom (r/atom []))
(def last-update-atom (r/atom nil))
(def loading-atom (r/atom true))
(def error-atom (r/atom nil))
(def initial-load-complete (r/atom false))
(def update-flash-atom (r/atom false))

;; Configuration constants
(def ^:const POLL_INTERVAL_MS 30000)
(def ^:const FLASH_DURATION_MS 800)
(def ^:const SCAN_HIDE_DELAY_MS 2100)
(def ^:const TIMEOUT_MS 10000)

;; Crypto icons and symbols mapping (updated for Figure Markets data)
(def crypto-icons
  {:btc "₿"
   :eth "Ξ"
   :link "⬡"
   :sol "◎"
   :uni "🦄"
   :xrp "💰"
   :hash "🔗"
   :figr_heloc "🏠"
   :figr "📈"})

(def crypto-symbols
  {:btc "BTC"
   :eth "ETH"
   :link "LINK"
   :sol "SOL"
   :uni "UNI"
   :xrp "XRP"
   :hash "HASH"
   :figr_heloc "FIGR_HELOC"
   :figr "FIGR"})
