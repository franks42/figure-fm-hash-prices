(ns crypto-app-v3.direct-api
  (:require [clojure.string :as str]))

(def ^:const TWELVE_DATA_API_KEY "b61354a1fe6f45a2a9e01c8c4145e617")

(defn direct-api-enabled? []
  ;; REAL-TIME DATA IS THE DEFAULT - no URL parameters needed
  (js/console.log "🚀 LIVE DATA IS DEFAULT - real-time APIs always enabled")
  true)

(defn fetch-figure-markets
  "Fetch crypto data from Figure Markets API"
  []
  (js/console.log "🚀 PHASE 1: Fetching Figure Markets crypto data")
  (-> (js/fetch "https://www.figuremarkets.com/service-hft-exchange/api/v1/markets")
      (.then (fn [response]
               (js/console.log "📡 PHASE 1: Figure Markets response status:" (.-status response))
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (js/console.log "✅ PHASE 1: Figure Markets success - assets:" (count (.-data data)))
               data))))

(defn fetch-twelve-data-quote
  "Fetch stock quote from Twelve Data API"
  [symbol]
  (js/console.log "🚀 LIVE: Fetching Twelve Data quote for" symbol)
  (let [url (str "https://api.twelvedata.com/quote?symbol=" symbol "&apikey=" TWELVE_DATA_API_KEY)]
    (-> (js/fetch url)
        (.then (fn [response]
                 (js/console.log "📡 LIVE: Twelve Data response status:" (.-status response))
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "✅ LIVE: Twelve Data success for" symbol)
                 data)))))

(defn fetch-github-backup
  "Fetch backup data from GitHub Actions JSON"
  []
  (js/console.log "🔄 BACKUP: Fetching GitHub Actions data")
  (-> (js/fetch "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json")
      (.then (fn [response]
               (js/console.log "📡 BACKUP: GitHub response status:" (.-status response))
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (js/console.log "✅ BACKUP: GitHub data success")
               data))))
