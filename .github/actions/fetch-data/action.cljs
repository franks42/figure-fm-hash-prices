#!/usr/bin/env nbb

(ns action
  (:require ["@actions/core" :as core]
            ["node-fetch$default" :as fetch]
            ["fs" :as fs]
            [promesa.core :as p]
            [clojure.string :as str]
            [applied-science.js-interop :as j]))

(defn log [& args]
  (apply js/console.log args))

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
                           :type "crypto"}))) {})))

(defn process-stock-data [stock-response]
  (let [meta (j/get-in stock-response [:chart :result 0 :meta])
        current-price (j/get meta :regularMarketPrice)
        previous-close (j/get meta :previousClose)
        change-percent (if (and current-price previous-close)
                         (* (/ (- current-price previous-close) previous-close) 100)
                         0)]
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

(defn main []
  (-> (p/all [(fetch-figure-markets) (fetch-yahoo-finance)])
      (p/then (fn [[crypto-data stock-data]]
                (log "🚀 Starting crypto data fetch action...")
                (core/info "Action started successfully")

                (let [processed-crypto (process-crypto-data crypto-data)
                      processed-stock (process-stock-data stock-data)
                      timestamp (.getTime (js/Date.))
                      combined-data (merge processed-crypto
                                           processed-stock
                                           {:timestamp (/ timestamp 1000)
                                            :source "figuremarkets+yahoo"
                                            :last_update (.toISOString (js/Date.))})]

                  ;; Ensure data directory exists
                  (when-not (fs/existsSync "../../../data")
                    (fs/mkdirSync "../../../data" (clj->js {:recursive true})))

                  ;; Write JSON file
                  (fs/writeFileSync "../../../data/crypto-prices.json"
                                    (js/JSON.stringify (clj->js combined-data) nil 2))

                  (log "📊 Processed" (count (keys processed-crypto)) "crypto currencies")
                  (log "📊 FIGR stock price:" (get-in combined-data [:figr :usd]))
                  (log "💾 Data written to data/crypto-prices.json")
                  (core/setOutput "data-file" "data/crypto-prices.json")
                  (log "✅ Action completed"))))))

;; Run main when script is executed directly
(-> (main)
    (p/catch (fn [error]
               (core/setFailed (str "Action failed: " error))
               (js/process.exit 1))))