(ns crypto-app-v3.portfolio-utils
  (:require [clojure.set :as set]))

;; Portfolio aggregation utilities for PF card
;; Converts portfolio holdings into Figure-Markets-style data structure

(defn portfolio->price-map
  "Convert portfolio holdings to Figure-Markets-style price data structure
   Inputs: holdings map {crypto-id quantity}, prices map {crypto-id price-data}
   Returns: price-data map compatible with existing crypto cards"
  [holdings prices]
  (if (or (empty? holdings) (empty? prices))
    nil
    (let [;; Calculate current total value
          total-current (->> holdings
                            (map (fn [[crypto-id qty]]
                                   (let [price-data (get prices crypto-id)
                                         current-price (get price-data "usd" 0)]
                                     (* qty current-price))))
                            (reduce + 0))
          
          ;; Calculate 24h ago total value for percentage change
          total-24h-ago (->> holdings
                            (map (fn [[crypto-id qty]]
                                   (let [price-data (get prices crypto-id)
                                         current-price (get price-data "usd" 0)
                                         change-pct (get price-data "usd_24h_change" 0)
                                         ;; Calculate 24h ago price: current / (1 + change%)
                                         price-24h-ago (if (= change-pct 0)
                                                         current-price
                                                         (/ current-price (+ 1 (/ change-pct 100))))]
                                     (* qty price-24h-ago))))
                            (reduce + 0))
          
          ;; Calculate portfolio percentage change
          pf-change-pct (if (> total-24h-ago 0)
                          (* 100 (/ (- total-current total-24h-ago) total-24h-ago))
                          0)
          
          ;; Calculate high/low from individual asset highs/lows
          total-high (->> holdings
                         (map (fn [[crypto-id qty]]
                                (let [price-data (get prices crypto-id)
                                      day-high (get price-data "day_high" 0)]
                                  (* qty day-high))))
                         (reduce + 0))
          
          total-low (->> holdings
                        (map (fn [[crypto-id qty]]
                               (let [price-data (get prices crypto-id)
                                     day-low (get price-data "day_low" 0)]
                                 (* qty day-low))))
                        (reduce + 0))
          
          ;; Aggregate volume and trades
          total-volume (->> holdings
                           (map (fn [[crypto-id _]]
                                  (let [price-data (get prices crypto-id)]
                                    (get price-data "usd_24h_vol" 0))))
                           (reduce + 0))
          
          total-trades (->> holdings
                           (map (fn [[crypto-id _]]
                                  (let [price-data (get prices crypto-id)]
                                    (get price-data "trades_24h" 0))))
                           (reduce + 0))]
      
      ;; Return Figure-Markets-compatible data structure
      {"usd" total-current
       "usd_24h_change" pf-change-pct
       "day_high" total-high
       "day_low" total-low
       "usd_24h_vol" total-volume
       "trades_24h" total-trades
       "type" "portfolio"})))

(defn aggregate-historical
  "Calculate portfolio historical data from individual asset historical data
   Inputs: holdings map {crypto-id quantity}, historical-map {crypto-id [timestamps prices]}
   Returns: [timestamps portfolio-values] in same format as crypto historical data"
  [holdings historical-map]
  (if (or (empty? holdings) (empty? historical-map))
    []
    (let [;; Get all assets that have both holdings and historical data
          available-assets (filter #(and (contains? holdings %)
                                         (contains? historical-map %)
                                         (seq (get historical-map %)))
                                   (keys holdings))
          
          ;; Find common timestamps across all available assets
          timestamp-sets (map (fn [crypto-id]
                               (let [[timestamps _] (get historical-map crypto-id)]
                                 (set timestamps)))
                             available-assets)
          
          common-timestamps (if (seq timestamp-sets)
                             (apply set/intersection timestamp-sets)
                             #{})
          
          ;; Sort timestamps chronologically
          sorted-timestamps (sort (vec common-timestamps))]
      
      (if (empty? sorted-timestamps)
        []
        (let [;; Create index lookups for each asset
              asset-lookups (into {}
                                 (map (fn [crypto-id]
                                        (let [[timestamps prices] (get historical-map crypto-id)
                                              lookup (zipmap timestamps prices)]
                                          [crypto-id lookup]))
                                      available-assets))
              
              ;; Calculate portfolio value at each timestamp
              portfolio-values (mapv (fn [timestamp]
                                      (->> available-assets
                                           (map (fn [crypto-id]
                                                  (let [qty (get holdings crypto-id 0)
                                                        price (get-in asset-lookups [crypto-id timestamp] 0)]
                                                    (* qty price))))
                                           (reduce + 0)))
                                    sorted-timestamps)]
          
          [sorted-timestamps portfolio-values])))))
