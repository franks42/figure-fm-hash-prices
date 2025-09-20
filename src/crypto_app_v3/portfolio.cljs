(ns crypto-app-v3.portfolio
  (:require [re-frame.core :as rf]))

;; Copy V2 portfolio persistence (small functions)

(defn save-portfolio-to-storage [portfolio-data]
  "Save portfolio data to localStorage"
  (try
    (js/console.log "💾 V3 Saving portfolio to localStorage:" portfolio-data)
    (.setItem js/localStorage "crypto-portfolio-v3" (.stringify js/JSON (clj->js portfolio-data)))
    (js/console.log "✅ V3 Portfolio saved successfully")
    true
    (catch :default e
      (js/console.warn "❌ V3 Failed to save portfolio to localStorage:" e)
      false)))

(defn load-portfolio-from-storage []
  "Load portfolio data from localStorage"
  (try
    (js/console.log "📖 V3 Loading portfolio from localStorage...")
    (let [stored-data (.getItem js/localStorage "crypto-portfolio-v3")]
      (js/console.log "📖 V3 Raw stored data:" stored-data)
      (when stored-data
        (let [parsed-data (js->clj (.parse js/JSON stored-data))]
          (js/console.log "✅ V3 Portfolio loaded successfully:" parsed-data)
          parsed-data)))
    (catch :default e
      (js/console.warn "❌ V3 Failed to load portfolio from localStorage:" e)
      {})))

;; Portfolio effects
(rf/reg-fx
 :portfolio/save
 (fn [portfolio-data]
   (save-portfolio-to-storage portfolio-data)))

(rf/reg-fx
 :portfolio/load
 (fn [_]
   (let [loaded-portfolio (load-portfolio-from-storage)]
     (when (seq loaded-portfolio)
       (rf/dispatch [:portfolio/restore loaded-portfolio])))))

;; Portfolio events with persistence
(rf/reg-event-fx
 :portfolio/update-and-save
 (fn [{:keys [db]} [_ crypto-id quantity]]
   (let [updated-holdings (if (and quantity (> quantity 0))
                            (assoc (get-in db [:portfolio :holdings]) crypto-id quantity)
                            (dissoc (get-in db [:portfolio :holdings]) crypto-id))]
     {:db (assoc-in db [:portfolio :holdings] updated-holdings)
      :portfolio/save updated-holdings})))

(rf/reg-event-fx
 :portfolio/clear-and-save
 (fn [cofx _]
   {:db (assoc-in (:db cofx) [:portfolio :holdings] {})
    :portfolio/save {}}))

(rf/reg-event-db
 :portfolio/restore
 (fn [db [_ loaded-portfolio]]
   (assoc-in db [:portfolio :holdings] loaded-portfolio)))

;; Startup event
(rf/reg-event-fx
 :portfolio/initialize
 (fn [_ _]
   {:portfolio/load nil}))
