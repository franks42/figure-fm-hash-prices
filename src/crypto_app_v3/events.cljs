(ns crypto-app-v3.events
  (:require [re-frame.core :as rf]))

;; Small, focused event handlers

(defn initial-db []
  {:prices {}
   :ui {:loading? true
        :error nil
        :flash? false
        :initial-load-complete? false}
   :meta {:last-update nil
          :price-keys []}})

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
