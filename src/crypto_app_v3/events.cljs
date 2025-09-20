(ns crypto-app-v3.events
  (:require [re-frame.core :as rf]))

;; Copy V2 constants (small, focused)
(def ^:const POLL_INTERVAL_MS 30000)
(def ^:const FLASH_DURATION_MS 800)
(def ^:const SCAN_HIDE_DELAY_MS 2100)
(def ^:const TIMEOUT_MS 10000)

;; Helper functions FIRST (small, pure)
(defn extract-prices-from-response [js-data]
  (dissoc js-data "timestamp" "source" "last_update"))

(defn format-timestamp [iso-string]
  (.replace iso-string "T" " "))

(defn current-iso-timestamp []
  (.toISOString (js/Date.)))

(defn price-field-changed? [old-data new-data field]
  (not= (get old-data field) (get new-data field)))

(defn coin-data-changed? [old-data new-data]
  (or (price-field-changed? old-data new-data "usd")
      (price-field-changed? old-data new-data "usd_24h_change")
      (price-field-changed? old-data new-data "usd_24h_vol")
      (price-field-changed? old-data new-data "bid")
      (price-field-changed? old-data new-data "ask")))

(defn collect-price-changes [old-prices new-prices]
  (into {}
        (for [[coin-id new-data] new-prices
              :let [old-data (get old-prices coin-id)]
              :when (coin-data-changed? old-data new-data)]
          [coin-id new-data])))

(defn price-keys-changed? [old-keys new-keys]
  (not= (set old-keys) (set new-keys)))

;; Database structure
(defn initial-db []
  {:prices {}
   :price-keys []
   :last-update nil
   :ui {:loading? true
        :error nil
        :flash? false
        :initial-load-complete? false
        :display-currency "USD"}
   :portfolio {:holdings {}
               :show-panel nil}
   :currency {:current "USD"
              :exchange-rates {}
              :using-mock-rates? false
              :show-selector? false}})

;; Basic events (small functions)
(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   (initial-db)))

(rf/reg-event-db
 :set-loading
 (fn [db [_ loading?]]
   (assoc-in db [:ui :loading?] loading?)))

(rf/reg-event-db
 :set-error
 (fn [db [_ error]]
   (assoc-in db [:ui :error] error)))

(rf/reg-event-db
 :clear-error
 (fn [db _]
   (assoc-in db [:ui :error] nil)))

(rf/reg-event-db
 :trigger-flash
 (fn [db _]
   (assoc-in db [:ui :flash?] true)))

(rf/reg-event-db
 :clear-flash
 (fn [db _]
   (assoc-in db [:ui :flash?] false)))

(rf/reg-event-db
 :set-initial-load-complete
 (fn [db _]
   (assoc-in db [:ui :initial-load-complete?] true)))

;; Portfolio events
(rf/reg-event-db
 :portfolio/show-panel
 (fn [db [_ crypto-id]]
   (assoc-in db [:portfolio :show-panel] crypto-id)))

(rf/reg-event-db
 :portfolio/hide-panel
 (fn [db _]
   (assoc-in db [:portfolio :show-panel] nil)))

(rf/reg-event-db
 :portfolio/update-holding
 (fn [db [_ crypto-id quantity]]
   (if (and quantity (> quantity 0))
     (assoc-in db [:portfolio :holdings crypto-id] quantity)
     (update-in db [:portfolio :holdings] dissoc crypto-id))))

(rf/reg-event-db
 :portfolio/clear-all
 (fn [db _]
   (assoc-in db [:portfolio :holdings] {})))

(rf/reg-event-fx
 :portfolio/set-quantity
 (fn [{:keys [db]} [_ crypto-id quantity]]
   (js/console.log "🔴 Portfolio set-quantity called:" crypto-id quantity)
   (let [updated-db (if (and quantity (> quantity 0))
                      (assoc-in db [:portfolio :holdings crypto-id] quantity)
                      (update-in db [:portfolio :holdings] dissoc crypto-id))
         updated-holdings (get-in updated-db [:portfolio :holdings])]
     (js/console.log "🔴 Updated holdings:" updated-holdings)
     {:db updated-db
      :fx [[:local-storage/persist-portfolio updated-holdings]]})))

