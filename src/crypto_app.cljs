(ns crypto-app
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

;; Fine-grained state atoms for selective updates
(def prices-atom (r/atom {}))
(def price-keys-atom (r/atom []))
(def last-update-atom (r/atom nil))
(def loading-atom (r/atom true))
(def error-atom (r/atom nil))
(def initial-load-complete (r/atom false))
(def update-flash-atom (r/atom false))

;; Legacy app-state for backward compatibility during migration
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

(defn crypto-card [crypto-id]
  (js/console.log "crypto-card rendering for:" crypto-id)
  (let [data @(r/cursor prices-atom [crypto-id])
        name (str/upper-case crypto-id)
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
  (js/console.log "app-component rendering")
  (let [last-update @last-update-atom
        loading @loading-atom
        error @error-atom
        flash @update-flash-atom]
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
               (let [price-keys @price-keys-atom
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

(defn fetch-crypto-data []
  (js/console.log "Starting crypto data fetch...")
  ;; Only show loading states for initial load
  (when (not @initial-load-complete)
    (when (not @loading-atom)
      (reset! loading-atom true))
    (when @error-atom  
      (reset! error-atom nil)))
  (-> (js/fetch (str "./data/crypto-prices.json?t=" (js/Date.now)))
      (.then (fn [response]
               (js/console.log "Fetch response received, status:" (.-status response))
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (js/console.log "JSON data parsed:" data)
               (let [js-data (js->clj data :keywordize-keys false)
                     prices (dissoc js-data "timestamp" "source" "last_update")  
                     last-update (get js-data "last_update")]
                 (js/console.log "Processed prices:" prices)
                 ;; Update new granular atoms with differential updates
                 (let [old-prices @prices-atom
                       new-keys (set (keys prices))
                       old-keys (set @price-keys-atom)]
                   ;; Only update individual coins that changed (compare key fields)
                   (doseq [[coin-id new-data] prices]
                     (let [old-data (get old-prices coin-id)
                           old-price (get old-data "usd")
                           new-price (get new-data "usd")
                           old-change (get old-data "usd_24h_change")
                           new-change (get new-data "usd_24h_change")]
                       (when (or (not= old-price new-price) 
                                 (not= old-change new-change))
                         (js/console.log "Updating coin:" coin-id "price:" old-price "→" new-price "change:" old-change "→" new-change)
                         (swap! prices-atom assoc coin-id new-data))))
                   ;; Only update keys if they actually changed
                   (when (not= new-keys old-keys)
                     (reset! price-keys-atom (keys prices))))
                 (when (not= @last-update-atom last-update)
                   (js/console.log "Updating timestamp:" @last-update-atom "→" last-update)
                   (reset! last-update-atom last-update))
                 ;; Flash indicator briefly to show data was fetched (always)
                 (reset! update-flash-atom true)
                 (js/setTimeout #(reset! update-flash-atom false) 800)
                 ;; Only update loading state for initial load
                 (when (not @initial-load-complete)
                   (reset! initial-load-complete true)
                   (when @loading-atom  
                     (reset! loading-atom false)))
                 ;; DISABLED: Update legacy app-state for backward compatibility
                 ;; (swap! app-state assoc
                 ;;        :prices prices
                 ;;        :last-update last-update
                 ;;        :loading false
                 ;;        :error nil)
                 )))
      (.catch (fn [error]
                (js/console.error "Failed to fetch crypto data:" error)
                (js/console.log "Error atom before:" @error-atom)
                ;; Update new granular atoms
                (reset! loading-atom false)
                (reset! error-atom (.-message error))
                (js/console.log "Error atom after:" @error-atom)
                ;; DISABLED: Update legacy app-state for backward compatibility
                ;; (swap! app-state assoc
                ;;        :loading false
                ;;        :error (.-message error))
                ))))

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
     (when @loading-atom
       ;; Update new granular atoms
       (reset! loading-atom false)
       (reset! error-atom "Timeout loading data")
       ;; Update legacy app-state for backward compatibility
       (swap! app-state assoc
              :loading false
              :error "Timeout loading data")))
   10000)

  ;; Fetch data immediately and then every 30 seconds
  (fetch-crypto-data)
  (js/setInterval fetch-crypto-data 30000))

;; Auto-start when script loads
(init)