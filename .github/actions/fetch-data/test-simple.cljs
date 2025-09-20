#!/usr/bin/env nbb

(ns test-simple
  (:require ["assert" :as assert]
            [applied-science.js-interop :as j]
            [action :refer [process-crypto-data 
                           valid-price?
                           calculate-change-percent]]))

(defn test-valid-price []
  (assert/ok (valid-price? 100))
  (assert/ok (not (valid-price? 0)))
  (assert/ok (not (valid-price? nil)))
  (println "✅ valid-price? test passed"))

(defn test-calculate-change-percent []
  (assert/strictEqual (calculate-change-percent 110 100) 10.0)
  (assert/strictEqual (calculate-change-percent 90 100) -10.0)
  (assert/strictEqual (calculate-change-percent 100 0) 0)
  (println "✅ calculate-change-percent test passed"))

(defn test-crypto-processing []
  ;; Create JS data structure that matches what the function expects
  (let [js-sample-data (clj->js {:data [{:symbol "BTC-USD"
                                        :midMarketPrice "50000"
                                        :percentageChange24h "0.02"
                                        :volume24h "1000000"
                                        :bestBid "49900"
                                        :bestAsk "50100"
                                        :lastTradedPrice "49950"
                                        :tradeCount24h "100"
                                        :high24h "51000"
                                        :low24h "49000"}]})
        result (process-crypto-data js-sample-data)]
    (println "Debug - result keys:" (keys result))
    (when (:btc result)
      (println "Debug - btc usd:" (:usd (:btc result))))
    (assert/ok (:btc result))
    (assert/strictEqual (:usd (:btc result)) 50000)
    (assert/strictEqual (:usd_24h_change (:btc result)) 2.0)
    (assert/strictEqual (:bid (:btc result)) 49900)
    (println "✅ Crypto processing test passed")))

;; Run only the simple tests
(println "🧪 Running simple unit tests...")
(test-valid-price)
(test-calculate-change-percent)
(test-crypto-processing)
(println "🎉 Simple tests passed!")
