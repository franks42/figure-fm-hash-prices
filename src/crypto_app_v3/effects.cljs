(ns crypto-app-v3.effects
  (:require [re-frame.core :as rf]))

;; Small, focused effects

(defn data-url []
  "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json")

(defn cache-bust-url [url]
  (str url "?t=" (js/Date.now)))

(rf/reg-fx
 :http-get
 (fn [{:keys [url on-success on-failure]}]
   (-> (js/fetch (cache-bust-url url))
       (.then (fn [response] (.json response)))
       (.then (fn [data]
                (rf/dispatch [on-success (js->clj data :keywordize-keys false)])))
       (.catch (fn [error]
                 (rf/dispatch [on-failure (.-message error)]))))))

;; Timer effect for flash animation
(rf/reg-fx
 :dispatch-later
 (fn [events]
   (doseq [{:keys [ms dispatch]} events]
     (js/setTimeout #(rf/dispatch dispatch) ms))))

;; Basic fetch event (placeholder)
(rf/reg-event-fx
 :fetch-crypto-data
 (fn [_ _]
   {:http-get {:url (data-url)
               :on-success :fetch-success
               :on-failure :fetch-failure}}))

(rf/reg-event-fx
 :fetch-success
 (fn [_ [_ data]]
   (println "✅ V3 Data received:", (keys data))
   {:dispatch [:smart-price-update data]}))

(rf/reg-event-db
 :fetch-failure
 (fn [db [_ error]]
   (println "❌ V3 Fetch failed:", error)
   (-> db
       (assoc-in [:ui :loading?] false)
       (assoc-in [:ui :error] error))))
