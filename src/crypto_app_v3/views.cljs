(ns crypto-app-v3.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [crypto-app-v3.portfolio-atoms :as portfolio-atoms]
            [crypto-app-v3.chart :as chart]))

;; Import version from core
(def VERSION "v4.2.0-ui-improvements")

;; Old background-chart removed - using chart.cljs instead to avoid conflicts

;; Stale data detection and warning functions
(defn is-stale-data?
  "Check if this data is stale/fallback data"
  [data]
  (= (get data "data_status") "STALE_FALLBACK"))

(defn stale-data-warning
  "Show warning banner for stale data"
  [_crypto-id data]
  (when (is-stale-data? data)
    (let [error-reason (get data "error_reason" "unknown")]
      [:div {:class "bg-neon-red/15 border border-neon-red/30 rounded-lg p-3 mb-4"}
       [:div {:class "flex items-center justify-between"}
        [:div {:class "flex items-center"}
         [:span {:class "text-neon-red text-xs font-bold mr-2"} "⚠️ DATA FEED ISSUE"]
         [:span {:class "text-neon-red/90 text-xs"}
          (case error-reason
            "invalid_price" "API returned invalid price"
            "missing_data" "API data unavailable"
            "Data feed issue")]]
        [:div {:class "text-neon-red/70 text-xs font-medium"} "USING FALLBACK"]]])))

(defn stale-data-card-styling
  "Add visual styling for stale data cards"
  [data base-classes]
  (if (is-stale-data? data)
    (str base-classes " border-neon-red/40 bg-neon-red/5")
    base-classes))

;; Copy V2 constants exactly
(def crypto-icons
  {:btc "₿" :eth "Ξ" :link "⬡" :sol "◎" :uni "🦄"
   :xrp "💰" :hash "🔗" :figr_heloc "🏠" :figr "📈"})

(def crypto-symbols
  {:btc "BTC" :eth "ETH" :link "LINK" :sol "SOL" :uni "UNI"
   :xrp "XRP" :hash "HASH" :figr_heloc "FIGR_HELOC" :figr "FIGR"})

;; Copy V2 formatting functions exactly (small, pure)
(defn format-number [n decimals]
  (.toLocaleString n "en-US"
                   #js{:minimumFractionDigits decimals
                       :maximumFractionDigits decimals}))

(defn convert-currency
  "Convert USD amount to target currency using current exchange rates"
  [usd-amount target-currency exchange-rates]
  (if (= target-currency "USD")
    usd-amount
    (let [currency-key (keyword target-currency)
          rate (get exchange-rates currency-key)]
      (if rate
        (* usd-amount rate)
        usd-amount))))  ; Fallback to USD if rate not available

