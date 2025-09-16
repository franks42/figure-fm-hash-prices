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

;; Portfolio state atoms
(def portfolio-atom (r/atom {}))           ; Simple holdings: crypto-id -> quantity
(def show-portfolio-panel (r/atom nil))    ; crypto-id of asset whose portfolio modal is open, nil = closed

;; Portfolio persistence functions (compositional approach)
(defn save-portfolio-to-storage [portfolio-data]
  "Save portfolio data to localStorage"
  (try
    (.setItem js/localStorage "crypto-portfolio" (.stringify js/JSON (clj->js portfolio-data)))
    true
    (catch :default e
      (js/console.warn "Failed to save portfolio to localStorage:" e)
      false)))

(defn load-portfolio-from-storage []
  "Load portfolio data from localStorage"
  (try
    (when-let [stored-data (.getItem js/localStorage "crypto-portfolio")]
      (js->clj (.parse js/JSON stored-data)))
    (catch :default e
      (js/console.warn "Failed to load portfolio from localStorage:" e)
      {})))

(defn persist-portfolio []
  "Save current portfolio state to localStorage"
  (save-portfolio-to-storage @portfolio-atom))

(defn restore-portfolio []
  "Load and restore portfolio state from localStorage"
  (let [stored-portfolio (load-portfolio-from-storage)]
    (when (seq stored-portfolio)
      (reset! portfolio-atom stored-portfolio))))

(defn clear-portfolio []
  "Clear portfolio data from both state and localStorage"
  (reset! portfolio-atom {})
  (save-portfolio-to-storage {}))

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
