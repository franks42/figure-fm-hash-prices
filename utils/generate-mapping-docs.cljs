#!/usr/bin/env nbb

(ns generate-mapping-docs
  (:require ["fs" :as fs]
            ["path" :as path]
            [clojure.string :as str]))

(defn load-mapping-config []
  "Load the field mapping configuration from JSON"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj
      (get "fieldMappings")))

(defn load-exchange-rate-config []
  "Load the exchange rate field mappings from JSON"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj
      (get "exchangeRateFields")))

(defn load-metadata []
  "Load metadata from JSON"
  (-> (fs/readFileSync "api-field-mapping.json" "utf8")
      js/JSON.parse
      js->clj
      (get "metadata")))

(defn format-field-value [value]
  "Format field value for markdown table"
  (cond
    (nil? value) "-"
    (= value "calculated") "(calculated)"
    (map? value) (str "`" (get value "amount") "` + `" (get value "currency") "`")
    :else (str "`" value "`")))

(defn generate-field-row [field-name field-data]
  "Generate a markdown table row for a field"
  (let [mappings (get field-data "mappings")
        type (get field-data "type")
        description (get field-data "description")]
    (str "| `" field-name "` | " type " | "
         (format-field-value (get mappings "figureMarkets")) " | "
         (format-field-value (get mappings "yahooFinance")) " | "
         (format-field-value (get mappings "coinGecko")) " | "
         (format-field-value (get mappings "alphaVantage")) " | "
         (format-field-value (get mappings "currentInternal")) " | "
         description " |")))

(defn generate-exchange-rate-row [field-name field-data]
  "Generate a markdown table row for exchange rate fields"
  (let [mappings (get field-data "mappings")
        type (get field-data "type")
        description (get field-data "description")]
    (str "| `" field-name "` | " type " | "
         (format-field-value (get mappings "freeCurrencyApi")) " | "
         (format-field-value (get mappings "exchangeRateHost")) " | "
         (format-field-value (get mappings "fixerIo")) " | "
         description " |")))

(defn generate-category-section [category-name category-data]
  "Generate markdown section for a category"
  (let [category-title (get category-data "category")
        fields (get category-data "fields")]
    (str "| **" category-title "** |\n"
         (str/join "\n"
                   (map (fn [[field-name field-data]]
                          (generate-field-row field-name field-data))
                        fields)))))

(defn generate-exchange-rate-category-section [category-name category-data]
  "Generate markdown section for exchange rate category"
  (let [category-title (get category-data "category")
        fields (get category-data "fields")]
    (str "| **" category-title "** |\n"
         (str/join "\n"
                   (map (fn [[field-name field-data]]
                          (generate-exchange-rate-row field-name field-data))
                        fields)))))

(defn typescript-type [field-type]
  "Convert field type to TypeScript type"
  (case field-type
    "array" "string[]"
    "currencyAmount" "CurrencyAmount"
    "tokenAmount" "TokenAmount"
    field-type))

(defn generate-typescript-interface [mapping-config]
  "Generate TypeScript interface from mapping config"
  (let [all-fields (mapcat (fn [[_ category]]
                             (map (fn [[field-name field-data]]
                                    [field-name field-data])
                                  (get category "fields")))
                           mapping-config)]
    (str "// Currency amount types\n"
         "interface CurrencyAmount {\n"
         "  amount: number;\n"
         "  currency: string; // USD, EUR, JPY, etc.\n"
         "}\n\n"
         "interface TokenAmount {\n"
         "  amount: number;\n"
         "  currency: string; // BTC, ETH, AAPL, etc.\n"
         "}\n\n"
         "interface StandardizedAsset {\n"
         (str/join "\n"
                   (map (fn [[field-name field-data]]
                          (let [type (get field-data "type")
                                required (get field-data "required")
                                description (get field-data "description")
                                optional-marker (if required "" "?")]
                            (str "  // " description "\n"
                                 "  " field-name optional-marker ": "
                                 (typescript-type type) ";")))
                        all-fields))
         "\n}")))

