(ns crypto-app-v3.subs
  (:require [re-frame.core :as rf]
            [crypto-app-v3.portfolio-utils :as pf-utils]))

;; Historical data subscription - handles portfolio aggregation
(rf/reg-sub
 :historical-data
 (fn [db [_ crypto-id]]
   (if (= crypto-id "pf")
     ;; Portfolio historical data - use aggregation
     @(rf/subscribe [:portfolio/historical-data])
     ;; Regular crypto historical data
     (get-in db [:historical-data crypto-id] []))))

;; UI state subscriptions
(rf/reg-sub
 :loading?
 (fn [db]
   (get-in db [:ui :loading?])))

(rf/reg-sub
 :error-message
 (fn [db]
   (get-in db [:ui :error])))

(rf/reg-sub
 :flash-active?
 (fn [db]
   (get-in db [:ui :flash?])))

(rf/reg-sub
 :initial-load-complete?
 (fn [db]
   (get-in db [:ui :initial-load-complete?])))

;; Data subscriptions
;; Raw prices from feed (before portfolio integration)
(rf/reg-sub
 :raw/prices
 (fn [db]
   (:prices db)))

;; Portfolio aggregation subscriptions
(rf/reg-sub
 :portfolio/aggregated-price
 :<- [:portfolio/holdings]
 :<- [:raw/prices]
 (fn [[holdings prices] _]
   (pf-utils/portfolio->price-map holdings prices)))

(rf/reg-sub
 :portfolio/historical-data
 :<- [:portfolio/holdings]
 :<- [:historical-map]
 :<- [:chart/current-period]  ; React to period changes
 (fn [[holdings historical-map period] _]
   (js/console.log "📊 PF: Aggregating historical data for period:" period "holdings:" (keys holdings))
   (let [result (pf-utils/aggregate-historical holdings historical-map)]
     (js/console.log "📊 PF: Aggregated result length:" (if (vector? result) (count (first result)) "not vector"))
     result)))

(rf/reg-sub
 :portfolio/has-holdings?
 :<- [:portfolio/holdings]
 (fn [holdings _]
   (and holdings (> (count holdings) 0))))

;; Historical map for portfolio aggregation
(rf/reg-sub
 :historical-map
 (fn [db _]
   (:historical-data db)))

;; Enhanced prices including portfolio when it exists
(rf/reg-sub
 :prices
 :<- [:raw/prices]
 :<- [:portfolio/aggregated-price]
 (fn [[feed-prices pf-price] _]
   (if pf-price
     (assoc feed-prices "pf" pf-price)
     feed-prices)))

(rf/reg-sub
 :price-keys
 (fn [db]
   (:price-keys db)))

(rf/reg-sub
 :last-update
 (fn [db]
   (:last-update db)))

(rf/reg-sub
 :data-sources
 (fn [db]
   (:data-sources db)))

;; Portfolio subscriptions
(rf/reg-sub
 :portfolio/holdings
 (fn [db]
   (get-in db [:portfolio :holdings])))

(rf/reg-sub
 :portfolio/show-panel
 (fn [db]
   (get-in db [:portfolio :show-panel])))

(rf/reg-sub
 :portfolio/panel-crypto-id
 (fn [db]
   (get-in db [:portfolio :show-panel])))

(rf/reg-sub
 :portfolio/quantity
 (fn [db [_ crypto-id]]
   (get-in db [:portfolio :holdings crypto-id] 0)))

;; Currency display subscriptions (copy V2)
(rf/reg-sub
 :display-currency
 (fn [db]
   (get-in db [:ui :display-currency] "USD")))

;; UI feature flag subscriptions
(rf/reg-sub
 :ui/new-layout?
 (fn [_]
   ;; V5 is the ONLY interface - no V4 option exists
   true))

;; Chart period selection subscriptions - GLOBAL like currency
(rf/reg-sub
 :chart/current-period
 (fn [db]
   (get-in db [:chart :current-period] "1W")))

(rf/reg-sub
 :chart/available-periods
 (fn [_]
   ["24H" "1W" "1M"]))

;; Currency subscriptions (copy V2)
(rf/reg-sub
 :currency/current
 (fn [db]
   (get-in db [:currency :current] "USD")))

(rf/reg-sub
 :currency/exchange-rates
 (fn [db]
   (get-in db [:currency :exchange-rates] {})))

(rf/reg-sub
 :currency/using-mock-rates?
 (fn [db]
   (get-in db [:currency :using-mock-rates?] false)))

(rf/reg-sub
 :currency/show-selector?
 (fn [db]
   (get-in db [:currency :show-selector?] false)))

;; Copy V2 sorting logic with portfolio first
(defn sort-crypto-keys [keys]
  (sort-by (fn [crypto-id]
             (cond
               (= crypto-id "pf") "00-pf"        ; Portfolio first
               (= crypto-id "hash") "01-hash"    ; HASH second
               (= crypto-id "figr") "02-figr"    ; FIGR third
               :else crypto-id)) keys))

(rf/reg-sub
 :sorted-price-keys
 :<- [:price-keys]
 :<- [:portfolio/has-holdings?]
 (fn [[price-keys has-holdings?] _]
   (let [keys-with-pf (if has-holdings?
                        (conj price-keys "pf")
                        price-keys)]
     (sort-crypto-keys keys-with-pf))))

;; Portfolio calculations (copy V2 portfolio logic)
(defn calculate-holding-value [quantity current-price]
  (when (and quantity current-price (> quantity 0) (> current-price 0))
    (* quantity current-price)))

(rf/reg-sub
 :portfolio/holding-value
 (fn [db [_ crypto-id]]
   (let [quantity (get-in db [:portfolio :holdings crypto-id])
         price (get-in db [:prices crypto-id "usd"])]
     (calculate-holding-value quantity price))))

(rf/reg-sub
 :portfolio/total-value
 :<- [:portfolio/holdings]
 :<- [:prices]
 (fn [[holdings prices]]
   (reduce-kv (fn [total crypto-id quantity]
                (let [current-price (get-in prices [crypto-id "usd"])]
                  (if (and current-price (> current-price 0))
                    (+ total (* quantity current-price))
                    total)))
              0
              holdings)))

;; V5 Portfolio data layer subscriptions
(rf/reg-sub
 :portfolio/qty
 (fn [db [_ crypto-id]]
   (get-in db [:portfolio :holdings crypto-id] 0)))

(rf/reg-sub
 :portfolio/value
 (fn [db [_ crypto-id]]
   (let [quantity (get-in db [:portfolio :holdings crypto-id] 0)
         price (get-in db [:prices crypto-id "usd"])]
     (calculate-holding-value quantity price))))
