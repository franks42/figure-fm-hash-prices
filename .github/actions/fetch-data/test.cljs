#!/usr/bin/env nbb

(ns test
  (:require ["assert" :as assert]
            [action :refer [process-crypto-data
                            process-stock-data
                            process-alpha-vantage-stock
                            validate-crypto-data
                            validate-output-data
                            valid-price?
                            has-required-fields?
                            calculate-change-percent
                            yahoo-data-valid?]]))

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
  (let [sample-data {:data [{:symbol "BTC-USD"
                             :midMarketPrice "50000"
                             :percentageChange24h "0.02"
                             :volume24h "1000000"
                             :bestBid "49900"
                             :bestAsk "50100"
                             :lastTradedPrice "49950"
                             :tradeCount24h "100"
                             :high24h "51000"
                             :low24h "49000"}]}
        result (process-crypto-data sample-data)]
    (println "Debug - result:" (js/JSON.stringify (clj->js result) nil 2))
    (println "Debug - btc data:" (js/JSON.stringify (clj->js (:btc result)) nil 2))
    (assert/strictEqual (:usd (:btc result)) 50000)
    (assert/strictEqual (:usd_24h_change (:btc result)) 2.0)
    (assert/strictEqual (:bid (:btc result)) 49900)
    (println "✅ Crypto processing test passed")))

(defn test-yahoo-stock-processing []
  (let [sample-data {:chart {:result [{:meta {:regularMarketPrice 40
                                              :previousClose 38
                                              :regularMarketVolume 1000000}}]}}
        result (process-stock-data sample-data)]
    (assert/strictEqual (:usd (:figr result)) 40)
    (assert/strictEqual (Math/round (:usd_24h_change (:figr result))) 5)
    (println "✅ Yahoo stock processing test passed")))

(defn test-alpha-vantage-processing []
  (let [sample-data {"Global Quote" {"05. price" "42.50"
                                     "10. change percent" "3.25%"
                                     "06. volume" "500000"}}
        result (process-alpha-vantage-stock sample-data)]
    (assert/strictEqual (:usd (:figr result)) 42.50)
    (assert/strictEqual (:usd_24h_change (:figr result)) 3.25)
    (assert/strictEqual (:exchange (:figr result)) "Alpha Vantage")
    (println "✅ Alpha Vantage processing test passed")))

(defn test-yahoo-data-validation []
  (let [valid-data {:chart {:result [{:meta {:regularMarketPrice 40}}]}}
        invalid-data {:chart {:result [{}]}}]
    (assert/strictEqual (yahoo-data-valid? valid-data) true)
    (assert/strictEqual (yahoo-data-valid? invalid-data) false)
    (assert/strictEqual (yahoo-data-valid? nil) false)
    (println "✅ Yahoo data validation test passed")))

(defn test-crypto-validation []
  (try
    (validate-crypto-data {:btc {:usd 50000}
                           :eth {:usd 3000}
                           :hash {:usd 0.038}})
    (println "✅ Valid crypto data validation test passed")
    (catch js/Error e
      (assert/fail "Should not have thrown error for valid data"))))

(defn test-crypto-validation-failure []
  (try
    (validate-crypto-data {:btc {:usd 0}})  ; Should fail - missing coins and invalid price
    (assert/fail "Validation should have failed")
    (catch js/Error e
      (println "✅ Invalid crypto data validation test passed"))))

(defn test-output-validation []
  (let [valid-data {:btc {:usd 50000}
                    :eth {:usd 3000}
                    :hash {:usd 0.038}
                    :figr {:usd 40}
                    :sol {:usd 200}
                    :timestamp 1234567890
                    :source "test"}]
    (validate-output-data valid-data)
    (println "✅ Output validation test passed")))

;; Run all tests
(println "🧪 Running enhanced nbb action tests...")
(test-valid-price)
(test-calculate-change-percent)
(test-crypto-processing)
(test-yahoo-stock-processing)
(test-alpha-vantage-processing)
(test-yahoo-data-validation)
(test-crypto-validation)
(test-crypto-validation-failure)
(test-output-validation)
(println "🎉 All tests passed!")
