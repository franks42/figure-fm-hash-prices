(ns crypto-app-v3.events
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; Copy V2 constants (small, focused)
(def ^:const POLL_INTERVAL_MS 30000)
(def ^:const FLASH_DURATION_MS 800)
(def ^:const SCAN_HIDE_DELAY_MS 2100)
(def ^:const TIMEOUT_MS 10000)

;; Historical chart events using custom :fetch effect
(rf/reg-event-fx
 :fetch-historical-data
 (fn [{:keys [db]} [_ crypto-id]]
   (let [end-time (js/Date.)
         start-time (js/Date. (- (.getTime end-time) (* 7 24 60 60 1000)))  ; 1 week ago
         url (str "https://www.figuremarkets.com/service-hft-exchange/api/v1/markets/"
                  (str/upper-case crypto-id) "-USD"
                  "/candles?start_date=" (.toISOString start-time)
                  "&end_date=" (.toISOString end-time)
                  "&interval_in_minutes=240&size=84")]  ; 4h intervals for 1 week
     (js/console.log "📈 Dispatching fetch for" crypto-id)
     {:db db
      :fetch {:url url
              :on-success [:historical-data-success crypto-id]
              :on-failure [:historical-data-failure crypto-id]}})))

(rf/reg-event-db
 :historical-data-success
 (fn [db [_ crypto-id response]]
   (let [raw-data (:matchHistoryData response)
         chart-data (when (seq raw-data)
                      (let [times (mapv #(-> % :date js/Date. .getTime (/ 1000)) raw-data)
                            prices (mapv #(js/parseFloat (:close %)) raw-data)]
                        [times prices]))]
     (js/console.log "✅ Historical data received for" crypto-id ":" (count raw-data) "points")
     (js/console.log "📊 Transformed to chart format:" chart-data)
     (assoc-in db [:historical-data crypto-id] chart-data))))

(rf/reg-event-db
 :historical-data-failure
 (fn [db [_ crypto-id error]]
   (js/console.log "❌ Historical data failed for" crypto-id ":" error)
   (assoc-in db [:historical-data crypto-id] [])))

;; Helper functions FIRST (small, pure)
(defn extract-prices-from-response [js-data]
  (dissoc js-data "timestamp" "source" "last_update"))

(defn extract-data-sources
  "Extract and parse the data sources from the top-level source field"
  [js-data]
  (when-let [source-str (get js-data "source")]
    (if (clojure.string/includes? source-str "+")
      (clojure.string/split source-str #"\+")
      [source-str])))

(defn format-timestamp [iso-string]
  (.replace iso-string "T" " "))

(defn update-timestamp []
  (let [now (js/Date.)
        formatted-time (.toLocaleString now "en-US" #js{:year "numeric"
                                                        :month "2-digit"
                                                        :day "2-digit"
                                                        :hour "2-digit"
                                                        :minute "2-digit"
                                                        :second "2-digit"
                                                        :hour12 false})]
    formatted-time))

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

;; Database structure - EDN handles ClojureScript data natively
(defn initial-db []
  {:prices {}
   :price-keys []
   :last-update nil
   :data-sources []
   :ui {:loading? true
        :error nil
        :flash? false
        :initial-load-complete? false
        :display-currency "USD"}
   :portfolio {:holdings {}  ; Regular ClojureScript map - EDN handles it fine
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

;; UI layout version toggle
(rf/reg-event-fx
 :ui/toggle-layout-version
 (fn [{:keys [db]} [_ version]]
   (js/console.log "🎨 Switching UI layout to:" version)
   (.setItem js/localStorage "crypto-tracker-ui" version)
   {:db (assoc-in db [:ui :layout-version] (= version "v5"))}))

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
   (js/console.log "📝 EDN Portfolio set-quantity called:" crypto-id quantity)
   (let [current-holdings (get-in db [:portfolio :holdings] {})
         updated-holdings (if (and quantity (> quantity 0))
                            (assoc current-holdings crypto-id quantity)
                            (dissoc current-holdings crypto-id))
         updated-db (assoc-in db [:portfolio :holdings] updated-holdings)]
     (js/console.log "📝 EDN Current holdings:" current-holdings)
     (js/console.log "📝 EDN Updated holdings:" updated-holdings)
     (js/console.log "📝 EDN Holdings type:" (type updated-holdings))
     {:db updated-db
      :fx [[:local-storage/persist-portfolio updated-holdings]]})))

(rf/reg-event-db
 :portfolio/restore
 (fn [db [_ js-holdings]]
   (js/console.log "📖 Restoring JS holdings to database:" js-holdings)
   (js/console.log "📖 JS holdings type:" (type js-holdings))
   ;; Store JS object directly to avoid persistent map conversion
   (assoc-in db [:portfolio :holdings] js-holdings)))

(rf/reg-event-fx
 :portfolio/initialize
 (fn [_ _]
   {:fx [[:local-storage/load-portfolio]]}))

;; Currency conversion functions (copy V2)
(defn convert-currency
  "Convert USD amount to target currency using current exchange rates"
  [usd-amount target-currency exchange-rates]
  (if (= target-currency "USD")
    usd-amount
    (let [currency-key (keyword target-currency)
          rate (get exchange-rates currency-key)]
      (if rate
        (* usd-amount rate)
        usd-amount))))  ; Fallback to USD if rate not available

(defn get-currency-symbol
  "Get currency symbol for display"
  [currency-code]
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
         data-sources (extract-data-sources js-data)
         old-prices (:prices db)
         old-keys (:price-keys db)
         new-keys (keys new-prices)
         changes (collect-price-changes old-prices new-prices)
         _timestamp (format-timestamp (current-iso-timestamp))
         has-changes? (seq changes)
         keys-changed? (price-keys-changed? old-keys new-keys)]

     (cond-> {:db (-> db
                      (assoc :last-update (update-timestamp))
                      (assoc :data-sources data-sources)
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
