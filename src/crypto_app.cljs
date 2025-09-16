(ns crypto-app
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(def app-state (r/atom {:prices {} :last-update nil :loading true :error nil}))

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

(defn crypto-card [crypto-id data]
  (let [name (str/upper-case crypto-id)
        symbol (get crypto-symbols (keyword crypto-id) (str/upper-case crypto-id))
        icon (get crypto-icons (keyword crypto-id) "◆")
        price (get data "usd")
        change (get data "usd_24h_change")
        volume (get data "usd_24h_vol")
        bid (get data "bid")
        ask (get data "ask")
        trades-24h (get data "trades_24h")
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
         name
         [:span {:class "text-sm text-gray-500 ml-2 uppercase"} symbol]]]]]
     [:div {:class "text-4xl font-bold mb-4 tabular-nums tracking-tight"} (format-price price crypto-id)]
     [:div {:class (str "inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold mb-5 border " change-classes)}
      [:span {:class "mr-1.5 text-base"} arrow]
      (str (format-number (js/Math.abs (or change 0)) 2) "%")]
     [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
      [:div {:class "flex flex-col"}
       [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Volume"]
       [:div {:class "text-base font-semibold text-white tabular-nums"} (format-volume (or volume 0))]]
      [:div {:class "flex flex-col"}
       [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Trades"]
       [:div {:class "text-base font-semibold text-white tabular-nums"} (format-number (or trades-24h 0) 0)]]]
     (when (and bid ask (> bid 0) (> ask 0))
       [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Bid"]
         [:div {:class "text-sm font-semibold text-green-400 tabular-nums"} (format-price bid crypto-id)]]
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Ask"]
         [:div {:class "text-sm font-semibold text-red-400 tabular-nums"} (format-price ask crypto-id)]]])]))

(defn app-component []
  (let [{:keys [prices last-update loading error]} @app-state]
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
               (let [sorted-prices (sort-by (fn [[crypto-id _]] 
                                             (cond
                                               (= crypto-id "hash") "0-hash"  ; Put HASH first
                                               (= crypto-id "figr") "1-figr"  ; Put FIGR second
                                               :else crypto-id)) prices)]
                 (doall (for [[crypto-id data] sorted-prices]
                         ^{:key crypto-id} [crypto-card crypto-id data])))]
              (when last-update
                [:div {:class "text-center mt-15 pb-10"}
                 [:div {:class "inline-flex items-center bg-white/[0.03] border border-white/10 px-6 py-3 rounded-full text-gray-400 text-sm"}
                  [:span {:class "w-2 h-2 bg-neon-green rounded-full mr-2.5 animate-pulse-dot"}]
                  "Last updated: " last-update]])])]))

(defn fetch-crypto-data []
  (js/console.log "Starting crypto data fetch...")
  (swap! app-state assoc :loading true :error nil)
  (-> (js/fetch "./data/crypto-prices.json")
      (.then (fn [response]
               (js/console.log "Fetch response received, status:" (.-status response))
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (js/console.log "JSON data parsed:" data)
               (let [js-data (js->clj data)
                     prices (dissoc js-data "timestamp" "source" "last_update")
                     last-update (get js-data "last_update")]
                 (js/console.log "Processed prices:" prices)
                 (swap! app-state assoc 
                        :prices prices 
                        :last-update last-update 
                        :loading false
                        :error nil))))
      (.catch (fn [error]
                (js/console.error "Failed to fetch crypto data:" error)
                (swap! app-state assoc 
                       :loading false 
                       :error (.-message error))))))

;; Mount the Reagent app
(defn mount-app []
  (rdom/render [app-component] (js/document.getElementById "app")))

;; Initialize the application
(defn init []
  ;; Start the app
  (mount-app)
  
  ;; Add a timeout to show error if loading takes too long
  (js/setTimeout 
    (fn []
      (when (:loading @app-state)
        (swap! app-state assoc 
               :loading false 
               :error "Timeout loading data")))
    10000)
  
  ;; Fetch data immediately and then every 30 seconds
  (fetch-crypto-data)
  (js/setInterval fetch-crypto-data 30000))

;; Auto-start when script loads
(init)