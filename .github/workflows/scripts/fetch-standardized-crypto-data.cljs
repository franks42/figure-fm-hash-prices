#!/usr/bin/env nbb

(ns fetch-standardized-crypto-data
  "Fetch crypto data and convert to standardized format with unmapped field logging"
  (:require ["fs" :as fs]
            ["axios" :as axios]
            [clojure.string :as str]))

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

(defn map-figure-markets-asset [field-mappings raw-asset]
  "Convert Figure Markets asset to standardized format"
  (let [raw-data (js->clj raw-asset)
        source-mappings (get-all-source-mappings field-mappings "figureMarkets")
        unmapped-fields (find-unmapped-fields raw-data source-mappings)
        mapped-data (atom {})]

    ;; Map known fields
    (doseq [[raw-field standard-field] source-mappings]
      (when-let [raw-value (get raw-data raw-field)]
        (swap! mapped-data assoc standard-field raw-value)))

    ;; Get asset symbol for logging
    (let [asset-symbol (or (get @mapped-data "symbol") "UNKNOWN")]
      ;; Log unmapped fields
      (log-unmapped-fields unmapped-fields "Figure Markets" asset-symbol))

    ;; Build standardized asset with special handling for currencyAmount fields
    (let [base-data @mapped-data
          enhanced-data (merge base-data
                               {;; currencyAmount conversions
                                "currentPrice" (convert-to-currency-amount
                                                (get base-data "midMarketPrice")
                                                (get base-data "quoteDenom"))
                                "volume24h" (convert-to-currency-amount
                                             (get base-data "volume24h")
                                             (get base-data "quoteDenom"))
                                "baseVolume24h" (convert-to-token-amount
                                                 (get base-data "baseVolume24h")
                                                 (get base-data "denom"))
                                "marketCap" (when (get base-data "marketCap")
                                              (convert-to-currency-amount
                                               (get base-data "marketCap")
                                               (get base-data "quoteDenom")))

                               ;; Add metadata
                                "dataSource" "Figure Markets"
                                "timestamp" (.getTime (js/Date.))
                                "lastUpdate" (.toISOString (js/Date.))})]

      ;; Return only non-nil values
      (into {} (filter #(some? (second %)) enhanced-data)))))

(defn fetch-figure-markets-data []
  "Fetch data from Figure Markets API"
  (println "🔄 Fetching data from Figure Markets API...")
  (let [api-token (.-FIGURE_API_TOKEN js/process.env)]
    (if (empty? api-token)
      (do
        (println "❌ FIGURE_API_TOKEN not found in environment")
        (js/process.exit 1))
      (-> (axios/get "https://api.figuremarkets.com/v1/markets"
                     #js {:headers #js {"Authorization" (str "Bearer " api-token)}})
          (.then #(.-data %))
          (.catch #(do
                     (println "❌ Error fetching Figure Markets data:" (.-message %))
                     (js/process.exit 1)))))))

(defn save-standardized-data [standardized-data]
  "Save standardized data with metadata"
  (let [json-string (.stringify js/JSON (clj->js standardized-data) nil 2)
        timestamp (.toISOString (js/Date.))]

    ;; Ensure data directory exists
    (when-not (fs/existsSync "data")
      (fs/mkdirSync "data"))

    ;; Save standardized data
    (fs/writeFileSync "data/standardized-prices.json" json-string)

    ;; Create metadata file
    (let [metadata {"lastUpdated" timestamp
                    "totalAssets" (count standardized-data)
                    "dataSource" "Figure Markets"
                    "format" "standardized-v1.0"
                    "currency" "USD"
                    "version" "1.0.0"
                    "fieldMappingVersion" "1.1.0"}]
      (fs/writeFileSync "data/standardized-metadata.json"
                        (.stringify js/JSON (clj->js metadata) nil 2)))

    (println "✅ Saved" (count standardized-data) "assets to data/standardized-prices.json")
    (println "📊 Metadata saved to data/standardized-metadata.json")))

(defn create-mapping-report [total-assets has-unmapped-fields]
  "Create a mapping report"
  (let [report {"timestamp" (.toISOString (js/Date.))
                "source" "Figure Markets"
                "totalAssets" total-assets
                "hasUnmappedFields" has-unmapped-fields
                "mappingCoverage" (if has-unmapped-fields
                                    "Partial - Check unmapped-fields.log"
                                    "Complete")
                "recommendation" (if has-unmapped-fields
                                   "Update api-field-mapping.json with new field mappings"
                                   "All fields successfully mapped")
                "logFiles" ["data/unmapped-fields.log" "data/mapping-report.json"]}]

    (fs/writeFileSync "data/mapping-report.json"
                      (.stringify js/JSON (clj->js report) nil 2))
    report))

(defn main []
  "Main function to fetch and convert crypto data"
  (-> (js/Promise.resolve)
      (.then #(do
                (println "🚀 Starting standardized crypto data fetch...")
                (load-field-mappings)))
      (.then (fn [field-mappings]
               (-> (fetch-figure-markets-data)
                   (.then (fn [raw-data]
                            (println "📊 Processing" (count raw-data) "assets...")
                            (let [standardized-data (map #(map-figure-markets-asset field-mappings %) raw-data)
                                  total-assets (count raw-data)
                                  has-unmapped-fields (fs/existsSync "data/unmapped-fields.log")]
                              (save-standardized-data standardized-data)
                              (create-mapping-report total-assets has-unmapped-fields)
                              (println "🎉 Standardized data fetch completed successfully!")
                              (println "📋 Check data/mapping-report.json for mapping statistics")
                              (when has-unmapped-fields
                                (println "⚠️  Check data/unmapped-fields.log for unmapped fields"))))))))
      (.catch (fn [error]
                (println "❌ Error in main process:" (.-message error))
                (js/process.exit 1)))))

;; Run the main function
(main)