(rf/reg-event-db
 :portfolio/restore
 (fn [db [_ holdings]]
   (assoc-in db [:portfolio :holdings] holdings)))

(rf/reg-event-fx
 :portfolio/initialize
 (fn [_ _]
   {:fx [[:local-storage/load-portfolio]]}))

;; Currency conversion functions (copy V2)
(defn convert-currency [usd-amount target-currency exchange-rates]
  "Convert USD amount to target currency using current exchange rates"
  (if (= target-currency "USD")
    usd-amount
    (let [currency-key (keyword target-currency)
          rate (get exchange-rates currency-key)]
      (if rate
        (* usd-amount rate)
        usd-amount))))  ; Fallback to USD if rate not available

(defn get-currency-symbol [currency-code]
  "Get currency symbol for display"
  (case currency-code
    "USD" "$"
    "EUR" "€"
    "GBP" "£"
    "JPY" "¥"
    "CAD" "C$"
    "AUD" "A$"
    "CHF" "CHF"
    "CNY" "¥"
    "KRW" "₩"
    "SEK" "kr"
    currency-code))

;; Currency events (copy V2 logic)
(rf/reg-event-db
 :currency/set
 (fn [db [_ currency]]
   (assoc-in db [:currency :current] currency)))

(rf/reg-event-db
 :currency/set-exchange-rates
 (fn [db [_ rates using-mock?]]
   (-> db
       (assoc-in [:currency :exchange-rates] rates)
       (assoc-in [:currency :using-mock-rates?] using-mock?))))

;; Currency selector events (copy V2)
(rf/reg-event-db
 :currency/show-selector
 (fn [db _]
   (assoc-in db [:currency :show-selector?] true)))

(rf/reg-event-db
 :currency/hide-selector
 (fn [db _]
   (assoc-in db [:currency :show-selector?] false)))

(rf/reg-event-fx
 :currency/select
 (fn [{:keys [db]} [_ currency-code]]
   {:db (-> db
            (assoc-in [:currency :current] currency-code)
            (assoc-in [:ui :display-currency] currency-code)
            (assoc-in [:currency :show-selector?] false))
    :fx [[:local-storage/persist-currency currency-code]]}))

;; Currency display events (copy V2)
(rf/reg-event-db
 :display-currency/set
 (fn [db [_ currency]]
   (-> db
       (assoc-in [:ui :display-currency] currency)
       (assoc-in [:currency :current] currency))))

;; Main price update event (uses helper functions defined above)
(rf/reg-event-fx
 :smart-price-update
 (fn [{:keys [db]} [_ raw-data]]
   (let [js-data (js->clj raw-data :keywordize-keys false)
         new-prices (extract-prices-from-response js-data)
         old-prices (:prices db)
         old-keys (:price-keys db)
         new-keys (keys new-prices)
         changes (collect-price-changes old-prices new-prices)
         timestamp (format-timestamp (current-iso-timestamp))
         has-changes? (seq changes)
         keys-changed? (price-keys-changed? old-keys new-keys)]

     (cond-> {:db (-> db
                      (assoc :last-update timestamp)
                      (assoc-in [:ui :loading?] false)
                      (assoc-in [:ui :error] nil))}

       has-changes?
       (assoc-in [:db :prices] (merge old-prices changes))

       keys-changed?
       (assoc-in [:db :price-keys] new-keys)

       has-changes?
       (assoc :dispatch [:trigger-flash])

       has-changes?
       (assoc :dispatch-later [{:ms 800 :dispatch [:clear-flash]}])

       (not (get-in db [:ui :initial-load-complete?]))
       (assoc :dispatch [:set-initial-load-complete])))))
