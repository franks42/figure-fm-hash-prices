(ns crypto-app-v3.card-v5
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [crypto-app-v3.chart-v5 :as chart-v5]))

;; V5 Card Component - Professional Terminal Style
;; Single asset prototype (HASH first)

(defn asset-description
  "Asset description below chart, left-aligned"
  [crypto-id]
  (let [descriptions {"hash" "Figure Markets Hash Token"
                      "figr" "Figure Technologies Inc."
                      "btc" "Bitcoin"
                      "eth" "Ethereum"
                      "sol" "Solana"
                      "link" "Chainlink"
                      "uni" "Uniswap"
                      "xrp" "XRP"}]
    [:div {:class "text-sm text-gray-400 mb-3 font-medium"}
     (get descriptions crypto-id (str/capitalize (name crypto-id)))]))

(defn stats-row
  "Volume, trades, and feed indicator in consolidated row"
  [volume trades feed-indicator]
  [:div {:class "flex items-center justify-between text-xs text-gray-400 overlay-tier3"}
   [:div {:class "flex items-center space-x-4"}
    [:span (str "Volume: $" (chart-v5/format-number volume 1) "M")]
    [:span (str "Trades: " trades)]]
   [:div {:class "bg-neon-cyan/10 border border-neon-cyan/40 text-neon-cyan rounded px-2 py-0.5 font-semibold"}
    feed-indicator]])

(defn crypto-card-v5
  "V5 crypto card - professional terminal style with square chart"
  [crypto-id]
  (let [prices @(rf/subscribe [:prices])
        data (get prices crypto-id)
        volume (/ (get data "usd_24h_vol" 0) 1000000)  ; Convert to millions
        trades (get data "trades_24h" 0)
        asset-type (get data "type" "crypto")
        feed-indicator (if (= asset-type "stock") "YF" "FM")]
    [:div {:class "bg-white/[0.03] border border-white/10 rounded-2xl p-4 backdrop-blur-lg hover:bg-white/[0.06] transition-all duration-300"}
     ;; Square chart with overlays
     [chart-v5/chart-v5 crypto-id]

     ;; Asset description
     [asset-description crypto-id]

     ;; Stats row  
     [stats-row volume trades feed-indicator]]))

;; Test component - shows HASH only when feature flag is enabled
(defn hash-prototype-test []
  (let [new-layout? @(rf/subscribe [:ui/new-layout?])]
    (when new-layout?
      [:div {:class "max-w-xs mx-auto mt-8"}
       [:div {:class "text-center mb-4"}
        [:h2 {:class "text-xl font-bold text-neon-green"} "V5 PROTOTYPE"]
        [:p {:class "text-sm text-gray-400"} "HASH Card - New Layout"]]
       [crypto-card-v5 "hash"]])))
