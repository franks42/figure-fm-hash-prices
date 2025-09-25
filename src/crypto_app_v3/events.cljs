(ns crypto-app-v3.events
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [crypto-app-v3.transform :as tx]))

;; Copy V2 constants (small, focused)
(def ^:const POLL_INTERVAL_MS 30000)
(def ^:const FLASH_DURATION_MS 800)
(def ^:const SCAN_HIDE_DELAY_MS 2100)
(def ^:const TIMEOUT_MS 10000)

;; FIGR IPO and Twelve Data constants  
(def ^:const FIGR_IPO_DATE "2025-09-11")
(def ^:const TWELVE_DATA_API_KEY "b61354a1fe6f45a2a9e01c8c4145e617")

;; FIGR period logic for Twelve Data (Oracle-recommended)
(defn get-figr-interval [period]
  (case period
    "24H" "5min"   ; Rich intraday for 24H
    "1W"  "15min"  ; Good granularity for 1W
    "1M"  "1day"   ; Daily for 1M
    "15min"))      ; Default to 1W

(defn get-figr-row-count [period]
  (case period
    "24H" 288  ; 24h * 12 intervals/hour (5min)
    "1W"  672  ; 7d * 24h * 4 intervals/hour (15min)  
    "1M"  30   ; 30 trading days max
    100))      ; Default reasonable limit

