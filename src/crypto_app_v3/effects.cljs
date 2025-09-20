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
