#!/usr/bin/env nbb

(require '[cljs.core :as cljs])
(require '[clojure.string :as str])

;; Exchange rate fetching script for GitHub Actions
;; Fetches USD exchange rates to various currencies using exchangerate.host (free, no API key needed)

(def supported-currencies ["EUR" "GBP" "JPY" "CAD" "AUD" "CHF" "CNY" "KRW" "SEK"])

(defn fetch-exchange-rates []
  "Fetch current USD exchange rates"
  (let [base-url "https://api.exchangerate.host/latest"
        params (str "?base=USD&symbols=" (str/join "," supported-currencies))]
    (-> (js/fetch (str base-url params))
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "✅ Exchange rates fetched successfully")
                 (js->clj data :keywordize-keys true))))))

(defn save-exchange-rates [rates-data]
  "Save exchange rates to JSON file"
  (let [fs (js/require "fs")
        path "data/exchange-rates.json"
        rates (:rates rates-data)
        timestamp (js/Date.now)
        output {:rates rates
                :timestamp timestamp
                :base "USD"
                :source "exchangerate.host"
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