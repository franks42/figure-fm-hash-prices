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
