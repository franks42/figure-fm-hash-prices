(ns crypto-app-v3.transform
  "Canonical data transformers for live-data-first architecture"
  (:require [clojure.string :as str]))

;; Canonical data format
;; {:id "hash", :type "crypto"|"stock", :source :figure|:twelve|:github, 
;;  :timestamp ms, :usd price, :day_high, :day_low, :usd_24h_change %, 
;;  :usd_24h_vol, :trades_24h, :symbol, :bid, :ask, :last_price}

(defmulti transform->canonical
  "Transform provider response to canonical format"
  (fn [provider _raw] provider))

;; Figure Markets transformer
(defmethod transform->canonical :figure [_ raw-response]
  (js/console.log "🔄 TRANSFORM: Figure Markets → canonical format")
  (let [data (get raw-response "data")
        ;; First pass: find YLDS-USD price for FGRD conversion
        ylds-usd-price (reduce (fn [_ item]
                                 (when (= (get item "symbol") "YLDS-USD-2F")
                                   (reduced (js/parseFloat (get item "lastTradedPrice" "1")))))
                               1.0 data)]
    (js/console.log "🔄 TRANSFORM: YLDS-USD price:" ylds-usd-price)
    (reduce (fn [acc item]
              (let [raw-symbol (get item "symbol" "")
                    symbol (-> raw-symbol
                               (str/replace "-USD" "")
                               str/lower-case)]
                (cond
                  ;; Standard -USD pairs
                  (and (str/ends-with? raw-symbol "-USD")
                       (contains? #{"hash" "btc" "eth" "figr_heloc" "link" "sol" "uni" "xrp"} symbol))
                  (do
                    (js/console.log "✅ TRANSFORM: Including" symbol)
                    (assoc acc (keyword symbol)
                           {:id symbol
                            :type "crypto"
                            :source :figure
                            :timestamp (js/Date.now)
                            :usd (js/parseFloat (get item "midMarketPrice" "0"))
                            :day_high (js/parseFloat (get item "high24h" "0"))
                            :day_low (js/parseFloat (get item "low24h" "0"))
                            :usd_24h_change (* (js/parseFloat (get item "percentageChange24h" "0")) 100)
                            :usd_24h_vol (js/parseFloat (get item "volume24h" "0"))
                            :trades_24h (js/parseInt (get item "tradeCount24h" "0"))
                            :symbol raw-symbol
                            :bid (js/parseFloat (get item "bestBid" "0"))
                            :ask (js/parseFloat (get item "bestAsk" "0"))
                            :last_price (js/parseFloat (get item "lastTradedPrice" "0"))}))

                  ;; FGRD-YLDS pair — convert to USD via YLDS price
                  (= raw-symbol "FGRD-YLDS")
                  (let [last-price (* (js/parseFloat (get item "lastTradedPrice" "0")) ylds-usd-price)
                        bid (* (js/parseFloat (get item "bestBid" "0")) ylds-usd-price)
                        ask (* (js/parseFloat (get item "bestAsk" "0")) ylds-usd-price)]
                    (js/console.log "✅ TRANSFORM: Including fgrd (via YLDS→USD @" ylds-usd-price ")")
                    (assoc acc :fgrd
                           {:id "fgrd"
                            :type "tokenized_stock"
                            :source :figure
                            :timestamp (js/Date.now)
                            :usd last-price
                            :day_high (* (js/parseFloat (get item "high24h" "0")) ylds-usd-price)
                            :day_low (* (js/parseFloat (get item "low24h" "0")) ylds-usd-price)
                            :usd_24h_change (* (js/parseFloat (get item "percentageChange24h" "0")) 100)
                            :usd_24h_vol (* (js/parseFloat (get item "volume24h" "0")) ylds-usd-price)
                            :trades_24h (js/parseInt (get item "tradeCount24h" "0"))
                            :symbol "FGRD-YLDS"
                            :bid bid
                            :ask ask
                            :last_price last-price
                            :quote_currency "YLDS"
                            :ylds_usd_rate ylds-usd-price}))

                  :else
                  (do
                    (js/console.log "⏭️ TRANSFORM: Skipping" symbol)
                    acc))))
            {}
            data))))

;; Twelve Data transformer  
(defmethod transform->canonical :twelve [_ raw-response]
  (js/console.log "🔄 TRANSFORM: Twelve Data → canonical format")
  (let [symbol (str/lower-case (get raw-response "symbol" "figr"))]
    {(keyword symbol) {:id symbol
                       :type "stock"
                       :source :twelve
                       :timestamp (js/Date.now)
                       :usd (js/parseFloat (get raw-response "close" "0"))
                       :day_high (js/parseFloat (get raw-response "high" "0"))
                       :day_low (js/parseFloat (get raw-response "low" "0"))
                       :usd_24h_change (js/parseFloat (get raw-response "percent_change" "0"))
                       :usd_24h_vol (js/parseInt (get raw-response "volume" "0"))
                       :trades_24h nil
                       :symbol (get raw-response "symbol" "FIGR")
                       :bid nil
                       :ask nil
                       :last_price (js/parseFloat (get raw-response "close" "0"))
                       :open (js/parseFloat (get raw-response "open" "0"))
                       :previous_close (js/parseFloat (get raw-response "previous_close" "0"))
                       :change (js/parseFloat (get raw-response "change" "0"))
                       :fifty_two_week_high (js/parseFloat (get-in raw-response ["fifty_two_week" "high"] "0"))
                       :fifty_two_week_low (js/parseFloat (get-in raw-response ["fifty_two_week" "low"] "0"))
                       :company_name (get raw-response "name" "")
                       :exchange (get raw-response "exchange" "")
                       :currency (get raw-response "currency" "USD")
                       :timezone (get raw-response "timezone" "EDT")
                       :is_market_open (get raw-response "is_market_open" false)}}))

;; GitHub Actions transformer (mostly pass-through)
(defmethod transform->canonical :github [_ raw-response]
  (js/console.log "🔄 TRANSFORM: GitHub backup → canonical format")
  (let [cleaned-data (dissoc raw-response "timestamp" "source" "last_update")]
    (reduce (fn [acc [asset-key asset-data]]
              (if (map? asset-data)
                (assoc acc asset-key
                       (assoc asset-data
                              :id (name asset-key)
                              :source :github
                              :timestamp (js/Date.now)))
                (assoc acc asset-key asset-data)))
            {}
            cleaned-data)))

;; Format conversion shim (Oracle-recommended)
(defn canonical->v5
  "Convert {:hash {:usd 1.0}} → {\"hash\" {\"usd\" 1.0}} (deep keyword→string)"
  [canonical]
  (into {}
        (for [[k v] canonical]
          [(name k)                       ; top-level key → string
           (into {} (for [[ik iv] v]      ; inner keys → string
                      [(name ik) iv]))])))

;; Merge strategy for mixed data sources
(defn fill-missing-data
  "Merge primary data with fallback, primary wins"
  [primary-data fallback-data]
  (js/console.log "🔄 MERGE: Filling missing data - primary:" (count (keys primary-data)) "fallback:" (count (keys fallback-data)))
  (merge fallback-data primary-data))
