(ns crypto-app-v3.subs
  (:require [re-frame.core :as rf]))

;; Historical data subscription
(rf/reg-sub
 :historical-data
 (fn [db [_ crypto-id]]
   (get-in db [:historical-data crypto-id] [])))

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
(rf/reg-sub
 :prices
 (fn [db]
   (:prices db)))

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
 (fn [db]
   (let [url-param (when js/window.location.search
                     (.includes js/window.location.search "ui=v5"))
         local-storage (when-let [stored (.getItem js/localStorage "crypto-tracker-ui")]
                         (= stored "v5"))
         db-setting (get-in db [:ui :layout-version] false)]
     (or url-param local-storage db-setting))))

;; Chart period selection subscriptions
(rf/reg-sub
 :chart/current-period
 (fn [db [_ crypto-id]]
   (get-in db [:chart :periods crypto-id] "1W")))

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

;; Copy V2 sorting logic (small function)
(defn sort-crypto-keys [keys]
  (sort-by (fn [crypto-id]
             (cond
               (= crypto-id "hash") "0-hash"
               (= crypto-id "figr") "1-figr"
               :else crypto-id)) keys))

(rf/reg-sub
 :sorted-price-keys
 :<- [:price-keys]
 (fn [price-keys]
   (sort-crypto-keys price-keys)))

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
