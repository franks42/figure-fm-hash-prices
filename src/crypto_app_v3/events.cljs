(ns crypto-app-v3.events
  (:require [re-frame.core :as rf]))

;; Small helper functions (copy V2 logic)

(defn initial-db []
  {:prices {}
   :ui {:loading? true
        :error nil
        :flash? false
        :initial-load-complete? false}
   :meta {:last-update nil
          :price-keys []}})

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

;; Small, focused event handlers

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

;; Main price update event (composed from small functions)
(rf/reg-event-fx
 :smart-price-update
 (fn [{:keys [db]} [_ raw-data]]
   (let [js-data (js->clj raw-data :keywordize-keys false)
         new-prices (extract-prices-from-response js-data)
         old-prices (:prices db)
         old-keys (get-in db [:meta :price-keys])
         new-keys (keys new-prices)
         changes (collect-price-changes old-prices new-prices)
         timestamp (format-timestamp (current-iso-timestamp))
         has-changes? (seq changes)
         keys-changed? (price-keys-changed? old-keys new-keys)]

     (cond-> {:db (-> db
                      (assoc-in [:meta :last-update] timestamp)
                      (assoc-in [:ui :loading?] false)
                      (assoc-in [:ui :error] nil))}

       ;; Update prices if changed
       has-changes?
       (assoc-in [:db :prices] (merge old-prices changes))

       ;; Update keys if changed  
       keys-changed?
       (assoc-in [:db :meta :price-keys] new-keys)

       ;; Flash animation if data changed
       has-changes?
       (assoc :dispatch [:trigger-flash])

       ;; Auto-clear flash
       has-changes?
       (assoc :dispatch-later [{:ms 800 :dispatch [:clear-flash]}])

       ;; Mark initial load complete
       (not (get-in db [:ui :initial-load-complete?]))
       (assoc :dispatch [:set-initial-load-complete])))))
