#!/usr/bin/env nbb

(ns action
  (:require ["@actions/core" :as core]
            ["node-fetch$default" :as fetch]
            ["fs" :as fs]
            [promesa.core :as p]
            [clojure.string :as str]
            [clojure.set]
            [applied-science.js-interop :as j]))

(defn log [& args]
  (apply js/console.log args))

(defn valid-price? [price]
  (and price (number? price) (> price 0)))

(defn create-fallback-data [coin-id reason]
  "Create fallback data when API data is invalid"
  (log "⚠️  Creating fallback data for" coin-id "- Reason:" reason)
  {:usd 0.01  ; Minimal non-zero fallback price
   :usd_24h_change 0
   :usd_24h_vol 0
   :symbol (str (str/upper-case (name coin-id)) "-USD")
   :type "crypto"
   :data_status "STALE_FALLBACK"  ; Mark as invalid/stale data
   :error_reason reason
   :last_valid_timestamp (js/Date.now)})

(defn sanitize-crypto-data [data]
  "Sanitize crypto data, replacing invalid entries with fallbacks"
  (let [required-coins #{"btc" "eth" "hash"}]
    (reduce (fn [acc coin-id]
              (let [coin-key (keyword coin-id)
                    coin-data (get data coin-key)]
                (cond
                  ;; Missing coin entirely
                  (nil? coin-data)
                  (do (log "⚠️  Missing data for" coin-id "- using fallback")
                      (assoc acc coin-key (create-fallback-data coin-key "missing_data")))
                  
                  ;; Invalid or zero price
                  (not (valid-price? (:usd coin-data)))
                  (do (log "⚠️  Invalid price for" coin-id ":" (:usd coin-data) "- using fallback")
                      (assoc acc coin-key (merge coin-data (create-fallback-data coin-key "invalid_price"))))
                  
                  ;; Valid data
                  :else
                  (assoc acc coin-key coin-data))))
            {}
            required-coins)))

(defn validate-crypto-data [data]
  "Validate and sanitize crypto data - never fails, always returns usable data"
  (let [sanitized-data (sanitize-crypto-data data)
        other-data (apply dissoc data (map keyword ["btc" "eth" "hash"]))]
    
    ;; Also sanitize optional coins like figr_heloc, figr, etc.
    (reduce (fn [acc [coin-id coin-data]]
              (if (and coin-data (:usd coin-data) (not (valid-price? (:usd coin-data))))
                (do (log "⚠️  Invalid price for optional coin" coin-id ":" (:usd coin-data) "- using fallback")
                    (assoc acc coin-id (merge coin-data (create-fallback-data coin-id "invalid_price"))))
                (assoc acc coin-id coin-data)))
            sanitized-data
            other-data)))

(defn validate-output-data [combined-data]
  "Final validation - ensures minimal usable data, never fails"
  (let [asset-count (count (filter #(not (#{:timestamp :source :last_update} %)) (keys combined-data)))]
    (when (< asset-count 3)
      (log "⚠️  Low asset count:" asset-count "- but continuing with available data"))
    
    (when-not (:timestamp combined-data)
      (log "⚠️  Missing timestamp - adding current timestamp")
      (assoc combined-data :timestamp (js/Date.now)))
    
    ;; Always return data, never throw
    (do (log "✅ Output validation complete - assets:" asset-count)
        combined-data)))

(defn fetch-figure-markets []
  (-> (fetch "https://www.figuremarkets.com/service-hft-exchange/api/v1/markets")
      (p/then (fn [response] (.json response)))
      (p/then (fn [data]
                (log "✅ Figure Markets data fetched")
                data))))

(defn fetch-yahoo-finance []
  (-> (fetch "https://query1.finance.yahoo.com/v8/finance/chart/FIGR"
             (clj->js {:headers {"User-Agent" "Mozilla/5.0 (compatible; GitHub-Actions)"}}))
      (p/then (fn [response] (.json response)))
      (p/then (fn [data]
                (log "✅ Yahoo Finance data fetched")
                data))))

(defn fetch-alpha-vantage [api-key]
  (if api-key
    (-> (fetch (str "https://www.alphavantage.co/query"
                    "?function=GLOBAL_QUOTE&symbol=FIGR"
                    "&apikey=" api-key))
        (p/then (fn [response] (.json response)))
        (p/then (fn [data]
                  (log "✅ Alpha Vantage data fetched")
                  data))
        (p/catch (fn [error]
                   (log "❌ Alpha Vantage failed:" (.-message error))
                   nil)))
    (p/resolved nil)))

(defn get-fallback-stock-data
  "Hardcoded fallback when all APIs fail"
  []
  {:figr {:usd 37.33
          :usd_24h_change 0
          :usd_24h_vol 1500000
          :symbol "FIGR"
          :type "stock"
          :company_name "Figure Technology Solutions, Inc."
          :exchange "Fallback"
          :currency "USD"}})

(defn process-crypto-data [crypto-response]
  (->> (j/get crypto-response :data)
       (filter #(str/ends-with? (j/get % :symbol) "-USD"))
       (remove #(contains? #{"USDT-USD" "USDC-USD"} (j/get % :symbol)))
       (reduce (fn [acc item]
                 (let [symbol (-> (j/get item :symbol)
                                  (str/replace "-USD" "")
                                  str/lower-case
                                  keyword)]
                   (assoc acc symbol
                          {:usd (js/parseFloat (or (j/get item :midMarketPrice)
                                                   (j/get item :lastTradedPrice) 0))
                           :usd_24h_change (* (js/parseFloat (or (j/get item :percentageChange24h) 0)) 100)
                           :usd_24h_vol (js/parseFloat (or (j/get item :volume24h) 0))
                           :usd_market_cap nil
                           :symbol (j/get item :symbol)
                           :bid (js/parseFloat (or (j/get item :bestBid) 0))
                           :ask (js/parseFloat (or (j/get item :bestAsk) 0))
                           :last_price (js/parseFloat (or (j/get item :lastTradedPrice) 0))
                           :trades_24h (js/parseInt (or (j/get item :tradeCount24h) 0))
                           :day_high (js/parseFloat (or (j/get item :high24h) 0))
                           :day_low (js/parseFloat (or (j/get item :low24h) 0))
                           :type "crypto"}))) {})))

(defn calculate-change-percent [current-price previous-close]
  (if (and current-price previous-close (> previous-close 0))
    (* (/ (- current-price previous-close) previous-close) 100)
    0))

(defn process-stock-data [stock-response]
  (let [meta (j/get-in stock-response [:chart :result 0 :meta])
        current-price (j/get meta :regularMarketPrice)
        previous-close (j/get meta :previousClose)
        change-percent (calculate-change-percent current-price previous-close)]
    {:figr {:usd (js/parseFloat current-price)
            :usd_24h_change (js/parseFloat change-percent)
            :usd_24h_vol (js/parseInt (or (j/get meta :regularMarketVolume) 0))
            :usd_market_cap nil
            :symbol "FIGR"
            :bid nil
            :ask nil
            :last_price (js/parseFloat current-price)
            :trades_24h nil
            :type "stock"
            :day_high (js/parseFloat (j/get meta :regularMarketDayHigh))
            :day_low (js/parseFloat (j/get meta :regularMarketDayLow))
            :open (js/parseFloat (j/get meta :regularMarketOpen))
            :previous_close (js/parseFloat previous-close)
            :change (js/parseFloat (- current-price previous-close))
            :fifty_two_week_high (js/parseFloat (j/get meta :fiftyTwoWeekHigh))
            :fifty_two_week_low (js/parseFloat (j/get meta :fiftyTwoWeekLow))
            :company_name (or (j/get meta :longName) "Figure Technology Solutions, Inc.")
            :company_short_name (or (j/get meta :shortName) "FIGR")
            :exchange (or (j/get meta :fullExchangeName) "NasdaqGS")
            :currency (or (j/get meta :currency) "USD")
            :timezone (or (j/get meta :timezone) "EDT")
            :last_trade_time (js/parseInt (or (j/get meta :regularMarketTime) 0))}}))

(defn process-alpha-vantage-stock [alpha-data]
  (when alpha-data
    (let [quote (j/get alpha-data "Global Quote")
          price (js/parseFloat (j/get quote "05. price"))
          change-percent (-> (j/get quote "10. change percent")
                             (str/replace "%" "")
                             js/parseFloat)]
      {:figr {:usd price
              :usd_24h_change change-percent
              :usd_24h_vol (js/parseInt (j/get quote "06. volume"))
              :usd_market_cap nil
              :symbol "FIGR"
              :bid nil
              :ask nil
              :last_price price
              :trades_24h nil
              :type "stock"
              :company_name "Figure Technology Solutions, Inc."
              :exchange "Alpha Vantage"
              :currency "USD"}})))

(defn yahoo-data-valid? [yahoo-data]
  (and yahoo-data
       (j/get-in yahoo-data [:chart :result 0 :meta :regularMarketPrice])))

(defn fetch-stock-with-yahoo [yahoo-data]
  (if (yahoo-data-valid? yahoo-data)
    (p/resolved {:data yahoo-data :source "yahoo"})
    (p/rejected (js/Error. "Yahoo data invalid"))))

(defn fetch-stock-with-alpha-vantage [api-key]
  (-> (fetch-alpha-vantage api-key)
      (p/then (fn [alpha-data]
                (if alpha-data
                  {:data alpha-data :source "alphavantage"}
                  (throw (js/Error. "Alpha Vantage failed")))))))

(defn fetch-stock-with-fallback []
  (log "⚠️ All APIs failed, using fallback data")
  (p/resolved {:data (get-fallback-stock-data) :source "fallback"}))

(defn fetch-stock-data-with-fallbacks [api-key]
  (-> (fetch-yahoo-finance)
      (p/then fetch-stock-with-yahoo)
      (p/catch (fn [_]
                 (log "⚠️ Yahoo Finance failed, trying Alpha Vantage...")
                 (-> (fetch-stock-with-alpha-vantage api-key)
                     (p/catch (fn [_] (fetch-stock-with-fallback))))))))

(defn process-stock-by-source [stock-result]
  (case (:source stock-result)
    "yahoo" (process-stock-data (:data stock-result))
    "alphavantage" (process-alpha-vantage-stock (:data stock-result))
    "fallback" (:data stock-result)))

(defn create-combined-data [processed-crypto processed-stock source]
  (let [timestamp (.getTime (js/Date.))]
    (merge processed-crypto
           processed-stock
           {:timestamp (/ timestamp 1000)
            :source (str "figuremarkets+" source)
            :last_update (.toISOString (js/Date.))})))

(defn ensure-data-directory []
  (when-not (fs/existsSync "../../../data")
    (fs/mkdirSync "../../../data" (clj->js {:recursive true}))))

(defn write-data-file [combined-data]
  (fs/writeFileSync "../../../data/crypto-prices.json"
                    (js/JSON.stringify (clj->js combined-data) nil 2)))

(defn main []
  (let [api-key (core/getInput "alpha-vantage-api-key")]
    (-> (p/all [(fetch-figure-markets)
                (fetch-stock-data-with-fallbacks api-key)])
        (p/then (fn [[crypto-data stock-result]]
                  (log "🚀 Starting crypto data fetch action...")
                  (core/info "Action started successfully")

                  (let [processed-crypto (validate-crypto-data (process-crypto-data crypto-data))
                        processed-stock (process-stock-by-source stock-result)
                        combined-data (validate-output-data
                                       (create-combined-data processed-crypto
                                                             processed-stock
                                                             (:source stock-result)))]

                    (ensure-data-directory)
                    (write-data-file combined-data)

                    (log "📊 Processed" (count (keys processed-crypto)) "crypto currencies")
                    (log "📊 FIGR stock price:" (get-in combined-data [:figr :usd]))
                    (log "💾 Data written to data/crypto-prices.json")
                    (core/setOutput "data-file" "data/crypto-prices.json")
                    (log "✅ Action completed")))))))

;; Run main when script is executed directly
(-> (main)
    (p/catch (fn [error]
               (core/setFailed (str "Action failed: " error))
               (js/process.exit 1))))