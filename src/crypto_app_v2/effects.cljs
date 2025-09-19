(ns crypto-app-v2.effects
  (:require [crypto-app-v2.state :as state]
            [clojure.string :as str]))

;; API data fetching and side effects

;; Mock exchange rates for testing (will be replaced by API data)
(def mock-exchange-rates
  {"EUR" 0.85
   "GBP" 0.73
   "JPY" 110.0
   "CAD" 1.25
   "AUD" 1.35
   "CHF" 0.92
   "CNY" 6.45
   "KRW" 1180.0
   "SEK" 9.75})

(defn show-fetch-indicator []
  (let [indicator (js/document.getElementById "fetch-indicator")]
    (when indicator
      (.add (.-classList indicator) "active"))))

(defn hide-fetch-indicator []
  (js/setTimeout
   (fn []
     (let [indicator (js/document.getElementById "fetch-indicator")]
       (when indicator
         (.remove (.-classList indicator) "active"))))
   state/SCAN_HIDE_DELAY_MS))

(defn flash-update-indicator []
  (reset! state/update-flash-atom true)
  (js/setTimeout #(reset! state/update-flash-atom false) state/FLASH_DURATION_MS))

(defn update-timestamp []
  (let [now (js/Date.)
        formatted-time (.toLocaleString now "en-US" #js{:year "numeric"
                                                        :month "2-digit"
                                                        :day "2-digit"
                                                        :hour "2-digit"
                                                        :minute "2-digit"
                                                        :second "2-digit"
                                                        :hour12 false})]
    (reset! state/last-update-atom formatted-time)))

(defn extract-symbol-key [asset]
  (when-let [symbol (get asset :symbol)]
    (keyword (str/lower-case (first (str/split symbol #"-"))))))

(defn get-data-source [crypto-id data-type]
  "Determine the actual data source for each asset"
  (cond
    (= crypto-id "figr") "Yahoo Finance"
    (contains? #{"hash" "figr_heloc"} crypto-id) "Figure Markets"
    (= data-type "stock") "Yahoo Finance"
    :else "Figure Markets"))

(defn transform-to-standardized
  ([old-data] (transform-to-standardized old-data false))
  ([old-data is-fallback]
   (let [prices (dissoc old-data "timestamp" "source" "last_update")]
     (mapv (fn [[crypto-id data]]
             {:symbol (get data "symbol" (.toUpperCase crypto-id))
              :currentPrice {:amount (get data "usd" 0)
                             :currency "USD"}
              :volume24h {:amount (get data "usd_24h_vol" 0)
                          :currency "USD"}
              :priceChange24h (get data "usd_24h_change" 0)
              :bidPrice (get data "bid" 0)
              :askPrice (get data "ask" 0)
              :dayHigh (get data "day_high" nil)
              :dayLow (get data "day_low" nil)
              :assetType (get data "type" "crypto")
              :dataSource (if is-fallback
                            "MOCK-DATA"
                            (get-data-source crypto-id (get data "type" "crypto")))
              :timestamp (js/Date.now)})
           prices))))

(defn process-data-changes [old-prices new-prices]
  (let [old-by-symbol (into {} (map (fn [asset] [(extract-symbol-key asset) asset]) old-prices))
        new-by-symbol (into {} (map (fn [asset] [(extract-symbol-key asset) asset]) new-prices))]
    (reduce-kv (fn [changes symbol-key new-asset]
                 (let [old-asset (get old-by-symbol symbol-key)
                       old-price (get-in old-asset [:currentPrice :amount])
                       new-price (get-in new-asset [:currentPrice :amount])
                       old-change (get old-asset :priceChange24h)
                       new-change (get new-asset :priceChange24h)
                       old-volume (get-in old-asset [:volume24h :amount])
                       new-volume (get-in new-asset [:volume24h :amount])
                       old-bid (get old-asset :bidPrice)
                       new-bid (get new-asset :bidPrice)
                       old-ask (get old-asset :askPrice)
                       new-ask (get new-asset :askPrice)]
                   (if (or (not= old-price new-price)
                           (not= old-change new-change)
                           (not= old-volume new-volume)
                           (not= old-bid new-bid)
                           (not= old-ask new-ask))
                     (conj changes new-asset)
                     changes)))
               []
               new-by-symbol)))

(defn update-price-data [js-data]
  (let [prices (js->clj js-data :keywordize-keys true)
        old-prices @state/prices-atom
        new-keys (set (map extract-symbol-key prices))
        old-keys (set (map extract-symbol-key old-prices))
        changes (process-data-changes old-prices prices)]

    ;; Single batched update - triggers only one re-render
    (when (seq changes)
      (reset! state/prices-atom prices))

    ;; Only update keys if they actually changed
    (when (not= new-keys old-keys)
      (reset! state/price-keys-atom (map extract-symbol-key prices)))))

(defn handle-fetch-success
  ([data] (handle-fetch-success data false))
  ([data is-fallback]
   ;; Transform current format to standardized format
   (let [js-data (js->clj data :keywordize-keys false)
         standardized-data (transform-to-standardized js-data is-fallback)]
     (update-price-data standardized-data)

     ;; Always update timestamp and flash indicator
     (update-timestamp)
     (flash-update-indicator)
     (hide-fetch-indicator)

     ;; Update loading state only for initial load
     (when (not @state/initial-load-complete)
       (reset! state/initial-load-complete true)
       (when @state/loading-atom
         (reset! state/loading-atom false))))))

(defn handle-fetch-error [error]
  (js/console.error "Failed to fetch crypto data:" error)

  ;; Hide fetch indicator and update error state
  (hide-fetch-indicator)
  (reset! state/loading-atom false)
  (reset! state/error-atom (.-message error)))

(def fallback-data
  {"btc" {"usd" 116662.10 "usd_24h_change" 0.0292 "usd_24h_vol" 237253.722867 "symbol" "BTC-USD" "bid" 116621.34 "ask" 116702.85 "type" "crypto"}
   "eth" {"usd" 4610.40 "usd_24h_change" 2.2179 "usd_24h_vol" 36029.256549 "symbol" "ETH-USD" "bid" 4608.17 "ask" 4612.63 "type" "crypto"}
   "hash" {"usd" 0.038 "usd_24h_change" 0 "usd_24h_vol" 32341.429001 "symbol" "HASH-USD" "bid" 0.038 "ask" 0.039 "type" "crypto"}
   "figr" {"usd" 37.17 "usd_24h_change" -8.176877470355715 "usd_24h_vol" 11794510 "symbol" "FIGR" "bid" nil "ask" nil "type" "stock"}
   "figr_heloc" {"usd" 1.0247849 "usd_24h_change" 3.933 "usd_24h_vol" 73434460.553824964879572253 "symbol" "FIGR_HELOC-USD" "bid" 0 "ask" 0 "type" "crypto"}
   "link" {"usd" 29.4505 "usd_24h_change" 13.4893 "usd_24h_vol" 5298.8267895 "symbol" "LINK-USD" "bid" 29.43 "ask" 29.47 "type" "crypto"}
   "sol" {"usd" 259.60 "usd_24h_change" 8.8433 "usd_24h_vol" 7433.442016 "symbol" "SOL-USD" "bid" 259.51 "ask" 259.69 "type" "crypto"}
   "uni" {"usd" 16.3 "usd_24h_change" 7.8947 "usd_24h_vol" 1655.252826 "symbol" "UNI-USD" "bid" 16.28 "ask" 16.32 "type" "crypto"}
   "xrp" {"usd" 3.3075 "usd_24h_change" 5.8876 "usd_24h_vol" 6568.2468925 "symbol" "XRP-USD" "bid" 3.3065 "ask" 3.3085 "type" "crypto"}})

(defn fetch-crypto-data []
  ;; Show visual feedback
  (show-fetch-indicator)

  ;; Only show loading states for initial load
  (when (not @state/initial-load-complete)
    (when (not @state/loading-atom)
      (reset! state/loading-atom true))
    (when @state/error-atom
      (reset! state/error-atom nil)))

  ;; Try to fetch data from GitHub, fallback to embedded data if CORS fails
  (-> (js/fetch (str "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json?t=" (js/Date.now)))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then handle-fetch-success)
      (.catch (fn [error]
                (js/console.warn "Remote fetch failed, using fallback data:" error)
                ;; Use fallback data when remote fetch fails (e.g., CORS with file://)
                (handle-fetch-success (clj->js fallback-data) true)))))

(defn setup-timeout-handler []
  (js/setTimeout
   (fn []
     (when @state/loading-atom
       (reset! state/loading-atom false)
       (reset! state/error-atom "Timeout loading data")))
   state/TIMEOUT_MS))

(defn fetch-exchange-rates []
  "Fetch exchange rates from data file"
  (js/console.log "💱 Fetching exchange rates...")
  (-> (js/fetch (str "data/exchange-rates.json?t=" (js/Date.now)))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (let [rates-data (js->clj data :keywordize-keys true)
                     rates (:rates rates-data)]
                 (js/console.log "✅ Exchange rates loaded from API:" rates)
                 (reset! state/exchange-rates-atom rates)
                 (reset! state/using-mock-rates-atom false))))
      (.catch (fn [error]
                (js/console.warn "💱 Failed to load exchange rates, using mock data:" error)
                (reset! state/exchange-rates-atom mock-exchange-rates)
                (reset! state/using-mock-rates-atom true)
                (js/console.log "💱 Mock exchange rates loaded:" @state/exchange-rates-atom)))))

(defn start-polling []
  (fetch-exchange-rates)
  (fetch-crypto-data)
  (js/setInterval fetch-crypto-data state/POLL_INTERVAL_MS))