;; Pure Twelve Data JSON transformer (Oracle-recommended)
(defn twelve-data->chart-data
  "Transform Twelve Data JSON to V5 chart format [timestamps[], prices[]]"
  [td-response period]
  (js/console.log "🔍 RAW TD-RESPONSE:" td-response)
  (when-let [values (get td-response "values")]
    (js/console.log "🔍 VALUES COUNT:" (count values))
    (let [;; Filter out old 2022 data (symbol reuse artifact)
          ipo-date (js/Date. FIGR_IPO_DATE)
          recent-values (filter #(>= (js/Date. (get % "datetime")) ipo-date) values)
          ;; Sort ascending (Twelve Data comes newest first)  
          sorted-values (reverse recent-values)
          ;; Apply period-based row limiting
          row-count (get-figr-row-count period)
          limited-values (take-last row-count sorted-values)
          timestamps (mapv #(-> % (get "datetime") js/Date. .getTime (/ 1000)) limited-values)
          prices (mapv #(-> % (get "close") js/parseFloat) limited-values)]
      (js/console.log "🏦 TWELVE-DATA SUCCESS:" period "points:" (count timestamps) "range:" (first prices) "-" (last prices))
      [timestamps prices])))

;; Period configuration
(defn get-period-config [period]
  (case period
    "24H" {:days 1 :interval 60 :size 48}      ; 1h intervals, 24h
    "1W"  {:days 7 :interval 240 :size 84}     ; 4h intervals, 1 week  
    "1M"  {:days 30 :interval 1440 :size 60}   ; 1 day intervals, 1 month
    {:days 7 :interval 240 :size 84}))         ; Default to 1W

;; Historical chart events using custom :fetch effect
(rf/reg-event-fx
 :fetch-historical-data
 (fn [{:keys [db]} [_ crypto-id]]
   (js/console.log "🚀 FETCH-HISTORICAL-DATA called for:" crypto-id)
   (if (= crypto-id "figr")
     ;; Route FIGR to Twelve Data
     (let [period (get-in db [:chart :current-period] "1W")]
       (js/console.log "🏦 ROUTING FIGR to Twelve Data, period:" period)
       {:fx [[:dispatch [:fetch-figr-daily period]]]})
     ;; Regular crypto logic
     (let [period (get-in db [:chart :current-period] "1W")
           config (get-period-config period)
           end-time (js/Date.)
           start-time (js/Date. (- (.getTime end-time) (* (:days config) 24 60 60 1000)))
           url (str "https://www.figuremarkets.com/service-hft-exchange/api/v1/markets/"
                    (str/upper-case crypto-id) "-USD"
                    "/candles?start_date=" (.toISOString start-time)
                    "&end_date=" (.toISOString end-time)
                    "&interval_in_minutes=" (:interval config) "&size=" (:size config))]
       (js/console.log "📈 Dispatching fetch for" crypto-id "period:" period "config:" config)
       {:db db
        :fetch {:url url
                :on-success [:historical-data-success crypto-id]
                :on-failure [:historical-data-failure crypto-id]}}))))

;; Period-specific fetch event
(rf/reg-event-fx
 :fetch-historical-data-period
 (fn [{:keys [db]} [_ crypto-id period]]
   (if (= crypto-id "figr")
     ;; Route FIGR to Alpha Vantage
     {:fx [[:dispatch [:fetch-figr-daily period]]]}
     ;; Regular crypto logic
     (let [config (get-period-config period)
           end-time (js/Date.)
           start-time (js/Date. (- (.getTime end-time) (* (:days config) 24 60 60 1000)))
           url (str "https://www.figuremarkets.com/service-hft-exchange/api/v1/markets/"
                    (str/upper-case crypto-id) "-USD"
                    "/candles?start_date=" (.toISOString start-time)
                    "&end_date=" (.toISOString end-time)
                    "&interval_in_minutes=" (:interval config) "&size=" (:size config))]
       (js/console.log "📈 Fetching" period "data for" crypto-id "config:" config)
       {:db db
        :fetch {:url url
                :on-success [:historical-data-success crypto-id]
                :on-failure [:historical-data-failure crypto-id]}}))))

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

;; FIGR Twelve Data events (Oracle-recommended rich intraday data)
(rf/reg-event-fx
 :fetch-figr-daily
 (fn [{:keys [db]} [_ period]]
   (js/console.log "🏦 FETCH-FIGR-DAILY called with period:" period)
   (let [cache-key (str "figr-twelve-v1-" period)
         cached-data (.getItem js/localStorage cache-key)]
     (if cached-data
       (let [parsed-cache (js->clj (js/JSON.parse cached-data) :keywordize-keys true)]
         (if (< (- (js/Date.now) (:timestamp parsed-cache)) (* 60 60 1000))  ; 1h cache for intraday
           (do
             (js/console.log "🏦 TWELVE-DATA: Using cached FIGR data for period:" period)
             {:db (assoc-in db [:historical-data "figr"] (:chart-data parsed-cache))})
           (do
             (js/console.log "📈 Cache expired, fetching fresh FIGR data for period:" period)
             (let [interval (get-figr-interval period)
                   url (str "https://api.twelvedata.com/time_series"
                            "?symbol=FIGR"
                            "&interval=" interval
                            "&apikey=" TWELVE_DATA_API_KEY)]
               {:db db
                :fetch-strings {:url url
                                :on-success [:figr-twelve-success period]
                                :on-failure [:figr-twelve-failure period]}}))))
       (do
         (js/console.log "🏦 TWELVE-DATA: No cache, fetching FIGR data for period:" period)
         (let [interval (get-figr-interval period)
               url (str "https://api.twelvedata.com/time_series"
                        "?symbol=FIGR"
                        "&interval=" interval
                        "&apikey=" TWELVE_DATA_API_KEY)]
           {:db db
            :fetch-strings {:url url
                            :on-success [:figr-twelve-success period]
                            :on-failure [:figr-twelve-failure period]}}))))))

(rf/reg-event-db
 :figr-twelve-success
 (fn [db [_ period response]]
   (js/console.log "🏦 TWELVE-DATA SUCCESS:" period)
   (js/console.log "🏦 RAW RESPONSE:" (pr-str response))
   (if (= (get response "status") "error")
     (do
       (js/console.log "❌ Twelve Data error:" (get response "message"))
       (assoc-in db [:historical-data "figr"] []))
     (let [chart-data (twelve-data->chart-data response period)]
       (js/console.log "🔍 TRANSFORM RESULT:" chart-data)
       (if chart-data
         (let [cache-key (str "figr-twelve-v1-" period)
               cache-entry {:chart-data chart-data
                            :timestamp (js/Date.now)
                            :period period}]
           (.setItem js/localStorage cache-key (js/JSON.stringify (clj->js cache-entry)))
           (js/console.log "📊 FIGR cached and stored:" chart-data)
           (assoc-in db [:historical-data "figr"] chart-data))
         (do
           (js/console.log "❌ TRANSFORM FAILED - no chart data returned")
           (assoc-in db [:historical-data "figr"] [])))))))

(rf/reg-event-db
 :figr-twelve-failure
 (fn [db [_ period error]]
   (js/console.log "🏦 TWELVE-DATA FAILED:" period "error:" error)
   (assoc-in db [:historical-data "figr"] [])))

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
              :show-selector? false}
   :chart {:current-period "1W"}})

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

;; Chart period selection events - GLOBAL like currency
(rf/reg-event-fx
 :chart/set-period
 (fn [{:keys [db]} [_ period]]
   (js/console.log "⏰ Setting GLOBAL period to" period)
   {:db (assoc-in db [:chart :current-period] period)
    :fx [[:local-storage/persist-period period]
         [:dispatch-later [{:ms 100 :dispatch [:chart/fetch-all-historical period]}]]]}))

(rf/reg-event-fx
 :chart/cycle-period
 (fn [{:keys [db]} [_]]
   (let [current-period (get-in db [:chart :current-period] "1W")
         next-period (case current-period
                       "24H" "1W"
                       "1W" "1M"
                       "1M" "24H"
                       "1W")]  ; Default fallback
     (js/console.log "⏰ Cycling GLOBAL period from" current-period "to" next-period)
     {:fx [[:dispatch [:chart/set-period next-period]]]})))

(rf/reg-event-fx
 :chart/fetch-all-historical
 (fn [{:keys [db]} [_ period]]
   (let [crypto-ids (get db :price-keys [])]
     (js/console.log "📈 Fetching historical data for all assets, period:" period)
     {:fx (mapv (fn [crypto-id] [:dispatch [:fetch-historical-data-period crypto-id period]]) crypto-ids)})))

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
 (fn [db [_ holdings-map]]
   (js/console.log "📖 Restoring CLJS holdings to database:" holdings-map)
   (js/console.log "📖 Holdings map type:" (type holdings-map))
   ;; Ensure we store a proper CLJS map for subscriptions
   (assoc-in db [:portfolio :holdings] (js->clj holdings-map :keywordize-keys false))))

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

