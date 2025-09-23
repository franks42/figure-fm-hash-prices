(ns crypto-app-v3.direct-api
  (:require [clojure.string :as str]))

(def ^:const TWELVE_DATA_API_KEY "b61354a1fe6f45a2a9e01c8c4145e617")

(defn direct-api-enabled? []
  (let [url-param (str/includes? (.-search js/window.location) "direct=true")
        local-storage (.getItem js/localStorage "enable-direct-api")
        enabled? (or url-param local-storage)]
    (js/console.log "🔍 PHASE 1: Feature flag check - URL:" url-param "LocalStorage:" local-storage "Result:" enabled?)
    enabled?))

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
  "Fetch FIGR stock quote from Twelve Data API"
  [symbol]
  (js/console.log "🚀 PHASE 1: Fetching Twelve Data quote for" symbol)
  (let [url (str "https://api.twelvedata.com/quote?symbol=" symbol "&apikey=" TWELVE_DATA_API_KEY)]
    (-> (js/fetch url)
        (.then (fn [response]
                 (js/console.log "📡 PHASE 1: Twelve Data response status:" (.-status response))
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "✅ PHASE 1: Twelve Data success for" symbol)
                 data)))))
