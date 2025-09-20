(ns crypto-app-v3.subs
  (:require [re-frame.core :as rf]))

;; Small, focused subscriptions

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
 :prices
 (fn [db]
   (:prices db)))

(rf/reg-sub
 :last-update
 (fn [db]
   (get-in db [:meta :last-update])))

(rf/reg-sub
 :price-keys
 (fn [db]
   (get-in db [:meta :price-keys])))

(rf/reg-sub
 :initial-load-complete?
 (fn [db]
   (get-in db [:ui :initial-load-complete?])))

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
