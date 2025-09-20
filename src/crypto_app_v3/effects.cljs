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

;; Basic fetch event (placeholder)
(rf/reg-event-fx
 :fetch-crypto-data
 (fn [_ _]
   {:http-get {:url (data-url)
               :on-success :fetch-success
               :on-failure :fetch-failure}}))

(rf/reg-event-db
 :fetch-success
 (fn [db [_ data]]
   (println "✅ V3 Data received:", (keys data))
   (-> db
       (assoc-in [:ui :loading?] false)
       (assoc-in [:ui :error] nil))))

(rf/reg-event-db
 :fetch-failure
 (fn [db [_ error]]
   (println "❌ V3 Fetch failed:", error)
   (-> db
       (assoc-in [:ui :loading?] false)
       (assoc-in [:ui :error] error))))
