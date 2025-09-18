#!/usr/bin/env nbb

(ns test-field-mapping
  "Test script to demonstrate field mapping functionality"
  (:require ["./field-mapper.cljs" :as mapper]
            [clojure.pprint :as pprint]))

(defn test-figure-markets-mapping []
  "Test Figure Markets API response mapping"
  (println "\n=== Testing Figure Markets Mapping ===")
  (let [sample-response {"id" "btc-usd"
                         "symbol" "BTCUSD"
                         "displayName" "Bitcoin/USD"
                         "denom" "BTC"
                         "quoteDenom" "USD"
                         "marketType" "crypto"
                         "midMarketPrice" "45000.50"
                         "bestBid" "44999.25"
                         "bestAsk" "45001.75"
                         "volume24h" "1500000"
                         "high24h" "46000"
                         "low24h" "44500"
                         "priceChange24h" "500.50"
                         "percentageChange24h" "1.12"}
        mapped (mapper/map-figure-markets-response sample-response)]
    (println "Original Figure Markets response:")
    (pprint/pprint sample-response)
    (println "\nMapped to standardized format:")
    (pprint/pprint mapped)))

(defn test-coingecko-mapping []
  "Test CoinGecko API response mapping"
  (println "\n=== Testing CoinGecko Mapping ===")
  (let [sample-response {"id" "bitcoin"
                         "symbol" "btc"
                         "name" "Bitcoin"
                         "current_price" 45000.50
                         "market_cap" 850000000000
                         "total_volume" 25000000000
                         "high_24h" 46000
                         "low_24h" 44500
                         "price_change_24h" 500.50
                         "price_change_percentage_24h" 1.12
                         "last_updated" "2025-09-17T10:30:00.000Z"}
        mapped (mapper/map-coingecko-response sample-response)]
    (println "Original CoinGecko response:")
    (pprint/pprint sample-response)
    (println "\nMapped to standardized format:")
    (pprint/pprint mapped)))

(defn test-exchange-rate-mapping []
  "Test exchange rate API response mapping"
  (println "\n=== Testing Exchange Rate Mapping ===")
  (let [freecurrency-response {"date" "2025-09-17"
                               "base_currency" "USD"
                               "data" {"EUR" 0.85
                                       "JPY" 110.25
                                       "GBP" 0.73
                                       "CAD" 1.25}}
        exchangerate-host-response {"base" "USD"
                                    "date" "2025-09-17"
                                    "timestamp" 1726574400
                                    "success" true
                                    "rates" {"EUR" 0.85
                                             "JPY" 110.25
                                             "GBP" 0.73
                                             "CAD" 1.25}}
        mapped-free (mapper/map-exchange-rate-response freecurrency-response "freeCurrencyApi")
        mapped-host (mapper/map-exchange-rate-response exchangerate-host-response "exchangeRateHost")]

    (println "Original FreeCurrencyAPI response:")
    (pprint/pprint freecurrency-response)
    (println "\nMapped FreeCurrencyAPI to standardized format:")
    (pprint/pprint mapped-free)

    (println "\nOriginal ExchangeRate.host response:")
    (pprint/pprint exchangerate-host-response)
    (println "\nMapped ExchangeRate.host to standardized format:")
    (pprint/pprint mapped-host)))

(defn test-field-lookup []
  "Test individual field mapping lookup"
  (println "\n=== Testing Field Lookup ===")
  (println "currentPrice mapping for figureMarkets:" (mapper/get-field-mapping "currentPrice" "figureMarkets"))
  (println "volume24h mapping for coinGecko:" (mapper/get-field-mapping "volume24h" "coinGecko"))
  (println "marketCap mapping for yahooFinance:" (mapper/get-field-mapping "marketCap" "yahooFinance")))

(defn main []
  "Run all tests"
  (println "🧪 Testing API Field Mapping Utilities")
  (println "=====================================")

  (test-figure-markets-mapping)
  (test-coingecko-mapping)
  (test-exchange-rate-mapping)
  (test-field-lookup)

  (println "\n✅ All tests completed!"))

;; Run tests
(main)