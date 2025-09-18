#!/usr/bin/env nbb

(ns test-standardized-mapping
  "Test the standardized field mapping without API calls"
  (:require ["fs" :as fs]))

(defn load-field-mappings []
  "Load the standardized field mapping configuration"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj))

(defn get-all-source-mappings [field-mappings source-key]
  "Get all field mappings for a specific source, including complex mappings"
  (let [mappings (atom {})]
    (doseq [[category-key category-data] (get field-mappings "fieldMappings")]
      (doseq [[field-name field-data] (get category-data "fields")]
        (let [source-mapping (get-in field-data ["mappings" source-key])]
          (cond
            ;; Simple string mapping
            (string? source-mapping)
            (swap! mappings assoc source-mapping field-name)

            ;; Complex object mapping (currencyAmount/tokenAmount)
            (map? source-mapping)
            (do
              (when-let [amount-field (get source-mapping "amount")]
                (swap! mappings assoc amount-field (str field-name ".amount")))
              (when-let [currency-field (get source-mapping "currency")]
                (swap! mappings assoc currency-field (str field-name ".currency"))))))))
    @mappings))

(defn find-unmapped-fields [raw-data source-mappings]
  "Find fields in raw data that don't have mappings"
  (let [mapped-fields (set (keys source-mappings))
        raw-fields (set (keys raw-data))]
    (remove mapped-fields raw-fields)))

(defn convert-to-currency-amount [amount currency]
  "Convert amount and currency to currencyAmount structure"
  (when (and amount currency (not= amount "") (not= currency ""))
    {"amount" (if (string? amount) (js/parseFloat amount) amount)
     "currency" (str currency)}))

(defn test-field-mapping []
  "Test field mapping with sample Figure Markets data"
  (println "🧪 Testing Standardized Field Mapping")
  (println "====================================")

  (let [field-mappings (load-field-mappings)
        source-mappings (get-all-source-mappings field-mappings "figureMarkets")

        ;; Sample Figure Markets data structure
        sample-raw-data {"symbol" "BTC-USD"
                         "displayName" "Bitcoin USD"
                         "denom" "BTC"
                         "quoteDenom" "USD"
                         "midMarketPrice" "45000.50"
                         "bestBid" "44999.00"
                         "bestAsk" "45001.00"
                         "volume24h" "1500000000"
                         "baseVolume24h" "33333"
                         "priceChange24h" "1250.30"
                         "percentageChange24h" "2.85"
                         "high24h" "45500.00"
                         "low24h" "44200.00"
                         "tradeCount24h" "125000"
                         "marketType" "crypto"
                         "proTradeAvailable" true
                         "unknownField1" "someValue"
                         "unknownField2" "anotherValue"}

        unmapped-fields (find-unmapped-fields sample-raw-data source-mappings)
        mapped-data (atom {})]

    (println "\n📊 **Source Mappings Found:**" (count source-mappings))
    (println "\n🔍 **Sample Raw Data Fields:**" (count (keys sample-raw-data)))

    ;; Map known fields
    (doseq [[raw-field standard-field] source-mappings]
      (when-let [raw-value (get sample-raw-data raw-field)]
        (swap! mapped-data assoc standard-field raw-value)))

    (println "\n✅ **Successfully Mapped Fields:**" (count @mapped-data))
    (doseq [[standard-field value] @mapped-data]
      (println (str "  " standard-field ": " value)))

    (println "\n⚠️  **Unmapped Fields:**" (count unmapped-fields))
    (doseq [field unmapped-fields]
      (println (str "  - " field ": " (get sample-raw-data field))))

    ;; Test currencyAmount conversion
    (let [base-data @mapped-data
          current-price (convert-to-currency-amount
                         (get base-data "midMarketPrice")
                         (get base-data "quoteDenom"))
          volume24h (convert-to-currency-amount
                     (get base-data "volume24h")
                     (get base-data "quoteDenom"))]

      (println "\n💰 **currencyAmount Conversion Test:**")
      (println "  currentPrice:" (.stringify js/JSON (clj->js current-price)))
      (println "  volume24h:" (.stringify js/JSON (clj->js volume24h))))

    (println "\n📋 **Mapping Summary:**")
    (println (str "  Total raw fields: " (count (keys sample-raw-data))))
    (println (str "  Successfully mapped: " (count @mapped-data)))
    (println (str "  Unmapped fields: " (count unmapped-fields)))
    (println (str "  Mapping coverage: " (.toFixed (* (/ (count @mapped-data) (count (keys sample-raw-data))) 100) 1) "%"))

    (if (> (count unmapped-fields) 0)
      (println "\n🔧 **Action Required:** Add unmapped fields to api-field-mapping.json")
      (println "\n🎉 **Perfect!** All fields successfully mapped"))))

(defn main []
  "Run the mapping test"
  (test-field-mapping))

;; Run the test
(main)