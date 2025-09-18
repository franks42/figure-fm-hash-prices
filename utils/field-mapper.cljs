(ns field-mapper
  "Utilities for mapping API responses to standardized field format using api-field-mapping.json"
  (:require ["fs" :as fs]
            [clojure.walk :as walk]))

(def ^:private mapping-config (atom nil))

(defn load-mapping-config! []
  "Load the field mapping configuration from JSON file"
  (when (nil? @mapping-config)
    (reset! mapping-config
            (-> (fs/readFileSync "api-field-mapping.json" "utf8")
                js/JSON.parse
                js->clj))))

(defn get-field-mapping [field-name api-name]
  "Get the field mapping for a specific field and API"
  (load-mapping-config!)
  (let [field-mappings (get @mapping-config "fieldMappings")]
    (some (fn [[category-key category-data]]
            (when-let [field-data (get-in category-data ["fields" field-name])]
              (get-in field-data ["mappings" api-name])))
          field-mappings)))

(defn get-all-field-mappings [api-name]
  "Get all field mappings for a specific API"
  (load-mapping-config!)
  (let [field-mappings (get @mapping-config "fieldMappings")]
    (reduce (fn [acc [category-key category-data]]
              (let [fields (get category-data "fields")]
                (merge acc
                       (reduce (fn [field-acc [field-name field-data]]
                                 (if-let [api-field (get-in field-data ["mappings" api-name])]
                                   (assoc field-acc field-name
                                          {:apiField api-field
                                           :type (get field-data "type")
                                           :required (get field-data "required")
                                           :description (get field-data "description")})
                                   field-acc))
                               {}
                               fields))))
            {}
            field-mappings)))

(defn convert-value [value target-type]
  "Convert a value to the target type"
  (cond
    (nil? value) nil
    (= target-type "number") (if (number? value) value (js/parseFloat value))
    (= target-type "string") (str value)
    (= target-type "boolean") (boolean value)
    (= target-type "array") (if (array? value) value [value])
    :else value))

(defn map-api-response [api-response api-name]
  "Map an API response to standardized field format"
  (let [field-mappings (get-all-field-mappings api-name)]
    (reduce (fn [acc [std-field {:keys [apiField type required]}]]
              (if (and apiField (not= apiField "calculated"))
                (let [api-value (if (str/includes? apiField ".")
                                  ;; Handle nested field access like "data.EUR"
                                  (get-in api-response (str/split apiField #"\."))
                                  ;; Handle direct field access
                                  (get api-response apiField))
                      converted-value (convert-value api-value type)]
                  (if (or converted-value required)
                    (assoc acc std-field converted-value)
                    acc))
                acc))
            {}
            field-mappings)))

(defn map-figure-markets-response [response]
  "Map Figure Markets API response to standardized format"
  (map-api-response response "figureMarkets"))

(defn map-yahoo-finance-response [response]
  "Map Yahoo Finance API response to standardized format"
  (map-api-response response "yahooFinance"))

(defn map-coingecko-response [response]
  "Map CoinGecko API response to standardized format"
  (map-api-response response "coinGecko"))

(defn map-alpha-vantage-response [response]
  "Map Alpha Vantage API response to standardized format"
  (map-api-response response "alphaVantage"))

(defn map-exchange-rate-response [response provider]
  "Map exchange rate API response to standardized format"
  (load-mapping-config!)
  (let [exchange-mappings (get @mapping-config "exchangeRateFields")]
    (reduce (fn [acc [category-key category-data]]
              (let [fields (get category-data "fields")]
                (merge acc
                       (reduce (fn [field-acc [field-name field-data]]
                                 (if-let [api-field (get-in field-data ["mappings" provider])]
                                   (let [api-value (if (str/includes? api-field ".")
                                                     (get-in response (str/split api-field #"\."))
                                                     (get response api-field))
                                         target-type (get field-data "type")
                                         converted-value (convert-value api-value target-type)]
                                     (if converted-value
                                       (assoc field-acc field-name converted-value)
                                       field-acc))
                                   field-acc))
                               {}
                               fields))))
            {}
            exchange-mappings)))

;; Export functions for use in other modules
(def exports #js {:mapFigureMarketsResponse map-figure-markets-response
                  :mapYahooFinanceResponse map-yahoo-finance-response
                  :mapCoinGeckoResponse map-coingecko-response
                  :mapAlphaVantageResponse map-alpha-vantage-response
                  :mapExchangeRateResponse map-exchange-rate-response
                  :getFieldMapping get-field-mapping
                  :getAllFieldMappings get-all-field-mappings})