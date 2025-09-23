(ns crypto-app-v3.card-v5
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [crypto-app-v3.chart-v5 :as chart-v5]
            [crypto-app-v3.currency :as curr]))

;; V5 Card Component - Professional Terminal Style
;; Single asset prototype (HASH first)

(defn asset-description
  "Asset description below chart with feed indicator on right"
  [crypto-id feed-indicator]
  (let [descriptions {"hash" "Provenance Blockchain HASH Token"
                      "figr" "Figure Technologies Inc."
                      "btc" "Bitcoin"
                      "eth" "Ethereum"
                      "sol" "Solana"
                      "link" "Chainlink"
                      "uni" "Uniswap"
                      "xrp" "XRP"}]
    [:div {:class "flex items-center justify-between mb-3"}
     [:div {:class "text-sm text-gray-400 font-medium"}
      (get descriptions crypto-id (str/capitalize (name crypto-id)))]
     [:div {:class "bg-neon-cyan/10 border border-neon-cyan/40 text-neon-cyan rounded px-2 py-0.5 font-semibold text-xs"}
      feed-indicator]]))

(defn stats-row
  "Volume and trades row with currency conversion"
  [volume trades current-currency currency-symbol exchange-rates]
  (let [converted-volume (curr/convert-currency volume current-currency exchange-rates)]
    [:div {:class "flex items-center justify-between text-xs text-gray-400 overlay-tier3"}
     [:div {:class "flex items-center"}
      [:span "Volume: "]
      [:span {:class "text-white"} (str currency-symbol (chart-v5/format-number converted-volume 0))]
      [:button {:class "text-neon-green hover:text-neon-green/80 ml-2 font-medium"
                :on-click #(rf/dispatch [:currency/show-selector])}
       current-currency]]
     [:span (str "Trades: " trades)]]))

(defn portfolio-section
  "Portfolio management section - add/edit holdings with quantity display"
  [crypto-id]
  (let [quantity @(rf/subscribe [:portfolio/qty crypto-id])
        value @(rf/subscribe [:portfolio/value crypto-id])
        current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        currency-symbol (curr/get-currency-symbol current-currency)
        ;; Convert value to current currency
        converted-value (when value (curr/convert-currency value current-currency exchange-rates))]
    [:div {:class "mt-3 pt-3 border-t border-white/10"}
     (if (> quantity 0)
       ;; Edit state - PV: $123.45 USD (quantity token) edit
       [:div {:class "flex items-center justify-between text-xs"}
        [:div {:class "flex items-center text-gray-300"}
         [:span {:class "text-gray-400"} "PV:"]
         (when converted-value
           [:span {:class "text-white ml-2"}
            (str currency-symbol (chart-v5/format-number converted-value 2))])
         [:button {:class "text-neon-green hover:text-neon-green/80 ml-2 font-medium"
                   :on-click #(rf/dispatch [:currency/show-selector])}
          current-currency]
         [:span {:class "text-gray-400 ml-2"}
          (str "(" quantity " " (str/lower-case crypto-id) ")")]]
        [:button {:class "text-neon-green hover:text-neon-green/80"
                  :on-click #(rf/dispatch [:portfolio/show-panel crypto-id])}
         "✏️"]]
       ;; Add state - show add button
       [:div {:class "text-center"}
        [:button {:class "text-neon-green hover:text-neon-green/80 text-sm font-medium"
                  :on-click #(rf/dispatch [:portfolio/show-panel crypto-id])}
         "Add to Portfolio"]])]))

(defn crypto-card-v5
  "V5 crypto card - professional terminal style with square chart"
  [crypto-id]
  (let [prices @(rf/subscribe [:prices])
        data (get prices crypto-id)
        price (get data "usd" 0)
        high (get data "day_high" 0)
        low (get data "day_low" 0)
        live-change (get data "usd_24h_change" 0)
        volume (get data "usd_24h_vol" 0)  ; Keep full volume amount
        trades (get data "trades_24h" 0)
        asset-type (get data "type" "crypto")
        feed-indicator (if (= asset-type "stock") "YF" "FM")
        current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        currency-symbol (curr/get-currency-symbol current-currency)
        current-period @(rf/subscribe [:chart/current-period])
        historical-data @(rf/subscribe [:historical-data crypto-id])
        ;; Calculate metrics from chart data for consistency
        chart-metrics (if (and historical-data (vector? historical-data) (= (count historical-data) 2))
                        (let [[_ prices] historical-data]
                          (when (and (seq prices) (>= (count prices) 2))
                            (let [start-price (first prices)
                                  end-price (last prices)
                                  chart-high (apply max prices)
                                  chart-low (apply min prices)
                                  pct-change (* 100 (/ (- end-price start-price) start-price))]
                              (js/console.log "🔴🟢 CHART-METRICS" crypto-id "Start:" start-price "End:" end-price "High:" chart-high "Low:" chart-low "Pct:" pct-change)
                              {:change pct-change :high chart-high :low chart-low})))
                        (do
                          (js/console.log "🔴🟢 LIVE-FALLBACK" crypto-id "Using live data - High:" high "Low:" low "Change:" live-change)
                          {:change live-change :high high :low low}))
        chart-change (:change chart-metrics)
        chart-high (:high chart-metrics)
        chart-low (:low chart-metrics)
        ;; Currency conversion for all overlay prices
        converted-price (curr/convert-currency price current-currency exchange-rates)
        converted-high (curr/convert-currency chart-high current-currency exchange-rates)
        converted-low (curr/convert-currency chart-low current-currency exchange-rates)]

;; Trigger historical data fetch if missing (same as V4)
    (when (or (nil? historical-data) (empty? historical-data))
      (js/console.log "🚀 V5 Triggering historical fetch for" crypto-id)
      (rf/dispatch [:fetch-historical-data crypto-id]))

    [:div {:class "bg-white/[0.03] border border-white/10 rounded-2xl p-4 backdrop-blur-lg hover:bg-white/[0.06] transition-all duration-300"}
     ;; Square chart with overlays
     [:div {:class "relative"}
      [chart-v5/square-chart-container crypto-id]
      [chart-v5/chart-overlay-symbol crypto-id]
      [chart-v5/chart-overlay-high converted-high currency-symbol]
      [chart-v5/chart-overlay-current-price converted-price currency-symbol current-currency]
      [chart-v5/chart-overlay-change chart-change]
      [chart-v5/chart-overlay-period current-period]
      [chart-v5/chart-overlay-low converted-low currency-symbol]]

     ;; Asset description with feed indicator
     [asset-description crypto-id feed-indicator]

     ;; Stats row with currency button
     [stats-row volume trades current-currency currency-symbol exchange-rates]

      ;; Portfolio section
     [portfolio-section crypto-id]]))

;; V5 Portfolio Total Value Display
(defn portfolio-total-v5
  "V5 total portfolio value display with currency conversion"
  []
  (let [total-value @(rf/subscribe [:portfolio/total-value])
        current-currency @(rf/subscribe [:currency/current])
        exchange-rates @(rf/subscribe [:currency/exchange-rates])
        currency-symbol (curr/get-currency-symbol current-currency)
        converted-value (curr/convert-currency total-value current-currency exchange-rates)]
    (when (> total-value 0)
      [:div {:class "bg-gradient-to-r from-neon-green/5 to-neon-cyan/5 border border-neon-green/20 rounded-2xl p-6 mb-8"}
       [:div {:class "text-center"}
        [:div {:class "text-sm text-neon-green uppercase mb-2 tracking-widest font-medium"} "Total Portfolio Value"]
        [:div {:class "text-4xl font-bold text-white tabular-nums flex items-center justify-center gap-3"}
         (str currency-symbol (chart-v5/format-number converted-value 2))
         [:button {:class "text-neon-green hover:text-neon-green/80 text-lg font-medium"
                   :on-click #(rf/dispatch [:currency/show-selector])}
          current-currency]]]])))

;; V5 Grid Component - following Oracle guidance
(defn crypto-grid-v5
  "Responsive grid of V5 cards, driven by :sorted-price-keys"
  []
  (let [crypto-ids @(rf/subscribe [:sorted-price-keys])]
    (if (empty? crypto-ids)
      [:div {:class "text-gray-400 text-center p-6"} "Loading market data…"]
      [:div {:class "grid gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 w-full"}
       (for [cid crypto-ids]
         ^{:key cid} [crypto-card-v5 cid])])))

;; V5 prototype component - can be called from anywhere
(defn v5-prototype-section
  "V5 prototype section - shows all asset cards when feature flag enabled"
  []
  (let [new-layout? @(rf/subscribe [:ui/new-layout?])]
    (when new-layout?
      [:section {:class "max-w-7xl mx-auto px-4 py-10 space-y-6"}
       [:div {:class "text-center"}
        [:h2 {:class "text-xl font-bold text-neon-green"} "V5 PROTOTYPE"]
        [:p {:class "text-sm text-gray-400"} "Professional card grid"]]
       [portfolio-total-v5]
       [crypto-grid-v5]])))
