#!/usr/bin/env nbb

(ns test-mapping-compatibility
  "Test if our new JSON mapping produces the same results as current tracker data"
  (:require ["fs" :as fs]
            [clojure.pprint :as pprint]))

(defn load-current-data []
  "Load the current crypto-prices.json data"
  (-> (fs/readFileSync "data/crypto-prices.json" "utf8")
      js/JSON.parse
      js->clj))

(defn load-mapping-config []
  "Load our new field mapping configuration"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj))

(defn get-current-internal-mappings []
  "Extract current internal field mappings from our JSON config"
  (let [config (load-mapping-config)
        field-mappings (get config "fieldMappings")]
    (reduce (fn [acc [category-key category-data]]
              (let [fields (get category-data "fields")]
                (merge acc
                       (reduce (fn [field-acc [field-name field-data]]
                                 (if-let [current-field (get-in field-data ["mappings" "currentInternal"])]
                                   (assoc field-acc current-field field-name)
                                   field-acc))
                               {}
                               fields))))
            {}
            field-mappings)))

(defn map-current-data-to-standard [current-data]
  "Map current data structure to our new standardized format"
  (let [internal-mappings (get-current-internal-mappings)]
    (reduce (fn [acc [asset-key asset-data]]
              (let [mapped-asset (reduce (fn [asset-acc [current-field value]]
                                           (if-let [standard-field (get internal-mappings current-field)]
                                             (assoc asset-acc standard-field value)
                                             asset-acc))
                                         {}
                                         (seq asset-data))]  ; Convert to seq to avoid ISeqable error
                (assoc acc asset-key mapped-asset)))
            {}
            current-data)))

(defn compare-field-mappings []
  "Compare current field names with our standardized mappings"
  (let [current-data (load-current-data)
        internal-mappings (get-current-internal-mappings)
        sample-asset (first (vals current-data))]

    (println "🔍 **Current Data Structure Analysis**")
    (println "====================================\n")

    (println "📊 **Sample Asset (first one):**")
    (pprint/pprint sample-asset)
    (println)

    (println "🗺️  **Field Mapping Analysis:**")
    (println "==============================")
    (println "Current Field → Standardized Field")
    (println "-----------------------------------")

    (doseq [[current-field standard-field] internal-mappings]
      (println (str current-field " → " standard-field)))

    (println)
    (println "❌ **Current Fields NOT in Our Mapping:**")
    (let [current-fields (set (keys sample-asset))
          mapped-fields (set (keys internal-mappings))
          unmapped-fields (clojure.set/difference current-fields mapped-fields)]
      (if (empty? unmapped-fields)
        (println "   ✅ All current fields are mapped!")
        (doseq [field unmapped-fields]
          (println (str "   - " field)))))

    (println)))

(defn test-data-conversion []
  "Test converting sample data to new format"
  (let [current-data (load-current-data)
        mapped-data (map-current-data-to-standard current-data)
        sample-key (first (keys current-data))
        original (get current-data sample-key)
        converted (get mapped-data sample-key)]

    (println "🧪 **Data Conversion Test**")
    (println "===========================\n")

    (println (str "📝 **Sample Asset: " sample-key "**"))
    (println)

    (println "🟦 **Original Format:**")
    (pprint/pprint original)
    (println)

    (println "🟩 **Converted to Standardized Format:**")
    (pprint/pprint converted)
    (println)

    (println "🔄 **Field-by-Field Comparison:**")
    (println "=================================")
    (let [internal-mappings (get-current-internal-mappings)]
      (doseq [[current-field standard-field] internal-mappings]
        (let [original-value (get original current-field)
              converted-value (get converted standard-field)]
          (if (= original-value converted-value)
            (println (str "✅ " current-field " (" original-value ") → " standard-field " (" converted-value ")"))
            (println (str "❌ " current-field " (" original-value ") → " standard-field " (" converted-value ") MISMATCH!"))))))

    (println)))

(defn test-currency-amount-conversion []
  "Test how we would convert to the new currencyAmount structure"
  (let [current-data (load-current-data)
        sample-key (first (keys current-data))
        sample-asset (get current-data sample-key)]

    (println "💰 **Currency Amount Structure Test**")
    (println "====================================\n")

    (println (str "📝 **Sample Asset: " sample-key "**"))
    (println)

    (println "🟦 **Current Price Fields:**")
    (println (str "  usd: " (get sample-asset "usd")))
    (println (str "  last_price: " (get sample-asset "last_price")))
    (println (str "  bid: " (get sample-asset "bid")))
    (println (str "  ask: " (get sample-asset "ask")))
    (println)

    (println "🟩 **New CurrencyAmount Structure:**")
    (println "  currentPrice: {")
    (println (str "    amount: " (get sample-asset "usd")))
    (println "    currency: \"USD\"")
    (println "  }")
    (println "  lastTradePrice: {")
    (println (str "    amount: " (get sample-asset "last_price")))
    (println "    currency: \"USD\"")
    (println "  }")
    (println "  bidPrice: {")
    (println (str "    amount: " (get sample-asset "bid")))
    (println "    currency: \"USD\"")
    (println "  }")
    (println "  askPrice: {")
    (println (str "    amount: " (get sample-asset "ask")))
    (println "    currency: \"USD\"")
    (println "  }")
    (println)

    (println "📊 **Volume Fields:**")
    (println (str "  usd_24h_vol: " (get sample-asset "usd_24h_vol")))
    (println)
    (println "🟩 **New Structure:**")
    (println "  volume24h: {")
    (println (str "    amount: " (get sample-asset "usd_24h_vol")))
    (println "    currency: \"USD\"")
    (println "  }")
    (println)))

(defn validate-all-assets []
  "Validate that all assets can be properly converted"
  (let [current-data (load-current-data)
        mapped-data (map-current-data-to-standard current-data)
        total-assets (count current-data)
        successful-conversions (count mapped-data)]

    (println "✅ **Validation Summary**")
    (println "========================\n")

    (println (str "📊 Total assets in current data: " total-assets))
    (println (str "✅ Successfully mapped assets: " successful-conversions))
    (println (str "🎯 Success rate: " (if (> total-assets 0)
                                         (str (Math/round (* 100 (/ successful-conversions total-assets))) "%")
                                         "N/A")))

    (if (= total-assets successful-conversions)
      (println "\n🎉 All assets can be successfully converted to the new format!")
      (println "\n⚠️ Some assets may have mapping issues."))

    (println)))

(defn main []
  "Run all compatibility tests"
  (println "🧪 **API Field Mapping Compatibility Test**")
  (println "==========================================\n")

  (println "Testing if our new JSON mapping configuration produces")
  (println "the same results as the current crypto tracker data.\n")

  (compare-field-mappings)
  (test-data-conversion)
  (test-currency-amount-conversion)
  (validate-all-assets)

  (println "✅ **Compatibility test completed!**")
  (println "\nNext steps:")
  (println "1. Review any unmapped fields and add them to api-field-mapping.json")
  (println "2. Implement currencyAmount conversion in the data processing pipeline")
  (println "3. Update frontend to use the new standardized field names"))

;; Run the tests
(main)