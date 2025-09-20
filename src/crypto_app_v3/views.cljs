(ns crypto-app-v3.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; Copy V2 data constants (small, focused)
(def crypto-icons
  {:btc "₿" :eth "Ξ" :link "⬡" :sol "◎" :uni "🦄"
   :xrp "💰" :hash "🔗" :figr_heloc "🏠" :figr "📈"})

(def crypto-symbols
  {:btc "BTC" :eth "ETH" :link "LINK" :sol "SOL" :uni "UNI"
   :xrp "XRP" :hash "HASH" :figr_heloc "FIGR_HELOC" :figr "FIGR"})

;; Copy V2 formatting functions (small, pure)
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

;; Small helper functions for crypto card
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

;; Small UI components
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

;; Crypto card components (broken into small functions)
(defn crypto-card-icon [icon]
  [:div {:class "w-11 h-11 rounded-xl mr-4 flex items-center justify-center text-2xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-white/10"}
   icon])

(defn crypto-card-header [crypto-id]
  (let [name (get-crypto-name crypto-id)
        symbol (get-crypto-symbol crypto-id)
        icon (get-crypto-icon crypto-id)]
    [:div {:class "flex items-center justify-between mb-5"}
     [:div {:class "flex items-center"}
      [crypto-card-icon icon]
      [:div
       [:div {:class "text-lg font-semibold text-white tracking-wide"}
        name
        [:span {:class "text-sm text-gray-500 ml-2 uppercase"} symbol]]]]]))

(defn crypto-card-price [price crypto-id]
  [:div {:class "text-4xl font-bold mb-4 tabular-nums tracking-tight"}
   (format-price price crypto-id)])

(defn crypto-card-change [change]
  (let [positive? (price-positive? change)
        arrow (price-arrow positive?)
        classes (change-classes positive?)]
    [:div {:class (str "inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold mb-5 border " classes)}
     [:span {:class "mr-1.5 text-base"} arrow]
     (str (format-number (js/Math.abs (or change 0)) 2) "%")]))

(defn crypto-card-stats [volume trades-24h]
  [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
   [:div {:class "flex flex-col"}
    [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Volume"]
    [:div {:class "text-base font-semibold text-white tabular-nums"} (format-volume (or volume 0))]]
   [:div {:class "flex flex-col"}
    [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Trades"]
    [:div {:class "text-base font-semibold text-white tabular-nums"} (format-number (or trades-24h 0) 0)]]])

(defn crypto-card-bid-ask [bid ask crypto-id]
  (when (and bid ask (> bid 0) (> ask 0))
    [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
     [:div {:class "flex flex-col"}
      [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Bid"]
      [:div {:class "text-sm font-semibold text-green-400 tabular-nums"} (format-price bid crypto-id)]]
     [:div {:class "flex flex-col"}
      [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Ask"]
      [:div {:class "text-sm font-semibold text-red-400 tabular-nums"} (format-price ask crypto-id)]]]))

;; Main crypto card (composed from small functions)  
(defn crypto-card [crypto-id]
  (let [prices @(rf/subscribe [:prices])
        data (get prices crypto-id)
        price (get data "usd")
        change (get data "usd_24h_change")
        volume (get data "usd_24h_vol")
        bid (get data "bid")
        ask (get data "ask")
        trades-24h (get data "trades_24h")]
    [:div {:class "relative bg-white/[0.03] border border-white/10 rounded-3xl p-6 backdrop-blur-lg transition-all duration-300 ease-out hover:scale-[1.02] hover:-translate-y-1 hover:bg-white/[0.06] hover:border-white/20 hover:shadow-2xl hover:shadow-purple-500/10 scan-line overflow-hidden animate-fade-in"}
     [crypto-card-header crypto-id]
     [crypto-card-price price crypto-id]
     [crypto-card-change change]
     [crypto-card-stats volume trades-24h]
     [crypto-card-bid-ask bid ask crypto-id]]))

;; Last update footer
(defn last-update-footer [last-update flash?]
  (when last-update
    [:div {:class "text-center mt-15 pb-10"}
     [:div {:class (str "inline-flex items-center px-6 py-3 rounded-full text-gray-400 text-sm transition-all duration-300 "
                        (if flash?
                          "bg-neon-green/20 border border-neon-green/40 text-neon-green"
                          "bg-white/[0.03] border border-white/10"))}
      [:span {:class (str "w-2 h-2 rounded-full mr-2.5 "
                          (if flash?
                            "bg-neon-green animate-ping"
                            "bg-neon-green animate-pulse-dot"))}]
      "Last updated: " last-update]]))

;; Main app component (composed from small functions)
(defn app []
  (let [loading? @(rf/subscribe [:loading?])
        error @(rf/subscribe [:error-message])
        flash? @(rf/subscribe [:flash-active?])
        last-update @(rf/subscribe [:last-update])
        sorted-keys @(rf/subscribe [:sorted-price-keys])]
    [:div
     (cond
       error [error-view error]
       loading? [loading-view]
       :else [:div
              [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 mb-10"}
               (doall (for [crypto-id sorted-keys]
                        ^{:key crypto-id} [crypto-card crypto-id]))]
              [last-update-footer last-update flash?]])]))
