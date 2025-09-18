#!/usr/bin/env nbb

(require '[cljs.core :as cljs])
(require '[clojure.string :as str])

;; Exchange rate fetching script for GitHub Actions
;; Fetches USD exchange rates to various currencies using exchangerate-api.com (free, no API key needed)

(def supported-currencies ["EUR" "GBP" "JPY" "CAD" "AUD" "CHF" "CNY" "KRW" "SEK"])

(defn fetch-exchange-rates []
  "Fetch current USD exchange rates from exchangerate-api.com"
  (let [api-url "https://api.exchangerate-api.com/v4/latest/USD"]
    (-> (js/fetch api-url)
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "✅ Exchange rates fetched successfully")
                 (let [clj-data (js->clj data :keywordize-keys true)
                       all-rates (:rates clj-data)
                       ;; Filter to only include our supported currencies
                       filtered-rates (select-keys all-rates
                                                   (map keyword supported-currencies))]
                   {:rates filtered-rates
                    :date (:date clj-data)
                    :base (:base clj-data)}))))))

(defn save-exchange-rates [rates-data]
  "Save exchange rates to JSON file"
  (let [fs (js/require "fs")
        path "data/exchange-rates.json"
        rates (:rates rates-data)
        timestamp (js/Date.now)
        output {:rates rates
                :timestamp timestamp
                :base "USD"
                :source "exchangerate-api.com"
                :lastUpdate (.toISOString (js/Date.))}]
    (js/console.log "💾 Saving exchange rates to" path)
    (js/console.log "📊 Rates:" (clj->js rates))
    (.writeFileSync fs path (.stringify js/JSON (clj->js output) nil 2))
    (js/console.log "✅ Exchange rates saved successfully")))

(defn main []
  "Main function to fetch and save exchange rates"
  (js/console.log "🚀 Starting exchange rate fetch...")
  (js/console.log "🌍 Fetching rates for currencies:" (clj->js supported-currencies))

  (-> (fetch-exchange-rates)
      (.then save-exchange-rates)
      (.then (fn []
               (js/console.log "🎉 Exchange rate fetch completed successfully")
               (js/process.exit 0)))
      (.catch (fn [error]
                (js/console.error "❌ Exchange rate fetch failed:" error)
                (js/process.exit 1)))))

;; Run the main function
(main)