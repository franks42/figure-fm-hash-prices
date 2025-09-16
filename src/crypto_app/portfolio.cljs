(ns crypto-app.portfolio
  (:require [crypto-app.state :as state]))

;; Pure portfolio calculation functions (no side effects)

(defn calculate-holding-value [quantity current-price]
  "Calculate the current value of a holding"
  (when (and quantity current-price (> quantity 0) (> current-price 0))
    (* quantity current-price)))

(defn get-total-portfolio-value [portfolio current-prices]
  "Calculate total portfolio value"
  (reduce-kv (fn [total crypto-id quantity]
               (let [current-price (get-in current-prices [crypto-id "usd"])]
                 (if (and current-price (> current-price 0))
                   (+ total (* quantity current-price))
                   total)))
             0
             portfolio))

(defn get-portfolio-assets-count [portfolio]
  "Get count of different assets held"
  (count (filter #(> (second %) 0) portfolio)))

;; State management functions (in-memory only for now)

(defn update-holding [crypto-id quantity]
  "Update a holding quantity (or remove if 0)"
  (if (and quantity (> quantity 0))
    (swap! state/portfolio-atom assoc crypto-id quantity)
    (swap! state/portfolio-atom dissoc crypto-id)))

(defn remove-holding [crypto-id]
  "Remove a holding completely"
  (update-holding crypto-id 0))

(defn clear-all-holdings []
  "Clear all portfolio holdings"
  (reset! state/portfolio-atom {}))
