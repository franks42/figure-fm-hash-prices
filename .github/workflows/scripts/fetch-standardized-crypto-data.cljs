#!/usr/bin/env nbb

(ns fetch-standardized-crypto-data
  "Fetch crypto data and convert to standardized format with unmapped field logging"
  (:require ["fs" :as fs]
            [clojure.string :as str]))

(defn load-field-mappings []
  "Load the standardized field mapping configuration"
  (try
    (let [file-content (fs/readFileSync "api-field-mapping.json" "utf8")
          parsed (js/JSON.parse file-content)
          clj-data (js->clj parsed)]
      (println "✅ Loaded field mappings successfully")
      clj-data)
    (catch js/Error e
      (println "❌ Error loading field mappings:" (.-message e))
      (js/process.exit 1))))

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

(defn map-github-data-to-standardized [raw-asset]
  "Convert GitHub data (old format) to standardized format"
  (let [raw-data (js->clj raw-asset)
        ;; The data from GitHub is in the old format
        symbol (get raw-data "symbol")
        price (get raw-data "usd")
        volume (get raw-data "usd_24h_vol")
        change (get raw-data "usd_24h_change")
        bid (get raw-data "bid")
        ask (get raw-data "ask")
        asset-type (get raw-data "type")
        timestamp (.valueOf (js/Date.))]

    ;; Build standardized format
    {"symbol" symbol
     "currentPrice" {"amount" price
                     "currency" "USD"}
     "volume24h" {"amount" volume
                  "currency" "USD"}
     "priceChange24h" change
     "bidPrice" bid
     "askPrice" ask
     "assetType" asset-type
     "dataSource" "GitHub/Figure Markets"
     "timestamp" timestamp}))

(defn fetch-figure-markets-data []
  "Fetch data from GitHub raw data branch (same source as the main app)"
  (println "🔄 Fetching data from GitHub data branch...")
  (let [url (str "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json?t=" (js/Date.now))]
    (-> (js/fetch url)
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP " (.-status response)))))))
        (.then (fn [data]
                 (let [clj-data (js->clj data)
                       assets-array (vals clj-data)]
                   (println "📊 Fetched" (count assets-array) "assets from GitHub")
                   (clj->js assets-array))))
        (.catch (fn [error]
                  (println "❌ Error fetching from GitHub data branch:" (.-message error))
                  (js/process.exit 1))))))

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

(defn -main []
  "Main function to fetch and convert crypto data"
  (println "🚀 Starting standardized crypto data fetch...")
  (let [field-mappings (load-field-mappings)]
    (-> (fetch-figure-markets-data)
        (.then (fn [raw-data]
                 (try
                   (println "📊 Processing" (count raw-data) "assets...")
                   (let [standardized-data (doall (map map-github-data-to-standardized raw-data))
                         total-assets (count raw-data)
                         has-unmapped-fields (fs/existsSync "data/unmapped-fields.log")]
                     (save-standardized-data standardized-data)
                     (create-mapping-report total-assets has-unmapped-fields)
                     (println "🎉 Standardized data fetch completed successfully!")
                     (println "📋 Check data/mapping-report.json for mapping statistics")
                     (when has-unmapped-fields
                       (println "⚠️  Check data/unmapped-fields.log for unmapped fields")))
                   (catch js/Error e
                     (println "❌ Error processing data:" (.-message e))
                     (println "Stack:" (.-stack e))
                     (js/process.exit 1)))))
        (.catch (fn [error]
                  (println "❌ Error fetching data:" (.-message error))
                  (js/process.exit 1))))))

;; Run the main function
(-main)