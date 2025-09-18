(ns crypto-app-v2.portfolio
  (:require [crypto-app-v2.state :as state]
            [clojure.string :as str]))

;; Pure portfolio calculation functions (no side effects)

(defn calculate-holding-value [quantity current-price]
  (when (and quantity current-price (> quantity 0) (> current-price 0))
    (* quantity current-price)))

(defn get-total-portfolio-value [portfolio current-prices]
  (reduce-kv (fn [total crypto-id quantity]
               (let [asset (first (filter (fn [a]
                                            (let [symbol (get a :symbol)
                                                  symbol-key (when symbol
                                                               (str/lower-case
                                                                (first (str/split symbol #"-"))))]
                                              (= crypto-id symbol-key))) current-prices))
                     current-price (get-in asset [:currentPrice :amount])]
                 (if (and current-price (> current-price 0))
                   (+ total (* quantity current-price))
                   total)))
             0
             portfolio))

(defn get-portfolio-assets-count [portfolio]
  (count (filter #(> (second %) 0) portfolio)))

;; State management functions (in-memory only for now)

(defn update-holding [crypto-id quantity]
  (if (and quantity (> quantity 0))
    (swap! state/portfolio-atom assoc crypto-id quantity)
    (swap! state/portfolio-atom dissoc crypto-id)))

(defn remove-holding [crypto-id]
  (update-holding crypto-id 0))

(defn clear-all-holdings []
  (reset! state/portfolio-atom {}))