(defn generate-exchange-rate-interface [exchange-config]
  "Generate TypeScript interface for exchange rates"
  (str "interface StandardizedExchangeRates {\n"
       "  baseCurrency: string;\n"
       "  date: string;\n"
       "  timestamp?: number;\n"
       "  rates: {\n"
       "    [currencyCode: string]: number;\n"
       "  };\n"
       "  success?: boolean;\n"
       "  source?: string;\n"
       "}\n\n"
       "// Common currency rates\n"
       "interface CommonCurrencyRates {\n"
       "  eur: number;   // Euro\n"
       "  jpy: number;   // Japanese Yen\n"
       "  gbp: number;   // British Pound\n"
       "  cad: number;   // Canadian Dollar\n"
       "  aud: number;   // Australian Dollar\n"
       "  chf: number;   // Swiss Franc\n"
       "  sgd: number;   // Singapore Dollar\n"
       "  hkd: number;   // Hong Kong Dollar\n"
       "  krw: number;   // Korean Won\n"
       "  cny: number;   // Chinese Yuan\n"
       "  inr: number;   // Indian Rupee\n"
       "  brl: number;   // Brazilian Real\n"
       "  mxn: number;   // Mexican Peso\n"
       "  aed: number;   // UAE Dirham\n"
       "  nok: number;   // Norwegian Krone\n"
       "  sek: number;   // Swedish Krona\n"
       "  nzd: number;   // New Zealand Dollar\n"
       "  thb: number;   // Thai Baht\n"
       "  vnd: number;   // Vietnamese Dong\n"
       "  myr: number;   // Malaysian Ringgit\n"
       "}"))

(defn generate-implementation-examples []
  "Generate implementation examples"
  (str "### Currency Conversion Implementation\n\n"
       "```typescript\n"
       "// Example mapping functions for each API\n"
       "function mapFigureMarketsData(apiResponse: any): StandardizedAsset {\n"
       "  return {\n"
       "    assetId: apiResponse.id,\n"
       "    symbol: apiResponse.symbol,\n"
       "    displayName: apiResponse.displayName,\n"
       "    baseCurrency: apiResponse.denom,\n"
       "    quoteCurrency: apiResponse.quoteDenom,\n"
       "    assetType: \"crypto\",\n"
       "    currentPrice: Number(apiResponse.midMarketPrice),\n"
       "    bidPrice: Number(apiResponse.bestBid),\n"
       "    askPrice: Number(apiResponse.bestAsk),\n"
       "    volume24h: Number(apiResponse.volume24h),\n"
       "    // ... map other fields\n"
       "  };\n"
       "}\n\n"
       "function mapYahooFinanceData(apiResponse: any): StandardizedAsset {\n"
       "  return {\n"
       "    symbol: apiResponse.symbol,\n"
       "    displayName: apiResponse.longName,\n"
       "    quoteCurrency: apiResponse.currency,\n"
       "    assetType: \"stock\",\n"
       "    currentPrice: apiResponse.regularMarketPrice,\n"
       "    bidPrice: apiResponse.bid,\n"
       "    askPrice: apiResponse.ask,\n"
       "    volume24h: apiResponse.regularMarketVolume,\n"
       "    // ... map other fields\n"
       "  };\n"
       "}\n\n"
       "function mapCoinGeckoData(apiResponse: any): StandardizedAsset {\n"
       "  return {\n"
       "    assetId: apiResponse.id,\n"
       "    symbol: apiResponse.symbol?.toUpperCase(),\n"
       "    displayName: apiResponse.name,\n"
       "    assetType: \"crypto\",\n"
       "    currentPrice: apiResponse.current_price,\n"
       "    marketCap: apiResponse.market_cap,\n"
       "    volume24h: apiResponse.total_volume,\n"
       "    priceChangePercent24h: apiResponse.price_change_percentage_24h,\n"
       "    // ... map other fields\n"
       "  };\n"
       "}\n\n"
       "function mapExchangeRateData(apiResponse: any, provider: string): StandardizedExchangeRates {\n"
       "  switch (provider) {\n"
       "    case 'freecurrencyapi':\n"
       "      return {\n"
       "        baseCurrency: apiResponse.base_currency || 'USD',\n"
       "        date: apiResponse.date,\n"
       "        rates: apiResponse.data,\n"
       "        source: 'FreeCurrencyAPI'\n"
       "      };\n"
       "    case 'exchangerate.host':\n"
       "      return {\n"
       "        baseCurrency: apiResponse.base,\n"
       "        date: apiResponse.date,\n"
       "        timestamp: apiResponse.timestamp,\n"
       "        rates: apiResponse.rates,\n"
       "        success: apiResponse.success,\n"
       "        source: 'ExchangeRate.host'\n"
       "      };\n"
       "    default:\n"
       "      throw new Error(`Unknown exchange rate provider: ${provider}`);\n"
       "  }\n"
       "}\n"
       "```"))

