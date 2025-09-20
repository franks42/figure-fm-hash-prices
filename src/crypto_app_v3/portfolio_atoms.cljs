(ns crypto-app-v3.portfolio-atoms
  (:require [reagent.core :as r]))

;; Plain Reagent atoms for portfolio data (like V2) - avoid re-frame persistent maps
(def portfolio-atom (r/atom {}))           ; Simple holdings: crypto-id -> quantity  
(def show-portfolio-panel (r/atom nil))    ; crypto-id of asset whose modal is open, nil = closed

;; V2's exact working persistence functions
(defn save-portfolio-to-storage [portfolio-data]
  (try
    (js/console.log "💾 Saving portfolio to localStorage:" portfolio-data)
    (.setItem js/localStorage "crypto-portfolio-v3-atoms" (.stringify js/JSON (clj->js portfolio-data)))
    (js/console.log "✅ Portfolio saved successfully")
    true
    (catch :default e
      (js/console.warn "❌ Failed to save portfolio to localStorage:" e)
      false)))

(defn load-portfolio-from-storage []
  (try
    (js/console.log "📖 Loading portfolio from localStorage...")
    (let [stored-data (.getItem js/localStorage "crypto-portfolio-v3-atoms")]
      (js/console.log "📖 Raw stored data:" stored-data)
      (when stored-data
        (let [parsed-data (js->clj (.parse js/JSON stored-data))]
          (js/console.log "✅ Portfolio loaded successfully:" parsed-data)
          parsed-data)))
    (catch :default e
      (js/console.warn "❌ Failed to load portfolio from localStorage:" e)
      {})))

(defn persist-portfolio []
  (save-portfolio-to-storage @portfolio-atom))

(defn restore-portfolio []
  (let [stored-portfolio (load-portfolio-from-storage)]
    (when (seq stored-portfolio)
      (reset! portfolio-atom stored-portfolio))))

;; Portfolio calculations (V2 logic)
(defn calculate-holding-value [quantity current-price]
  (when (and quantity current-price (> quantity 0) (> current-price 0))
    (* quantity current-price)))
