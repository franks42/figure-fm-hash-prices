#!/usr/bin/env nbb

(ns demo-mapping
  "Demo script to show how the JSON mapping configuration works"
  (:require ["fs" :as fs]
            [clojure.pprint :as pprint]))

(defn load-config []
  "Load the field mapping configuration"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj))

(defn demo-field-mappings []
  "Demonstrate how field mappings work"
  (let [config (load-config)
        field-mappings (get config "fieldMappings")]

    (println "🗂️  API Field Mapping Configuration Demo")
    (println "======================================\n")

    (println "📋 Available API mappings:")
    (println "- figureMarkets: Figure Markets API")
    (println "- yahooFinance: Yahoo Finance API")
    (println "- coinGecko: CoinGecko API")
    (println "- alphaVantage: Alpha Vantage API")
    (println "- freeCurrencyApi: FreeCurrencyAPI")
    (println "- exchangeRateHost: ExchangeRate.host")
    (println "- fixerIo: Fixer.io\n")

    (println "🏷️  Sample Field Mappings:")
    (println "========================\n")

    ;; Show some key field examples
    (let [pricing-fields (get-in field-mappings ["pricingData" "fields"])]
      (println "💰 Current Price Field:")
      (pprint/pprint (select-keys (get pricing-fields "currentPrice") ["description" "type" "mappings"]))
      (println))

    (let [stats-fields (get-in field-mappings ["statistics24h" "fields"])]
      (println "📊 24h Volume Field:")
      (pprint/pprint (select-keys (get stats-fields "volume24h") ["description" "type" "mappings"]))
      (println))

    (println "🔄 Example Mapping Usage:")
    (println "=========================")
    (println "To get currentPrice from Figure Markets API:")
    (println "  API field: 'midMarketPrice' → Standard field: 'currentPrice'")
    (println "To get currentPrice from CoinGecko API:")
    (println "  API field: 'current_price' → Standard field: 'currentPrice'")
    (println "To get currentPrice from Yahoo Finance API:")
    (println "  API field: 'regularMarketPrice' → Standard field: 'currentPrice'\n")))

(defn demo-sample-mapping []
  "Show a sample API response mapping"
  (println "📝 Sample API Response Mapping:")
  (println "==============================\n")

  (let [sample-figma-response {"id" "btc-usd"
                               "symbol" "BTCUSD"
                               "displayName" "Bitcoin/USD"
                               "midMarketPrice" "45000.50"
                               "volume24h" "1500000"}

        sample-coingecko-response {"id" "bitcoin"
                                   "symbol" "btc"
                                   "name" "Bitcoin"
                                   "current_price" 45000.50
                                   "total_volume" 25000000000}]

    (println "🏢 Figure Markets API Response:")
    (pprint/pprint sample-figma-response)
    (println "   Maps to standardized:")
    (println "   - id → assetId")
    (println "   - midMarketPrice → currentPrice")
    (println "   - volume24h → volume24h\n")

    (println "🥇 CoinGecko API Response:")
    (pprint/pprint sample-coingecko-response)
    (println "   Maps to standardized:")
    (println "   - id → assetId")
    (println "   - current_price → currentPrice")
    (println "   - total_volume → volume24h\n")))

(defn main []
  "Run the demo"
  (demo-field-mappings)
  (demo-sample-mapping)
  (println "✅ Demo completed! The JSON config provides a single source of truth for all API field mappings."))

;; Run the demo
(main)