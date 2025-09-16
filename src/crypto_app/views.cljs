(ns crypto-app.views
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [crypto-app.state :as state]
            [crypto-app.portfolio :as portfolio]))

;; Utility functions for formatting
(defn format-number [n decimals]
  (.toLocaleString n "en-US"
                   #js{:minimumFractionDigits decimals
                       :maximumFractionDigits decimals}))

(defn format-price [price crypto-id]
  (let [decimals (if (= crypto-id "hash") 3 2)]
    (str "$" (format-number price decimals))))

(defn format-volume [vol]
  (cond
    (>= vol 1e9) (str "$" (format-number (/ vol 1e9) 2) "B")
    (>= vol 1e6) (str "$" (format-number (/ vol 1e6) 2) "M")
    :else (str "$" (format-number vol 0))))

(defn format-market-cap [cap]
  (format-volume cap))

;; UI Components
(defn crypto-card [crypto-id]
  (let [data @(r/cursor state/prices-atom [crypto-id])
        name (str/upper-case crypto-id)
        symbol (get state/crypto-symbols (keyword crypto-id) (str/upper-case crypto-id))
        icon (get state/crypto-icons (keyword crypto-id) "◆")
        price (get data "usd")
        change (get data "usd_24h_change")
        volume (get data "usd_24h_vol")
        bid (get data "bid")
        ask (get data "ask")
        trades-24h (get data "trades_24h")
        ;; Enhanced FIGR stock data
        is-stock? (= (get data "type") "stock")
        company-name (get data "company_name")
        exchange (get data "exchange")
        open-price (get data "open")
        day-high (get data "day_high")
        day-low (get data "day_low")
        fifty-two-week-high (get data "fifty_two_week_high")
        fifty-two-week-low (get data "fifty_two_week_low")
        previous-close (get data "previous_close")
        ;; Portfolio data
        quantity @(r/cursor state/portfolio-atom [crypto-id])
        holding-value (when quantity (portfolio/calculate-holding-value quantity price))
        ;; Display logic
        positive? (>= (or change 0) 0)
        arrow (if positive? "▲" "▼")
        change-classes (if positive?
                         "bg-neon-green/10 text-neon-green border-neon-green/20"
                         "bg-neon-red/10 text-neon-red border-neon-red/20")]
    [:div {:class "relative bg-white/[0.03] border border-white/10 rounded-3xl p-6 backdrop-blur-lg transition-all duration-300 ease-out hover:scale-[1.02] hover:-translate-y-1 hover:bg-white/[0.06] hover:border-white/20 hover:shadow-2xl hover:shadow-purple-500/10 scan-line overflow-hidden animate-fade-in"}
     [:div {:class "flex items-center justify-between mb-5"}
      [:div {:class "flex items-center"}
       [:div {:class "w-11 h-11 rounded-xl mr-4 flex items-center justify-center text-2xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-white/10"}
        icon]
       [:div
        [:div {:class "text-lg font-semibold text-white tracking-wide"}
         (if is-stock? company-name name)
         [:span {:class "text-sm text-gray-500 ml-2 uppercase"} symbol]]
        (when (and is-stock? exchange)
          [:div {:class "text-xs text-gray-500 mt-0.5"} exchange])]]]
     [:div {:class "text-4xl font-bold mb-4 tabular-nums tracking-tight"} (format-price price crypto-id)]
     [:div {:class (str "inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold mb-5 border " change-classes)}
      [:span {:class "mr-1.5 text-base"} arrow]
      (str (format-number (js/Math.abs (or change 0)) 2) "%")]
     ;; Main stats grid - different for stocks vs crypto
     (if is-stock?
       ;; Stock-specific stats
       [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Open"]
         [:div {:class "text-base font-semibold text-white tabular-nums"}
          (if open-price (format-price open-price crypto-id) "N/A")]]
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Volume"]
         [:div {:class "text-base font-semibold text-white tabular-nums"} (format-volume (or volume 0))]]]
       ;; Crypto-specific stats  
       [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Volume"]
         [:div {:class "text-base font-semibold text-white tabular-nums"} (format-volume (or volume 0))]]
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Trades"]
         [:div {:class "text-base font-semibold text-white tabular-nums"} (format-number (or trades-24h 0) 0)]]])
     ;; Bid/Ask for crypto or 52-week range for stocks
     (if is-stock?
       ;; 52-week high/low for stocks
       (when (and fifty-two-week-high fifty-two-week-low)
         [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "52W High"]
           [:div {:class "text-sm font-semibold text-green-400 tabular-nums"} (format-price fifty-two-week-high crypto-id)]]
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "52W Low"]
           [:div {:class "text-sm font-semibold text-red-400 tabular-nums"} (format-price fifty-two-week-low crypto-id)]]])
       ;; Bid/Ask for crypto
       (when (and bid ask (> bid 0) (> ask 0))
         [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Bid"]
           [:div {:class "text-sm font-semibold text-green-400 tabular-nums"} (format-price bid crypto-id)]]
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Ask"]
           [:div {:class "text-sm font-semibold text-red-400 tabular-nums"} (format-price ask crypto-id)]]]))]))

     ;; Portfolio button
[:div {:class "flex mt-4"}
 [:button {:class "flex-1 bg-white/[0.05] hover:bg-white/[0.10] border border-white/20 rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
           :onClick #(reset! state/show-portfolio-panel true)}
  "📊 Portfolio"]]

(defn app-component []
  (let [last-update @state/last-update-atom
        loading @state/loading-atom
        error @state/error-atom
        flash @state/update-flash-atom]
    [:div
     (cond
       error [:div {:class "bg-neon-red/10 border border-neon-red/20 text-neon-red p-5 rounded-xl my-5 text-center"}
              "Failed to load market data: " error
              [:br]
              "Retrying in 30 seconds..."]
       loading [:div {:class "text-center text-gray-400 text-xl py-24"}
                [:div {:class "inline-block w-10 h-10 border-3 border-gray-700 border-t-neon-green rounded-full animate-spin mb-5"}]
                [:div "Loading market data..."]]
       :else [:div
              [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 mb-10"}
               (let [price-keys @state/price-keys-atom
                     sorted-keys (sort-by (fn [crypto-id]
                                            (cond
                                              (= crypto-id "hash") "0-hash"  ; Put HASH first
                                              (= crypto-id "figr") "1-figr"  ; Put FIGR second
                                              :else crypto-id)) price-keys)]
                 (doall (for [crypto-id sorted-keys]
                          ^{:key crypto-id} [crypto-card crypto-id])))]
              (when last-update
                [:div {:class "text-center mt-15 pb-10"}
                 [:div {:class (str "inline-flex items-center px-6 py-3 rounded-full text-gray-400 text-sm transition-all duration-300 "
                                    (if flash
                                      "bg-neon-green/20 border border-neon-green/40 text-neon-green"
                                      "bg-white/[0.03] border border-white/10"))}
                  [:span {:class (str "w-2 h-2 rounded-full mr-2.5 "
                                      (if flash
                                        "bg-neon-green animate-ping"
                                        "bg-neon-green animate-pulse-dot"))}]
                  "Last updated: " last-update]])])]))
