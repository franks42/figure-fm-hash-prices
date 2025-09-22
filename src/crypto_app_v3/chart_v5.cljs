(ns crypto-app-v3.chart-v5
  (:require [reagent.core :as r]
            [clojure.string :as str]))

;; V5 Chart Component - Square Layout with HTML Overlays
;; This is the new chart implementation for the comprehensive redesign

;; Utility functions
(defn format-number
  "Format number with specified decimal places"
  [n decimals]
  (if (number? n)
    (.toLocaleString n "en-US"
                     #js{:minimumFractionDigits decimals
                         :maximumFractionDigits decimals})
    "0"))

(defn square-chart-container
  [crypto-id]
  (let [container-ref (r/atom nil)
        _chart-instance (r/atom nil)]
    (r/create-class
     {:display-name "square-chart-v5"
      :component-did-mount
      (fn [_]
        (js/console.log "🎯 V5 Chart component mounted for" crypto-id))
      :component-did-update
      (fn [_ _]
        (js/console.log "🔄 V5 Chart update for" crypto-id))
      :reagent-render
      (fn []
        [:div {:class "relative w-full bg-gray-900/20 rounded-lg border border-white/10"
               :style {:aspect-ratio "1"}
               :ref (fn [el] (when el (reset! container-ref el)))}
         [:div {:class "absolute inset-2"}
          [:div {:class "w-full h-full bg-gradient-to-br from-gray-800/20 to-gray-600/20 rounded flex items-center justify-center"}
           [:div {:class "text-white/40 text-sm"} "V5 Square Chart"]]]])})))

;; Chart overlays - positioned absolutely over the chart
(defn chart-overlay-symbol
  "Asset symbol in upper-left corner"
  [crypto-id]
  [:div {:class "absolute top-2 left-2 text-sm font-bold text-white/90 overlay-tier2"}
   (str/upper-case (name crypto-id))])

(defn chart-overlay-high
  "High price in upper-right corner"
  [high-price currency-symbol]
  [:div {:class "absolute top-2 right-2 text-xs text-white/70 overlay-tier2"}
   (str currency-symbol (format-number high-price 3))])

(defn chart-overlay-current-price
  "Current price + currency button in left-middle"
  [price currency-code]
  [:div {:class "absolute left-2 top-1/2 -translate-y-1/2 flex items-center overlay-tier1"}
   [:span {:class "text-lg font-bold text-white mr-2"}
    (str "$" (format-number price 3))]
   [:button {:class "text-xs bg-white/10 border border-white/20 rounded px-1 py-0.5 text-white/80"}
    currency-code]])

(defn chart-overlay-change
  "Change percentage in right-middle"
  [change-percent]
  (let [positive? (>= change-percent 0)]
    [:div {:class "absolute right-2 top-1/2 -translate-y-1/2 overlay-tier1"}
     [:span {:class (str "text-sm font-semibold "
                         (if positive? "text-neon-green" "text-neon-red"))}
      (str (if positive? "▲" "▼") (format-number (js/Math.abs change-percent) 2) "%")]]))

(defn chart-overlay-period
  "Period selector in bottom-left corner"
  [current-period]
  [:div {:class "absolute bottom-2 left-2"}
   [:button {:class "text-xs bg-white/10 border border-white/20 rounded px-2 py-1 text-white/80 overlay-tier3"}
    current-period]])

(defn chart-overlay-low
  "Low price in bottom-right corner"
  [low-price currency-symbol]
  [:div {:class "absolute bottom-2 right-2 text-xs text-white/70 overlay-tier2"}
   (str currency-symbol (format-number low-price 3))])

;; Individual overlay components - used by card_v5.cljs
