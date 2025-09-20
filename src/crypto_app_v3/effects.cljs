(ns crypto-app-v3.effects
  (:require [re-frame.core :as rf]))

;; Copy V2 data processing logic (small functions)

(defn extract-prices-from-response [js-data]
  (dissoc js-data "timestamp" "source" "last_update"))

(defn format-timestamp [iso-string]
  (.replace iso-string "T" " "))

(defn current-iso-timestamp []
  (.toISOString (js/Date.)))

(defn data-url []
  "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json")

(defn cache-bust-url [url]
  (str url "?t=" (js/Date.now)))

;; Copy V2 change detection (small functions)
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

;; Effects (small, focused)
(rf/reg-fx
 :http-get
 (fn [{:keys [url on-success on-failure]}]
   (-> (js/fetch (cache-bust-url url))
       (.then (fn [response] (.json response)))
       (.then (fn [data]
                (rf/dispatch [on-success (js->clj data :keywordize-keys false)])))
       (.catch (fn [error]
                 (rf/dispatch [on-failure (.-message error)]))))))

(rf/reg-fx
 :dispatch-later
 (fn [events]
   (doseq [{:keys [ms dispatch]} events]
     (js/setTimeout #(rf/dispatch dispatch) ms))))

(rf/reg-fx
 :start-polling
 (fn [interval-ms]
   (js/setInterval #(rf/dispatch [:fetch-crypto-data]) interval-ms)))

(rf/reg-fx
 :dom-add-class
 (fn [{:keys [element-id class-name]}]
   (when-let [element (.getElementById js/document element-id)]
     (.add (.-classList element) class-name))))

(rf/reg-fx
 :dom-remove-class
 (fn [{:keys [element-id class-name]}]
   (when-let [element (.getElementById js/document element-id)]
     (.remove (.-classList element) class-name))))

;; Main fetch events (copy V2 logic)
(rf/reg-event-fx
 :fetch-crypto-data
 (fn [_ _]
   {:http-get {:url (data-url)
               :on-success :fetch-success
               :on-failure :fetch-failure}
    :dom-add-class {:element-id "fetch-indicator" :class-name "active"}}))

(rf/reg-event-fx
 :fetch-success
 (fn [_ [_ data]]
   {:dispatch [:smart-price-update data]
    :dispatch-later [{:ms 2100 :dispatch [:hide-fetch-indicator]}]}))

(rf/reg-event-fx
 :fetch-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:ui :loading?] false)
            (assoc-in [:ui :error] error))
    :dom-remove-class {:element-id "fetch-indicator" :class-name "active"}}))

(rf/reg-event-fx
 :hide-fetch-indicator
 (fn [_ _]
   {:dom-remove-class {:element-id "fetch-indicator" :class-name "active"}}))

(rf/reg-event-fx
 :start-auto-polling
 (fn [_ _]
   {:start-polling 30000}))

;; Mock exchange rates (copy V2)
(def mock-exchange-rates
  {:EUR 0.85
   :GBP 0.73
   :JPY 110.0
   :CAD 1.25
   :AUD 1.35
   :CHF 0.92
   :CNY 6.45
   :KRW 1180.0
   :SEK 9.75})

;; Exchange rate effects (copy V2)
(rf/reg-fx
 :http-exchange-rates
 (fn [{:keys [url on-success on-failure]}]
   (-> (js/fetch (cache-bust-url url))
       (.then (fn [response] 
                (if (.-ok response)
                  (.json response)
                  (throw (js/Error. (str "HTTP " (.-status response)))))))
       (.then (fn [data]
                (let [rates-data (js->clj data :keywordize-keys true)
                      rates (:rates rates-data)]
                  (rf/dispatch [on-success rates false]))))
       (.catch (fn [error]
                  (js/console.warn "💱 Failed to load exchange rates, using mock:" error)
                  (rf/dispatch [on-success mock-exchange-rates true]))))))

;; Exchange rate events
(rf/reg-event-fx
 :fetch-exchange-rates
 (fn [_ _]
   {:http-exchange-rates {:url "data/exchange-rates.json"
                          :on-success :exchange-rates-success
                          :on-failure :exchange-rates-failure}}))

(rf/reg-event-fx
 :exchange-rates-success
 (fn [{:keys [db]} [_ rates using-mock?]]
   {:db (-> db
            (assoc-in [:currency :exchange-rates] rates)
            (assoc-in [:currency :using-mock-rates?] using-mock?))}))

;; Copy V2's EXACT working localStorage implementation
(rf/reg-fx
 :local-storage/persist-portfolio
 (fn [holdings]
   (try
     (js/console.log "🚨 FINAL - Saving portfolio to localStorage:" holdings)
     (js/console.log "🚨 FINAL - Holdings type:" (type holdings))
     (let [json-string (.stringify js/JSON (clj->js holdings))]
       (js/console.log "🚨 FINAL - JSON string being saved:" json-string)
       (.setItem js/localStorage "crypto-portfolio-v3" json-string)
       ;; IMMEDIATE READ-BACK VERIFICATION
       (let [read-back (.getItem js/localStorage "crypto-portfolio-v3")]
         (js/console.log "🚨 VERIFICATION - Read back from localStorage:" read-back)
         (js/console.log "🚨 VERIFICATION - Read-back matches saved?" (= json-string read-back))))
     (js/console.log "🚨 FINAL - Portfolio saved successfully")
     (catch :default e
       (js/console.warn "❌ Failed to save portfolio to localStorage:" e)))))

(rf/reg-fx
 :local-storage/load-portfolio
 (fn [_]
   (try
     (js/console.log "📖 Loading portfolio from localStorage...")
     (let [stored-data (.getItem js/localStorage "crypto-portfolio-v3")]
       (js/console.log "📖 Raw stored data:" stored-data)
       (when stored-data
         (let [parsed-data (js->clj (.parse js/JSON stored-data))]
           (js/console.log "✅ Portfolio loaded successfully:" parsed-data)
           (rf/dispatch [:portfolio/restore parsed-data]))))
     (catch :default e
       (js/console.warn "❌ Failed to load portfolio from localStorage:" e)))))

(rf/reg-fx
 :local-storage/persist-currency
 (fn [currency]
   (js/localStorage.setItem "selected-currency" currency)))

(rf/reg-fx
 :local-storage/load-currency
 (fn [_]
   (when-let [currency (js/localStorage.getItem "selected-currency")]
     (rf/dispatch [:currency/set currency]))))
