(ns crypto-app
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(def app-state (r/atom {:prices {} :last-update nil :loading true :error nil}))

;; Crypto icons and symbols mapping
(def crypto-icons
  {:bitcoin "₿"
   :ethereum "Ξ"
   :cardano "₳"
   :polkadot "●"
   :chainlink "⬡"
   :solana "◎"
   :avalanche-2 "▲"
   :polygon "Ⓜ"})

(def crypto-symbols
  {:bitcoin "BTC"
   :ethereum "ETH"
   :cardano "ADA"
   :polkadot "DOT"
   :chainlink "LINK"
   :solana "SOL"
   :avalanche-2 "AVAX"
   :polygon "MATIC"})

(defn format-number [n decimals]
  (.toLocaleString n "en-US" 
    #js{:minimumFractionDigits decimals 
        :maximumFractionDigits decimals}))

(defn format-price [price]
  (str "$" (format-number price 2)))

(defn format-volume [vol]
  (cond
    (>= vol 1e9) (str "$" (format-number (/ vol 1e9) 2) "B")
    (>= vol 1e6) (str "$" (format-number (/ vol 1e6) 2) "M")
    :else (str "$" (format-number vol 0))))

(defn format-market-cap [cap]
  (format-volume cap))

(defn crypto-card [crypto-id data]
  (let [name (-> crypto-id (str/replace "-" " ") str/capitalize)
        symbol (get crypto-symbols (keyword crypto-id) "")
        icon (get crypto-icons (keyword crypto-id) "◆")
        price (get data "usd")
        change (get data "usd_24h_change")
        volume (get data "usd_24h_vol")
        market-cap (get data "usd_market_cap")
        positive? (>= change 0)
        arrow (if positive? "▲" "▼")
        change-class (if positive? "change-positive" "change-negative")]
    [:div {:class "crypto-card"}
     [:div {:class "crypto-header"}
      [:div {:class "crypto-info"}
       [:div {:class "crypto-icon"} icon]
       [:div 
        [:div {:class "crypto-name"} 
         name
         [:span {:class "crypto-symbol"} symbol]]]]]
     [:div {:class "price"} (format-price price)]
     [:div {:class (str "change-container " change-class)}
      [:span {:class "arrow"} arrow]
      (str (format-number (js/Math.abs change) 2) "%")]
     [:div {:class "stats"}
      [:div {:class "stat"}
       [:div {:class "stat-label"} "24h Volume"]
       [:div {:class "stat-value"} (format-volume volume)]]
      [:div {:class "stat"}
       [:div {:class "stat-label"} "Market Cap"]
       [:div {:class "stat-value"} (format-market-cap market-cap)]]]]))

(defn app-component []
  (let [{:keys [prices last-update loading error]} @app-state]
    [:div
     (cond
       error [:div {:class "error"} 
              "Failed to load market data: " error 
              [:br]
              "Retrying in 30 seconds..."]
       loading [:div {:class "loading"} 
                [:div {:class "loading-spinner"}]
                [:div "Loading market data..."]]
       :else [:div
              [:div {:class "crypto-grid"}
               (doall (for [[crypto-id data] prices]
                       ^{:key crypto-id} [crypto-card crypto-id data]))]
              (when last-update
                [:div {:class "footer"}
                 [:div {:class "last-update"}
                  [:span {:class "update-dot"}]
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