(defn get-currency-symbol
  "Get currency symbol for display"
  [currency-code]
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

(defn format-price
  ([price crypto-id]
   (let [decimals (if (= crypto-id "hash") 3 2)]
     (str "$" (format-number price decimals))))
  ([price crypto-id currency exchange-rates]
   (let [decimals (if (= crypto-id "hash") 3 2)
         converted-price (convert-currency price currency exchange-rates)
         symbol (get-currency-symbol currency)]
     (str symbol (format-number converted-price decimals)))))

(defn format-volume
  ([vol]
   (cond
     (>= vol 1e9) (str "$" (format-number (/ vol 1e9) 2) "B")
     (>= vol 1e6) (str "$" (format-number (/ vol 1e6) 2) "M")
     :else (str "$" (format-number vol 0))))
  ([vol current-currency exchange-rates]
   (let [converted-vol (convert-currency vol current-currency exchange-rates)
         symbol (get-currency-symbol current-currency)]
     (cond
       (>= converted-vol 1e9) (str symbol (format-number (/ converted-vol 1e9) 2) "B")
       (>= converted-vol 1e6) (str symbol (format-number (/ converted-vol 1e6) 2) "M")
       :else (str symbol (format-number converted-vol 0))))))

;; Currency button component (copy V2 exactly) - MOVED UP
(defn currency-button [currency-code]
  [:button {:class "ml-1 text-xs bg-white/10 hover:bg-white/20 border border-white/20 rounded px-2 py-0.5 transition-colors"
            :on-click #(do (.stopPropagation %) (rf/dispatch [:currency/show-selector]))}
   currency-code])

;; Data source mapping and chip component  
(defn get-feed-indicator
  "Get short feed indicator based on asset type and data source"
  [asset-type data-source]
  (cond
    (= data-source "MOCK-DATA") "MOCK"
    (= asset-type "stock") "YF"      ; Yahoo Finance for stocks (FIGR)
    :else "FM"))                     ; Figure Markets for crypto

(defn data-source-chip [asset-type data-source]
  (let [indicator (get-feed-indicator asset-type data-source)
        is-mock? (= indicator "MOCK")]
    [:span {:class (str "ml-3 text-xs font-semibold px-2 py-0.5 rounded border "
                        (if is-mock?
                          "border-red-400 text-red-400 bg-red-500/10"
                          "border-neon-cyan/40 text-neon-cyan bg-neon-cyan/10"))}
     indicator]))

;; Consolidated portfolio component (merged value display + button)
(defn portfolio-section
  "Consolidated portfolio component showing value and portfolio access button"
  [holding-value crypto-id current-currency exchange-rates]
  (if holding-value
;; Show value + edit button when there's a holding
    [:div {:class "bg-blue-500/10 border border-blue-500/20 rounded-lg p-3 mt-4"}
     [:div {:class "flex items-center justify-between mb-2"}
      [:div {:class "text-xs text-blue-400 uppercase tracking-widest"} "Portfolio Value"]
      [:button {:class "text-xs bg-white/[0.05] hover:bg-white/[0.10] border border-white/20 rounded px-2 py-1 font-semibold transition-colors"
                :on-click #(reset! portfolio-atoms/show-portfolio-panel crypto-id)}
       "✏️"]]
     [:div {:class "text-lg font-bold text-blue-300 tabular-nums flex items-center"}
      (format-price holding-value crypto-id current-currency exchange-rates)
      [currency-button current-currency]]]
    ;; Show add button when no holding
    [:div {:class "mt-4"}
     [:button {:class "w-full bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/20 hover:border-blue-500/40 rounded-lg px-4 py-3 text-sm font-semibold text-blue-300 transition-colors flex items-center justify-center"
               :on-click #(reset! portfolio-atoms/show-portfolio-panel crypto-id)}
      "📊 Add to Portfolio"]]))

;; Copy V2 supported currencies exactly
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

(defn portfolio-summary-header []
  (let [portfolio @portfolio-atoms/portfolio-atom
        prices @(rf/subscribe [:prices])
        ;; Calculate total value manually like V2
        total-value (reduce-kv (fn [total crypto-id quantity]
                                 (let [current-price (get-in prices [crypto-id "usd"])]
                                   (if (and current-price (> current-price 0))
                                     (+ total (* quantity current-price))
                                     total)))
                               0
                               portfolio)
        current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        converted-value (convert-currency total-value current-currency exchange-rates)
        symbol (get-currency-symbol current-currency)]
    (when (> total-value 0)
      [:div {:class "bg-gradient-to-r from-blue-500/10 to-purple-500/10 border border-blue-500/20 rounded-2xl p-6 mb-8"}
       [:div {:class "text-center"}
        [:div {:class "text-sm text-blue-400 uppercase mb-2 tracking-widest"} "Total Portfolio Value"]
        [:div {:class "text-4xl font-bold text-white tabular-nums flex items-center justify-center"}
         (str symbol (format-number converted-value 2))
         [currency-button current-currency]]]])))

;; Small helper functions
(defn get-crypto-name [crypto-id]
  (str/upper-case crypto-id))

(defn get-crypto-symbol [crypto-id]
  (get crypto-symbols (keyword crypto-id) (str/upper-case crypto-id)))

(defn get-crypto-icon [crypto-id]
  (get crypto-icons (keyword crypto-id) "◆"))

(defn price-positive? [change]
  (>= (or change 0) 0))

(defn price-arrow [positive?]
  (if positive? "▲" "▼"))

(defn change-classes [positive?]
  (if positive?
    "bg-neon-green/10 text-neon-green border-neon-green/20"
    "bg-neon-red/10 text-neon-red border-neon-red/20"))

;; UI state components
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

;; Crypto card sub-components (copy V2, break into small functions)
(defn crypto-card-icon [icon]
  [:div {:class "w-11 h-11 rounded-xl mr-4 flex items-center justify-center text-2xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-white/10"}
   icon])

(defn crypto-card-title [crypto-id is-stock? company-name exchange]
  (let [name (get-crypto-name crypto-id)
        symbol (get-crypto-symbol crypto-id)]
    [:div
     [:div {:class "text-lg font-semibold text-white tracking-wide"}
      (if is-stock? company-name name)
      [:span {:class "text-sm text-gray-500 ml-2 uppercase"} symbol]]
     (when (and is-stock? exchange)
       [:div {:class "text-xs text-gray-500 mt-0.5"} exchange])]))

(defn crypto-card-header [crypto-id is-stock? company-name exchange]
  (let [icon (get-crypto-icon crypto-id)]
    [:div {:class "flex items-center justify-between mb-5"}
     [:div {:class "flex items-center"}
      [crypto-card-icon icon]
      [crypto-card-title crypto-id is-stock? company-name exchange]]]))

(defn crypto-card-price [price crypto-id current-currency exchange-rates data-sources data]
  (let [is-stale? (is-stale-data? data)]
    [:div {:class "text-4xl font-bold mb-4 tabular-nums tracking-tight flex items-center"}
     [:span {:class (if is-stale? "text-neon-red" "text-white")}
      (format-price price crypto-id current-currency exchange-rates)]
     (when is-stale?
       [:span {:class "ml-2 text-xs bg-neon-red/20 text-neon-red px-2 py-1 rounded border border-neon-red/40"} "FALLBACK"])
     [currency-button current-currency]
     ;; Show first data source as chip
     (when (and data-sources (> (count data-sources) 0))
       [:span {:class "ml-1 text-xs font-semibold px-2 py-0.5 rounded border border-neon-cyan/40 text-neon-cyan bg-neon-cyan/10"}
        (get-feed-indicator (get data "type") (first data-sources))])]))

(defn crypto-card-change [change]
  (let [positive? (price-positive? change)
        arrow (price-arrow positive?)
        classes (change-classes positive?)]
    [:div {:class (str "inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold mb-5 border " classes)}
     [:span {:class "mr-1.5 text-base"} arrow]
     (str (format-number (js/Math.abs (or change 0)) 2) "%")]))

;; Stock vs Crypto stats (copy V2 differentiation)
(defn stock-stats [open-price trades-24h crypto-id]
  [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
   [:div {:class "flex flex-col"}
    [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Open"]
    [:div {:class "text-base font-semibold text-white tabular-nums"}
     (if open-price (format-price open-price crypto-id) "N/A")]]
   [:div {:class "flex flex-col"}
    [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Trades"]
    [:div {:class "text-base font-semibold text-white tabular-nums"}
     (if trades-24h (format-number trades-24h 0) "N/A")]]])

(defn crypto-stats [volume trades-24h crypto-id current-price current-currency exchange-rates]
  (js/console.log "🔍 CRYPTO STATS called with volume:" volume "price:" current-price "crypto:" crypto-id)
  [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
   [:div {:class "flex flex-col"}
    [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Volume"]
    [:div {:class "text-base font-semibold text-white tabular-nums flex items-center"}
     (format-volume (or volume 0) current-currency exchange-rates)
     [currency-button current-currency]]
    ;; Add token volume calculation
    (when (and volume current-price (> current-price 0))
      (let [token-volume (/ volume current-price)
            symbol (get-crypto-symbol crypto-id)]
        [:div {:class "text-xs text-gray-400 mt-1"}
         (str (format-number token-volume 0) " " symbol)]))]
   [:div {:class "flex flex-col"}
    [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Trades"]
    [:div {:class "text-base font-semibold text-white tabular-nums"}
     (if trades-24h (format-number trades-24h 0) "N/A")]]])

(defn crypto-card-high-low [high low crypto-id current-currency exchange-rates]
  (when (or high low)
    [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
     [:div {:class "flex flex-col"}
      [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h High"]
      [:div {:class "text-sm font-semibold text-green-400 tabular-nums flex items-center"}
       (if high
         (format-price high crypto-id current-currency exchange-rates)
         "N/A")
       [currency-button current-currency]]]
     [:div {:class "flex flex-col"}
      [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Low"]
      [:div {:class "text-sm font-semibold text-red-400 tabular-nums flex items-center"}
       (if low
         (format-price low crypto-id current-currency exchange-rates)
         "N/A")
       [currency-button current-currency]]]]))

;; Enhanced stock data display (copy V2 52-week range)
(defn stock-52w-range [fifty-two-week-high fifty-two-week-low crypto-id current-currency exchange-rates]
  (when (and fifty-two-week-high fifty-two-week-low)
    [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
     [:div {:class "flex flex-col"}
      [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "52W High"]
      [:div {:class "text-sm font-semibold text-green-400 tabular-nums flex items-center"}
       (format-price fifty-two-week-high crypto-id current-currency exchange-rates)
       [currency-button current-currency]]]
     [:div {:class "flex flex-col"}
      [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "52W Low"]
      [:div {:class "text-sm font-semibold text-red-400 tabular-nums flex items-center"}
       (format-price fifty-two-week-low crypto-id current-currency exchange-rates)
       [currency-button current-currency]]]]))

;; Old portfolio-button function removed - consolidated into portfolio-section above

;; Main crypto card (copy V2 exactly but use subscriptions)
(defn crypto-card [crypto-id]
  (let [prices @(rf/subscribe [:prices])
        data (get prices crypto-id)
        _ (js/console.log "🔍 CRYPTO CARD DATA for" crypto-id ":" data)
        _name (get-crypto-name crypto-id)
        price (get data "usd")
        change (get data "usd_24h_change")
        volume (get data "usd_24h_vol")
        high (get data "day_high")
        low (get data "day_low")
        trades-24h (get data "trades_24h")
        _data-source (get data "dataSource")  ; Market feed indicator data
        ;; Enhanced FIGR stock data (copy V2)
        is-stock? (= (get data "type") "stock")
        company-name (get data "company_name")
        exchange (get data "exchange")
        open-price (get data "open")
        _day-high (get data "day_high")
        _day-low (get data "day_low")
        fifty-two-week-high (get data "fifty_two_week_high")
        fifty-two-week-low (get data "fifty_two_week_low")
        _previous-close (get data "previous_close")
        ;; Portfolio data (hybrid - use atoms)
        portfolio-quantity (get @portfolio-atoms/portfolio-atom crypto-id 0)
        holding-value (portfolio-atoms/calculate-holding-value portfolio-quantity price)
        ;; Currency data (V2 feature!)
        current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        ;; Global data sources
        data-sources @(rf/subscribe [:data-sources])
        historical-data @(rf/subscribe [:historical-data crypto-id])]

        ;; Fetch historical data for all crypto assets (not stocks)
    (when (not= (get data "type") "stock")
      (js/console.log "🔎" (str/upper-case crypto-id) "card - historical-data value:" historical-data "nil?" (nil? historical-data) "empty?" (empty? historical-data))
      (when (or (nil? historical-data) (empty? historical-data))
        (js/console.log "🚀 Triggering fetch for" crypto-id)
        (rf/dispatch [:fetch-historical-data crypto-id])))

    [:div {:class (stale-data-card-styling data "relative bg-white/[0.03] border border-white/10 rounded-3xl p-6 backdrop-blur-lg transition-all duration-300 ease-out hover:scale-[1.02] hover:-translate-y-1 hover:bg-white/[0.06] hover:border-white/20 hover:shadow-2xl hover:shadow-purple-500/10 scan-line overflow-hidden animate-fade-in")}
     ;; Background chart for all crypto assets  
     [chart/background-chart crypto-id]
     [stale-data-warning crypto-id data]
     [crypto-card-header crypto-id is-stock? company-name exchange]
     [crypto-card-price price crypto-id current-currency exchange-rates data-sources data]
     [crypto-card-change change]
     ;; Different stats for stocks vs crypto (copy V2 logic)
     (if is-stock?
       [stock-stats open-price trades-24h crypto-id]
       [crypto-stats volume trades-24h crypto-id price current-currency exchange-rates])
     ;; 24h High/Low for crypto or 52-week range for stocks (copy V2 logic)
     (if is-stock?
       [stock-52w-range fifty-two-week-high fifty-two-week-low crypto-id current-currency exchange-rates]
       [crypto-card-high-low high low crypto-id current-currency exchange-rates])
     ;; Portfolio value display
     [portfolio-section holding-value crypto-id current-currency exchange-rates]]))

;; Main grid (copy V2)
(defn crypto-grid []
  (let [sorted-keys @(rf/subscribe [:sorted-price-keys])]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 mb-10"}
     (doall (for [crypto-id sorted-keys]
              ^{:key crypto-id} [crypto-card crypto-id]))]))

;; Data sources indicator
(defn data-sources-display []
  (let [data-sources @(rf/subscribe [:data-sources])]
    (when (and data-sources (> (count data-sources) 0))
      [:div {:class "fixed top-5 left-1/2 transform -translate-x-1/2 z-10"}
       [:div {:class "inline-flex items-center px-3 py-1.5 rounded-full bg-white/[0.03] border border-white/10 text-xs"}
        [:span {:class "text-gray-400 mr-2"} "Data sources:"]
        (for [[idx source] (map-indexed vector data-sources)]
          ^{:key idx}
          [:span {:class "inline-flex items-center"}
           [:span {:class "ml-1 text-xs font-semibold px-2 py-0.5 rounded border border-neon-cyan/40 text-neon-cyan bg-neon-cyan/10"}
            (get-feed-indicator "crypto" source)]
           (when (< idx (dec (count data-sources)))
             [:span {:class "text-gray-500 mx-1"} "•"])])]])))

;; Last update footer (copy V2)
(defn last-update-footer []
  (let [last-update @(rf/subscribe [:last-update])
        flash? @(rf/subscribe [:flash-active?])]
    (when last-update
      [:div {:class "fixed top-5 right-5 z-10"}
       [:div {:class (str "inline-flex items-center px-4 py-2 rounded-full text-gray-400 text-xs transition-all duration-300 "
                          (if flash?
                            "bg-neon-green/20 border border-neon-green/40 text-neon-green"
                            "bg-white/[0.03] border border-white/10"))}
        [:span {:class (str "w-2 h-2 rounded-full mr-2 "
                            (if flash?
                              "bg-neon-green animate-ping"
                              "bg-neon-green animate-pulse-dot"))}]
        "Last updated: " last-update]])))

;; Portfolio modal components (copy V2 exactly)
(defn modal-backdrop [content]
  [:div {:class "fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"}
   content])

(defn modal-container [content]
  [:div {:class "bg-gray-900 border border-white/20 rounded-2xl p-6 max-w-md w-full"}
   content])

(defn modal-header [icon symbol close-fn]
  [:div {:class "flex items-center justify-between mb-6"}
   [:div {:class "flex items-center"}
    [:span {:class "text-2xl mr-3"} icon]
    [:h2 {:class "text-xl font-bold text-white"} symbol " Portfolio"]]
   [:button {:class "text-gray-400 hover:text-white text-2xl"
             :on-click close-fn}
    "×"]])

(defn holdings-display [current-quantity holding-value crypto-id]
  [:div {:class "text-center"}
   [:div {:class "text-sm text-gray-400"} "Current Holdings"]
   [:div {:class "text-2xl font-bold text-white"} (format-number current-quantity 4)]
   (when holding-value
     [:div {:class "text-lg text-gray-300 mt-1"} (format-price holding-value crypto-id)])])

(defn quantity-input [current-quantity]
  [:div {:class "flex flex-col space-y-2"}
   [:label {:class "text-sm text-gray-400"} "Enter Quantity:"]
   [:input {:type "number"
            :step "0.0001"
            :placeholder "0.0000"
            :class "bg-gray-800 border border-white/20 rounded-lg px-3 py-2 text-white text-center focus:outline-none focus:border-white/40"
            :default-value current-quantity
            :id "quantity-input"}]])

(defn action-buttons [save-fn cancel-fn]
  [:div {:class "flex space-x-2"}
   [:button {:class "flex-1 bg-green-600 hover:bg-green-700 text-white rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
             :on-click save-fn}
    "Save"]
   [:button {:class "flex-1 bg-gray-600 hover:bg-gray-700 text-white rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
             :on-click cancel-fn}
    "Cancel"]])

(defn input-form [current-quantity save-fn cancel-fn]
  [:div {:class "border-t border-white/10 pt-4 space-y-3"}
   [quantity-input current-quantity]
   [action-buttons save-fn cancel-fn]])

(defn portfolio-panel []
  (when-let [crypto-id @portfolio-atoms/show-portfolio-panel]
    (let [symbol (get-crypto-symbol crypto-id)
          icon (get-crypto-icon crypto-id)
          current-quantity (get @portfolio-atoms/portfolio-atom crypto-id 0)
          price (get-in @(rf/subscribe [:prices]) [crypto-id "usd"] 0)
          holding-value (portfolio-atoms/calculate-holding-value current-quantity price)
          close-fn #(reset! portfolio-atoms/show-portfolio-panel nil)
          save-fn #(do
                     (let [input-val (-> js/document (.getElementById "quantity-input") .-value js/parseFloat)]
                       (when-not (js/isNaN input-val)
                         (swap! portfolio-atoms/portfolio-atom assoc crypto-id input-val)
                         (portfolio-atoms/persist-portfolio)
                         (reset! portfolio-atoms/show-portfolio-panel nil))))
          header [modal-header icon symbol close-fn]
          holdings [holdings-display current-quantity holding-value crypto-id]
          form [input-form current-quantity save-fn close-fn]
          content [:div {:class "space-y-4"} holdings form]]
      [modal-backdrop
       [modal-container
        [:div header content]]])))

;; Exchange rate indicator (copy V2)
(defn exchange-rate-indicator []
  (let [current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        using-mock? @(rf/subscribe [:currency/using-mock-rates?])
        currency-key (keyword current-currency)
        exchange-rate (get exchange-rates currency-key)]
    (when (and exchange-rate (not= current-currency "USD"))
      [:div {:class "flex justify-center mb-6"}
       [:div {:class (str "text-sm bg-white/5 border rounded-lg px-4 py-2 "
                          (if using-mock?
                            "border-amber-500/40 text-amber-400"
                            "border-blue-500/40 text-blue-400"))}
        [:div {:class "flex items-center"}
         [:span {:class "mr-2"} "💱"]
         [:span
          (str "1 USD = " (format-number exchange-rate 3) " " current-currency)]
         (when using-mock?
           [:div {:class "text-xs text-amber-300 ml-2"} "(Mock)"])]]])))

;; Currency selector - modular, composable functions
(defn currency-selector-header [close-fn]
  [:div {:class "flex items-center justify-between mb-6"}
   [:div {:class "flex items-center"}
    [:span {:class "text-2xl mr-3"} "💱"]
    [:h2 {:class "text-xl font-bold text-white"} "Select Currency"]]
   [:button {:class "text-gray-400 hover:text-white text-2xl" :on-click close-fn} "×"]])

(defn currency-button-item [currency current-currency select-fn]
  [:button {:class (str "flex items-center justify-between p-3 rounded-lg border transition-colors "
                        (if (= current-currency (:code currency))
                          "bg-blue-600 border-blue-500 text-white"
                          "bg-gray-800 border-gray-600 hover:bg-gray-700 text-gray-300"))
            :on-click #(select-fn (:code currency))}
   [:div {:class "flex items-center"}
    [:span {:class "text-lg mr-2"} (:symbol currency)]
    [:div
     [:div {:class "text-sm font-semibold"} (:code currency)]
     [:div {:class "text-xs opacity-75"} (:name currency)]]]
   (when (= current-currency (:code currency))
     [:span {:class "text-blue-300"} "✓"])])

(defn currency-grid [current-currency select-fn]
  [:div {:class "grid grid-cols-2 gap-3 max-h-80 overflow-y-auto"}
   (doall (for [currency supported-currencies]
            ^{:key (:code currency)}
            [currency-button-item currency current-currency select-fn]))])

(defn currency-selector-panel []
  (when @(rf/subscribe [:currency/show-selector?])
    (let [current-currency @(rf/subscribe [:currency/current])
          close-fn #(rf/dispatch [:currency/hide-selector])
          select-fn #(rf/dispatch [:currency/select %])]
      [modal-backdrop
       [modal-container
        [:div
         [currency-selector-header close-fn]
         [currency-grid current-currency select-fn]]]])))

(defn currency-toggle []
  [:div])

;; Version display component
(defn version-display []
  [:div {:class "fixed top-4 left-4 z-50 bg-black/80 border border-white/20 rounded-lg px-3 py-1.5 text-xs text-gray-300"}
   [:span {:class "text-neon-cyan"} "V3"]
   [:span {:class "mx-1"} "•"]
   [:span VERSION]])

;; Widget mode detection
(defn is-widget-mode?
  "Check if we're in widget mode based on URL parameters"
  []
  (let [url (.-href js/window.location)
        is-widget (or (.includes url "widget=")
                      (.includes url "#widget"))]
    (js/console.log "🎯 Widget mode check - URL:" url "Is widget:" is-widget)
    is-widget))

;; Extract widget size from URL
(defn get-widget-size []
  (let [url (.-search js/window.location)]
    (if-let [match (.match url #"widget=(\w+)")]
      (aget match 1)
      "medium")))

;; Compact crypto card for widgets
(defn widget-crypto-card [crypto-id]
  (let [data @(rf/subscribe [:crypto-data crypto-id])
        price (get data "usd" 0)
        change (get data "usd_24h_change" 0)
        icon (get crypto-icons (keyword crypto-id) "💰")
        display-name (get-crypto-name crypto-id)
        current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        is-stale? (is-stale-data? data)]
    [:div {:class (str "bg-white/[0.08] rounded-xl p-3 text-center border border-white/10 "
                       (when is-stale? "border-neon-red/40 bg-neon-red/10"))}
     [:div {:class "text-lg mb-1"} icon]
     [:div {:class "text-xs font-semibold mb-1 text-gray-300"} display-name]
     [:div {:class (str "text-sm font-bold mb-1 "
                        (if is-stale? "text-neon-red" "text-white"))}
      (format-price price crypto-id current-currency exchange-rates)]
     [:div {:class (str "text-xs font-semibold "
                        (change-classes (price-positive? change)))}
      (str (if (>= change 0) "+" "") (format-number change 2) "%")]
     (when is-stale?
       [:div {:class "text-xs text-neon-red mt-1"} "⚠ STALE"])]))

;; Widget-specific compact layout
(defn widget-layout []
  (let [widget-size (get-widget-size)
        sorted-keys @(rf/subscribe [:sorted-price-keys])
        top-assets (take (case widget-size
                           "small" 2   ; Show top 2 assets
                           "medium" 4  ; Show top 4 assets  
                           "large" 6   ; Show top 6 assets
                           4) sorted-keys)]
    (js/console.log "🎯 Widget layout - Size:" widget-size "Assets:" (count top-assets) "Keys:" (clj->js sorted-keys))
    [:div {:class "widget-container bg-black text-white p-3 font-inter"
           :id "widget-ready"
           :style {:min-height "400px"}}  ; Fixed height, signal for Playwright
     [:div {:class "grid gap-2"
            :style {:grid-template-columns
                    (case widget-size
                      "small" "1fr"
                      "medium" "1fr 1fr"
                      "large" "1fr 1fr 1fr"
                      "1fr 1fr")}}
      (if (empty? top-assets)
        [:div {:class "text-center text-gray-400 p-4"} "Loading widget data..."]
        (doall (for [crypto-id top-assets]
                 ^{:key crypto-id} [widget-crypto-card crypto-id])))]
     ;; Compact timestamp
     [:div {:class "text-xs text-gray-400 text-center mt-3 opacity-70"}
      "Live Data"]]))

;; Main app component with widget mode detection
(defn app-component []
  (let [loading? @(rf/subscribe [:loading?])
        error @(rf/subscribe [:error-message])]
    (if (is-widget-mode?)
      ;; Widget mode - compact layout optimized for screenshots
      (cond
        error [:div {:class "widget-container bg-black text-white p-4 text-center min-h-screen flex items-center justify-center"
                     :id "widget-ready"}
               [:div {:class "text-red-400 text-sm"} "Data Error"]]
        loading? [:div {:class "widget-container bg-black text-white p-4 text-center min-h-screen flex items-center justify-center"
                        :id "widget-ready"}
                  [:div {:class "text-gray-400 text-sm"} "Loading..."]]
        :else [widget-layout])
      ;; Normal mode - full layout  
      [:div
       [version-display]           ; Version indicator in top-left!
       [portfolio-summary-header]  ; V2 feature!
       [exchange-rate-indicator]   ; V2 market feed indicator!
       [currency-toggle]           ; V2 feature!
       (cond
         error [error-view error]
         loading? [loading-view]
         :else [:div
                [crypto-grid]
                [last-update-footer]])
       [portfolio-panel]
       [currency-selector-panel]])))