(defn generate-markdown-doc []
  "Generate complete markdown documentation from JSON config"
  (let [mapping-config (load-mapping-config)
        exchange-config (load-exchange-rate-config)
        metadata (load-metadata)

        header (str "# API Field Mapping & Standardization\n\n"
                    "*Generated from api-field-mapping.json - DO NOT EDIT MANUALLY*\n\n"
                    "## Field Mapping Table\n\n"
                    "| **Standardized Field** | **Type** | **Figure Markets API** | **Yahoo Finance API** | **CoinGecko API** | **Alpha Vantage API** | **Current Internal** | **Description** |\n"
                    "|------------------------|----------|------------------------|----------------------|-------------------|----------------------|---------------------|-----------------|")

        field-sections (str/join "\n"
                                 (map (fn [[category-name category-data]]
                                        (generate-category-section category-name category-data))
                                      mapping-config))

        exchange-rate-header (str "\n\n## Currency Exchange Rate APIs\n\n"
                                  "| **Standardized Field** | **Type** | **FreeCurrencyAPI** | **ExchangeRate.host** | **Fixer.io** | **Description** |\n"
                                  "|------------------------|----------|--------------------|-----------------------|---------------|-----------------|")

        exchange-rate-sections (str/join "\n"
                                         (map (fn [[category-name category-data]]
                                                (generate-exchange-rate-category-section category-name category-data))
                                              exchange-config))

        typescript-section (str "\n\n### Exchange Rate TypeScript Interface\n\n"
                                "```typescript\n"
                                (generate-exchange-rate-interface exchange-config)
                                "\n```\n\n"
                                "## Type Definitions\n\n"
                                "```typescript\n"
                                (generate-typescript-interface mapping-config)
                                "\n```")

        standardization-section (str "\n\n## Key Standardization Decisions\n\n"
                                     "1. **Naming Convention**: camelCase for all fields (JavaScript/TypeScript friendly)\n"
                                     "2. **Price Fields**: All prices as `number` type (no strings)\n"
                                     "3. **Consistent Terminology**:\n"
                                     "   - `Price` suffix for all price-related fields\n"
                                     "   - `24h` suffix for 24-hour statistics\n"
                                     "   - `Percent` for percentage values\n"
                                     "4. **Core Fields**: Every asset must have `symbol`, `currentPrice`, `assetType`\n"
                                     "5. **Optional Fields**: Most fields optional to accommodate different API capabilities\n"
                                     "6. **Type Safety**: Strong typing with enums for `assetType`")

        implementation-section (str "\n\n## Implementation Notes\n\n"
                                    "- **Field Mapping Function**: Create utility functions to map from each API format to standardized format\n"
                                    "- **Validation**: Ensure required fields are present and types are correct\n"
                                    "- **Fallbacks**: Handle missing fields gracefully with sensible defaults\n"
                                    "- **Performance**: Consider caching mapped data to avoid repeated transformations\n\n"
                                    (generate-implementation-examples))

        footer (str "\n\n---\n\n"
                    "*Last updated: " (get metadata "lastUpdated") "*\n"
                    "*Generated from: api-field-mapping.json v" (get metadata "version") "*")]

    (str header "\n"
         field-sections "\n"
         exchange-rate-header "\n"
         exchange-rate-sections
         typescript-section
         standardization-section
         implementation-section
         footer)))

(defn main []
  "Main function to generate markdown documentation"
  (try
    (let [markdown-content (generate-markdown-doc)]
      (fs/writeFileSync "api-field-mapping.md" markdown-content)
      (println "✅ Generated api-field-mapping.md from api-field-mapping.json"))
    (catch js/Error e
      (println "❌ Error generating documentation:" (.-message e)))))

;; Run main function
(main)