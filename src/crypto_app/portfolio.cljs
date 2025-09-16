(ns crypto-app.portfolio
  (:require [crypto-app.state :as state]))

;; Portfolio utility functions

(defn save-portfolio-to-localStorage [portfolio]
  "Save portfolio data to localStorage"
  (try
    (js/localStorage.setItem "crypto-tracker-portfolio" (js/JSON.stringify (clj->js portfolio)))
    (catch js/Error e
      (js/console.error "Failed to save portfolio to localStorage:" e))))

(defn load-portfolio-from-localStorage []
  "Load portfolio data from localStorage"
  (try
    (let [stored (js/localStorage.getItem "crypto-tracker-portfolio")]
      (if stored
        (js->clj (js/JSON.parse stored) :keywordize-keys false)
        {}))
    (catch js/Error e
      (js/console.error "Failed to load portfolio from localStorage:" e)
      {})))

(defn update-holding [crypto-id quantity]
  "Update a holding quantity (or remove if 0)"
  (let [new-portfolio (if (and quantity (> quantity 0))
                        (assoc @state/portfolio-atom crypto-id quantity)
                        (dissoc @state/portfolio-atom crypto-id))]
    (reset! state/portfolio-atom new-portfolio)
    (save-portfolio-to-localStorage new-portfolio)))

(defn remove-holding [crypto-id]
  "Remove a holding completely"
  (update-holding crypto-id 0))

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

(defn initialize-portfolio []
  "Load portfolio from localStorage on app start"
  (let [stored-portfolio (load-portfolio-from-localStorage)]
    (reset! state/portfolio-atom stored-portfolio)))