;; V5 Portfolio data layer events
(rf/reg-event-fx
 :portfolio/set-qty
 (fn [{:keys [db]} [_ crypto-id quantity]]
   (js/console.log "📝 V5 Portfolio set-qty called:" crypto-id quantity)
   (let [updated-holdings (if (and quantity (> quantity 0))
                            (assoc (get-in db [:portfolio :holdings] {}) crypto-id quantity)
                            (dissoc (get-in db [:portfolio :holdings] {}) crypto-id))
         updated-db (assoc-in db [:portfolio :holdings] updated-holdings)]
     (js/console.log "📝 V5 Updated holdings:" updated-holdings)
     {:db updated-db
      :fx [[:local-storage/persist-portfolio updated-holdings]
            ;; Trigger historical data fetch for all holdings to enable PF chart
           [:dispatch-later [{:ms 500 :dispatch [:portfolio/fetch-all-historical]}]]]})))

(rf/reg-event-db
 :portfolio/remove
 (fn [db [_ crypto-id]]
   (update-in db [:portfolio :holdings] dissoc crypto-id)))

;; Fetch historical data for all portfolio holdings to enable PF chart
(rf/reg-event-fx
 :portfolio/fetch-all-historical
 (fn [{:keys [db]} [_]]
   (let [holdings (get-in db [:portfolio :holdings] {})
         current-period (get-in db [:chart :current-period] "1W")]
     (js/console.log "📊 PF: Fetching historical data for holdings:" (keys holdings))
     {:fx (mapv (fn [crypto-id]
                  [:dispatch [:fetch-historical-data crypto-id]])
                (keys holdings))})))

;; Live-data-first provider events (Oracle-recommended)
(rf/reg-event-db
 :provider/success
 (fn [db [_ provider canonical-data]]
   (js/console.log "✅ LIVE: Provider success -" provider "assets:" (count (keys canonical-data)))
   (let [v5-data (tx/canonical->v5 canonical-data)  ; Oracle's shim
         current-prices (:prices db)
         updated-prices (merge current-prices v5-data)
         new-keys (keys updated-prices)]
     (js/console.log "🔄 LIVE: Converted to V5 format -" (keys v5-data))
     (-> db
         (assoc :prices updated-prices)
         (assoc :price-keys new-keys)
         (assoc :last-update (update-timestamp))
         (assoc-in [:ui :loading?] false)
         (assoc-in [:ui :error] nil)
         (assoc-in [:provider-status provider] :success)))))

(rf/reg-event-db
 :provider/failure
 (fn [db [_ provider error]]
   (js/console.error "❌ LIVE: Provider failure -" provider ":" error)
   (assoc-in db [:provider-status provider] :failed)))

(rf/reg-event-fx
 :market-data/fallback-check
 (fn [{:keys [db]} [_]]
   ;; LIVE DATA ONLY - NO BACKUP FALLBACK
   (js/console.log "✅ LIVE DATA ONLY - No GitHub backup used")
   {:dispatch [:trigger-flash]
    :dispatch-later [{:ms 800 :dispatch [:clear-flash]}]}))

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
