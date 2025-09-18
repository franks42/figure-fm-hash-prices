#!/usr/bin/env nbb

(ns standardized-field-mapper
  "Advanced field mapping utility with unmapped field detection"
  (:require ["fs" :as fs]
            [clojure.string :as str]))

(defn load-field-mappings []
  "Load the standardized field mapping configuration"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj))

(defn get-all-source-mappings [field-mappings source-key]
  "Get all field mappings for a specific source"
  (let [mappings (atom {})]
    (doseq [[category-key category-data] (get field-mappings "fieldMappings")]
      (doseq [[field-name field-data] (get category-data "fields")]
        (when-let [source-mapping (get-in field-data ["mappings" source-key])]
          (when source-mapping
            (swap! mappings assoc source-mapping field-name)))))
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

(defn convert-to-token-amount [amount token]
  "Convert amount and token to tokenAmount structure"
  (when (and amount token (not= amount "") (not= token ""))
    {"amount" (if (string? amount) (js/parseFloat amount) amount)
     "currency" (str token)}))

(defn apply-field-mapping [raw-data field-mappings source-key]
  "Apply field mappings to raw data"
  (let [source-mappings (get-all-source-mappings field-mappings source-key)
        unmapped-fields (find-unmapped-fields raw-data source-mappings)
        mapped-data (atom {})]

    ;; Map known fields
    (doseq [[raw-field standard-field] source-mappings]
      (when-let [raw-value (get raw-data raw-field)]
        (swap! mapped-data assoc standard-field raw-value)))

    ;; Handle special currencyAmount and tokenAmount conversions
    (let [base-data @mapped-data
          enhanced-data (merge base-data
                               {;; currencyAmount conversions for Figure Markets
                                "currentPrice" (when (= source-key "figureMarkets")
                                                 (convert-to-currency-amount
                                                  (get base-data "midMarketPrice")
                                                  (get base-data "quoteDenom")))
                                "volume24h" (when (= source-key "figureMarkets")
                                              (convert-to-currency-amount
                                               (get base-data "volume24h")
                                               (get base-data "quoteDenom")))
                                "baseVolume24h" (when (= source-key "figureMarkets")
                                                  (convert-to-token-amount
                                                   (get base-data "baseVolume24h")
                                                   (get base-data "denom")))
                                "marketCap" (when (and (= source-key "figureMarkets")
                                                       (get base-data "marketCap"))
                                              (convert-to-currency-amount
                                               (get base-data "marketCap")
                                               (get base-data "quoteDenom")))

                               ;; Add metadata
                                "dataSource" (case source-key
                                               "figureMarkets" "Figure Markets"
                                               "yahooFinance" "Yahoo Finance"
                                               "coinGecko" "CoinGecko"
                                               "alphaVantage" "Alpha Vantage"
                                               "Unknown")
                                "timestamp" (.getTime (js/Date.))
                                "lastUpdate" (.toISOString (js/Date.))})]

      ;; Return both mapped data and unmapped fields
      {:mapped-data (into {} (filter #(some? (second %)) enhanced-data))
       :unmapped-fields unmapped-fields})))

(defn log-unmapped-fields [unmapped-fields source asset-symbol]
  "Log unmapped fields with context"
  (when (seq unmapped-fields)
    (let [timestamp (.toISOString (js/Date.))
          log-entry (str "\n[" timestamp "] " source " (" asset-symbol ") - Unmapped fields:\n"
                         (str/join "\n" (map #(str "  - " %) unmapped-fields))
                         "\n")]

      ;; Ensure data directory exists
      (when-not (fs/existsSync "data")
        (fs/mkdirSync "data"))

      ;; Append to log file
      (fs/appendFileSync "data/unmapped-fields.log" log-entry)
      (println "⚠️  Logged" (count unmapped-fields) "unmapped fields for" asset-symbol))))

(defn map-asset-data [raw-asset source-key field-mappings]
  "Map a single asset's data to standardized format"
  (let [{:keys [mapped-data unmapped-fields]} (apply-field-mapping raw-asset field-mappings source-key)
        asset-symbol (or (get mapped-data "symbol") "UNKNOWN")]

    ;; Log unmapped fields
    (log-unmapped-fields unmapped-fields source-key asset-symbol)

    ;; Return mapped data
    mapped-data))

(defn create-mapping-report [source-key total-assets total-unmapped-fields]
  "Create a mapping report"
  (let [report {"timestamp" (.toISOString (js/Date.))
                "source" source-key
                "totalAssets" total-assets
                "totalUnmappedFields" total-unmapped-fields
                "mappingCoverage" (if (> total-unmapped-fields 0)
                                    "Partial - Check unmapped-fields.log"
                                    "Complete")
                "recommendation" (if (> total-unmapped-fields 0)
                                   "Update api-field-mapping.json with new field mappings"
                                   "All fields successfully mapped")}]

    ;; Ensure data directory exists
    (when-not (fs/existsSync "data")
      (fs/mkdirSync "data"))

    (fs/writeFileSync "data/mapping-report.json"
                      (.stringify js/JSON (clj->js report) nil 2))
    report))

;; Export functions for use in other scripts
(set! js/exports #js {:loadFieldMappings load-field-mappings
                      :mapAssetData map-asset-data
                      :createMappingReport create-mapping-report
                      :logUnmappedFields log-unmapped-fields})

;; Demo function if run directly
(defn demo []
  "Demo the field mapping functionality"
  (let [field-mappings (load-field-mappings)
        sample-raw-data {"symbol" "BTC-USD"
                         "midMarketPrice" "45000.50"
                         "quoteDenom" "USD"
                         "volume24h" "1500000000"
                         "unknownField1" "someValue"
                         "unknownField2" "anotherValue"}]

    (println "🔍 Testing field mapping with sample data...")
    (let [mapped-asset (map-asset-data sample-raw-data "figureMarkets" field-mappings)]
      (println "\n📊 Mapped data:")
      (println (.stringify js/JSON (clj->js mapped-asset) nil 2))

      (println "\n📋 Check data/unmapped-fields.log for unmapped fields")
      (create-mapping-report "figureMarkets" 1 2))))

;; Run demo if this file is executed directly
(when (= js/__filename js/process.argv.1)
  (demo))