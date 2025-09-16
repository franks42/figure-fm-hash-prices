(ns crypto-app.effects
  (:require [crypto-app.state :as state]))

;; API data fetching and side effects

(defn show-fetch-indicator []
  "Shows the top scan line during data fetching"
  (let [indicator (js/document.getElementById "fetch-indicator")]
    (when indicator
      (.add (.-classList indicator) "active"))))

(defn hide-fetch-indicator []
  "Hides the top scan line after data fetching completes"
  (js/setTimeout
   (fn []
     (let [indicator (js/document.getElementById "fetch-indicator")]
       (when indicator
         (.remove (.-classList indicator) "active"))))
   state/SCAN_HIDE_DELAY_MS))

(defn flash-update-indicator []
  "Shows brief flash indicator to signal data was updated"
  (reset! state/update-flash-atom true)
  (js/setTimeout #(reset! state/update-flash-atom false) state/FLASH_DURATION_MS))

(defn update-timestamp []
  "Updates the last-update timestamp to current time"
  (let [now (js/Date.)
        formatted-time (.toISOString now)]
    (reset! state/last-update-atom (.replace formatted-time "T" " "))))

(defn process-data-changes [old-prices new-prices]
  "Compares old and new price data, returns map of changes"
  (into {}
        (for [[coin-id new-data] new-prices
              :let [old-data (get old-prices coin-id)
                    old-price (get old-data "usd")
                    new-price (get new-data "usd")
                    old-change (get old-data "usd_24h_change")
                    new-change (get new-data "usd_24h_change")
                    old-volume (get old-data "usd_24h_vol")
                    new-volume (get new-data "usd_24h_vol")
                    old-bid (get old-data "bid")
                    new-bid (get new-data "bid")
                    old-ask (get old-data "ask")
                    new-ask (get new-data "ask")]
              :when (or (not= old-price new-price)
                        (not= old-change new-change)
                        (not= old-volume new-volume)
                        (not= old-bid new-bid)
                        (not= old-ask new-ask))]
          [coin-id new-data])))

(defn update-price-data [js-data]
  "Updates state atoms with new price data using batched updates"
  (let [prices (dissoc js-data "timestamp" "source" "last_update")
        old-prices @state/prices-atom
        new-keys (set (keys prices))
        old-keys (set @state/price-keys-atom)
        changes (process-data-changes old-prices prices)]

    ;; Single batched update - triggers only one re-render
    (when (seq changes)
      (swap! state/prices-atom merge changes))

    ;; Only update keys if they actually changed
    (when (not= new-keys old-keys)
      (reset! state/price-keys-atom (keys prices)))))

(defn handle-fetch-success [data]
  "Handles successful API data fetch"
  (let [js-data (js->clj data :keywordize-keys false)]
    ;; Update price data with batched changes
    (update-price-data js-data)

    ;; Always update timestamp and flash indicator
    (update-timestamp)
    (flash-update-indicator)
    (hide-fetch-indicator)

    ;; Update loading state only for initial load
    (when (not @state/initial-load-complete)
      (reset! state/initial-load-complete true)
      (when @state/loading-atom
        (reset! state/loading-atom false)))))

(defn handle-fetch-error [error]
  "Handles API fetch errors"
  (js/console.error "Failed to fetch crypto data:" error)

  ;; Hide fetch indicator and update error state
  (hide-fetch-indicator)
  (reset! state/loading-atom false)
  (reset! state/error-atom (.-message error)))

(defn fetch-crypto-data []
  "Fetches cryptocurrency data from the API"
  ;; Show visual feedback
  (show-fetch-indicator)

  ;; Only show loading states for initial load
  (when (not @state/initial-load-complete)
    (when (not @state/loading-atom)
      (reset! state/loading-atom true))
    (when @state/error-atom
      (reset! state/error-atom nil)))

  ;; Fetch data from GitHub raw data branch
  (-> (js/fetch (str "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json?t=" (js/Date.now)))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then handle-fetch-success)
      (.catch handle-fetch-error)))

(defn setup-timeout-handler []
  "Sets up timeout error if loading takes too long"
  (js/setTimeout
   (fn []
     (when @state/loading-atom
       (reset! state/loading-atom false)
       (reset! state/error-atom "Timeout loading data")))
   state/TIMEOUT_MS))

(defn start-polling []
  "Starts the data polling interval"
  (fetch-crypto-data)
  (js/setInterval fetch-crypto-data state/POLL_INTERVAL_MS))
