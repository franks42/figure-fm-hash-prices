(ns crypto-app-v2.state
  (:require [reagent.core :as r]))

;; Fine-grained state atoms for selective updates
(def prices-atom (r/atom []))                     ; Changed: array instead of object
(def price-keys-atom (r/atom []))
(def last-update-atom (r/atom nil))
(def loading-atom (r/atom true))
(def error-atom (r/atom nil))
(def initial-load-complete (r/atom false))
(def update-flash-atom (r/atom false))

;; Portfolio state atoms
(def portfolio-atom (r/atom {}))           ; Simple holdings: crypto-id -> quantity
(def show-portfolio-panel (r/atom nil))    ; crypto-id of asset whose portfolio modal is open, nil = closed

;; Currency state atoms
(def currency-atom (r/atom "USD"))         ; Currently selected currency
(def show-currency-panel (r/atom false))   ; Currency selector modal open/closed
(def exchange-rates-atom (r/atom {}))      ; Exchange rates: {"EUR" 0.85, "GBP" 0.73, ...}
(def using-mock-rates-atom (r/atom true))  ; Track if using mock exchange rates

;; Portfolio persistence functions (compositional approach)
(defn save-portfolio-to-storage [portfolio-data]
  (try
    (js/console.log "💾 Saving portfolio to localStorage:" portfolio-data)
    (.setItem js/localStorage "crypto-portfolio-v2" (.stringify js/JSON (clj->js portfolio-data)))
    (js/console.log "✅ Portfolio saved successfully")
    true
    (catch :default e
      (js/console.warn "❌ Failed to save portfolio to localStorage:" e)
      false)))

(defn load-portfolio-from-storage []
  (try
    (js/console.log "📖 Loading portfolio from localStorage...")
    (let [stored-data (.getItem js/localStorage "crypto-portfolio-v2")]
      (js/console.log "📖 Raw stored data:" stored-data)
      (when stored-data
        (let [parsed-data (js->clj (.parse js/JSON stored-data))]
          (js/console.log "✅ Portfolio loaded successfully:" parsed-data)
          parsed-data)))
    (catch :default e
      (js/console.warn "❌ Failed to load portfolio from localStorage:" e)
      {})))

(defn persist-portfolio []
  (save-portfolio-to-storage @portfolio-atom))

(defn restore-portfolio []
  (let [stored-portfolio (load-portfolio-from-storage)]
    (when (seq stored-portfolio)
      (reset! portfolio-atom stored-portfolio))))

(defn clear-portfolio []
  (reset! portfolio-atom {})
  (save-portfolio-to-storage {}))

;; Currency persistence functions
(defn save-currency-to-storage [currency]
  (try
    (js/console.log "💱 Saving currency to localStorage:" currency)
    (.setItem js/localStorage "crypto-currency-v2" currency)
    (js/console.log "✅ Currency saved successfully")
    true
    (catch :default e
      (js/console.warn "❌ Failed to save currency to localStorage:" e)
      false)))

(defn load-currency-from-storage []
  (try
    (js/console.log "💱 Loading currency from localStorage...")
    (let [stored-currency (.getItem js/localStorage "crypto-currency-v2")]
      (js/console.log "💱 Raw stored currency:" stored-currency)
      (if stored-currency
        (do
          (js/console.log "✅ Currency loaded successfully:" stored-currency)
          stored-currency)
        "USD"))
    (catch :default e
      (js/console.warn "❌ Failed to load currency from localStorage:" e)
      "USD")))

(defn persist-currency []
  (save-currency-to-storage @currency-atom))

(defn restore-currency []
  (let [stored-currency (load-currency-from-storage)]
    (reset! currency-atom stored-currency)))

;; Currency conversion functions
(defn convert-currency [usd-amount target-currency]
  "Convert USD amount to target currency using current exchange rates"
  (if (= target-currency "USD")
    usd-amount
    (let [currency-key (keyword target-currency)
          rate (get @exchange-rates-atom currency-key)]
      (if rate
        (* usd-amount rate)
        usd-amount))))  ; Fallback to USD if rate not available

(defn get-currency-symbol [currency-code]
  "Get currency symbol for display"
  (case currency-code
    "USD" "$"
    "EUR" "€"
    "GBP" "£"
    "JPY" "¥"
    "CAD" "C$"
    "AUD" "A$"
    "CHF" "CHF"
    "CNY" "¥"
    "KRW" "₩"
    "SEK" "kr"
    currency-code))

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

;; Supported currencies with symbols and names
(def supported-currencies
  [{:code "USD" :symbol "$" :name "US Dollar"}
   {:code "EUR" :symbol "€" :name "Euro"}
   {:code "GBP" :symbol "£" :name "British Pound"}
   {:code "JPY" :symbol "¥" :name "Japanese Yen"}
   {:code "CAD" :symbol "C$" :name "Canadian Dollar"}
   {:code "AUD" :symbol "A$" :name "Australian Dollar"}
   {:code "CHF" :symbol "CHF" :name "Swiss Franc"}
   {:code "CNY" :symbol "¥" :name "Chinese Yuan"}
   {:code "KRW" :symbol "₩" :name "Korean Won"}
   {:code "SEK" :symbol "kr" :name "Swedish Krona"}])

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