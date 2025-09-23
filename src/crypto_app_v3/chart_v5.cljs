(ns crypto-app-v3.chart-v5
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as str]))

;; V5 Chart Component - Square Layout with HTML Overlays
;; This is the new chart implementation for the comprehensive redesign

;; Subliminal color gradient based on percentage change magnitude
(defn get-change-color
  "Returns color with intensity based on percentage change magnitude for subliminal messaging"
  [percent-change]
  (let [abs-change (js/Math.abs percent-change)
        ;; Cap at 10% - anything above 10% gets same max intensity (extreme)
        capped-change (js/Math.min abs-change 10.0)
        ;; Map 0% to 10% onto 0.1 to 0.8 opacity (seamless gradient)
        intensity (js/Math.min (* capped-change 0.08) 0.8) ; 0.08 = 8% per percent change
        intensity (js/Math.max intensity 0.3)] ; Much higher minimum visibility
    (cond
      (> percent-change 0.05) ; Positive > 0.05%
      {:text-color (str "rgba(0, 255, 65, " (js/Math.min (+ intensity 0.7) 1.0) ")") ; Bright neon green text
       :bg-color (str "rgba(0, 255, 65, " (* intensity 1.5) ")") ; Much more visible green background
       :border-color (str "rgba(0, 255, 65, " (* intensity 1.2) ")")} ; Strong green border

      (< percent-change -0.05) ; Negative < -0.05%  
      {:text-color (str "rgba(220, 38, 38, " (js/Math.min (+ intensity 0.7) 1.0) ")") ; Bright red text
       :bg-color (str "rgba(220, 38, 38, " (* intensity 1.5) ")") ; Much more visible red background  
       :border-color (str "rgba(220, 38, 38, " (* intensity 1.2) ")")} ; Strong red border

      :else ; Nearly flat (-0.05% to +0.05%)
      {:text-color "rgba(156, 163, 175, 0.8)" ; Neutral gray
       :bg-color "rgba(107, 114, 128, 0.3)" ; More visible gray background
       :border-color "rgba(107, 114, 128, 0.2)"})))

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
        chart-instance (r/atom nil)]
    (r/create-class
     {:display-name "square-chart-v5"
      :component-did-mount
      (fn [_]
        (js/console.log "🎯 V5 Chart component mounted for" crypto-id))
      :component-did-update
      (fn [_ _]
        (let [current-data @(rf/subscribe [:historical-data crypto-id])]
          (js/console.log "🔄 V5 Chart update for" crypto-id)
          ;; Destroy existing chart to force recreation with new period data
          (when @chart-instance
            (js/console.log "🗑️ Destroying existing chart for period update")
            (.destroy @chart-instance)
            (reset! chart-instance nil))
          ;; Create chart when we have data and container
          (when (and current-data @container-ref js/uPlot
                     (vector? current-data) (= (count current-data) 2))
            (let [[times prices] current-data]
              (when (and (seq times) (seq prices))
                (js/console.log "📊 Creating V5 square chart for" crypto-id "with" (count times) "points")
           ;; Calculate subliminal gradient colors based on percentage change
                (let [start-price (first prices)
                      end-price (last prices)
                      pct-change (* 100 (/ (- end-price start-price) start-price))
                      _ (js/console.log "🔴🟢 CHART-GRADIENT" crypto-id "Start:" start-price "End:" end-price "Pct:" pct-change)
                      gradient-colors (get-change-color pct-change)
           ;; White stroke color for all charts (clean line)
                      stroke-color "#ffffff"
                       ;; Background fill uses the subtle background color from gradient
                      fill-color (:bg-color gradient-colors)
;; Build trend line data
                      trend-data (let [n (count times)]
                                   (mapv (fn [i]
                                           (cond
                                             (= i 0) start-price
                                             (= i (dec n)) end-price
                                             :else nil)) (range n)))
;; Make container square
                      size (min (.-offsetWidth @container-ref) (.-offsetHeight @container-ref))
                      instance (js/uPlot.
                                (clj->js {:width size
                                          :height size
                                          :series [{}
                                                   {:stroke "#888"
                                                    :width 2
                                                    :dash [4, 4]
                                                    :spanGaps true
                                                    :points {:show false}}
                                                   {:stroke stroke-color
                                                    :fill fill-color
                                                    :width 3
                                                    :points {:show false}}]
                                          :axes [{:show false} {:show false}]
                                          :legend {:show false}
                                          :cursor {:show false}})
                                (clj->js [times trend-data prices])
                                @container-ref)]
                  (reset! chart-instance instance)
                  (js/console.log "✅ V5 Square chart created successfully!")))))))
      :reagent-render
      (fn []
        (let [current-data @(rf/subscribe [:historical-data crypto-id])
              has-data? (and current-data (vector? current-data) (not-empty (first current-data)))]
          (let [[times prices] (or current-data [nil nil])
                bg-color (if (and prices (> (count prices) 1))
                           ;; Calculate gradient background color for visible subliminal effect
                           (let [start-price (first prices)
                                 end-price (last prices)
                                 pct-change (* 100 (/ (- end-price start-price) start-price))]
                             (:bg-color (get-change-color pct-change)))
                           ;; Default background
                           "rgba(17, 24, 39, 0.2)")]
            [:div {:class "relative w-full bg-gray-900/20 rounded-lg border border-white/10"
                   :style {:aspect-ratio "1"}
                   :ref (fn [el] (when el (reset! container-ref el)))}
             [:div {:class "absolute inset-2"}
              (if has-data?
                [:div {:class "w-full h-full"}]
                [:div {:class "w-full h-full bg-gradient-to-br from-gray-800/20 to-gray-600/20 rounded flex items-center justify-center"}
                 [:div {:class "text-white/40 text-sm"} "Loading chart..."]])]])))})))

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
  [price currency-symbol currency-code]
  [:div {:class "absolute left-2 top-1/2 -translate-y-1/2 flex items-center overlay-tier1"}
   [:span {:class "text-lg font-bold text-white mr-2"}
    (str currency-symbol (format-number price 3))]
   [:button {:class "text-xs bg-white/10 hover:bg-white/20 border border-white/20 rounded px-1 py-0.5 text-white/80 cursor-pointer transition-colors"
             :on-click #(do (.stopPropagation %) (rf/dispatch [:currency/show-selector]))}
    currency-code]])

(defn chart-overlay-change
  "Change percentage in right-middle with subliminal gradient intensity"
  [change-percent]
  (let [positive? (> change-percent 0)
        colors (get-change-color change-percent)
        arrow (if positive? "▲" "▼")]
    [:div {:class "absolute right-2 top-1/2 -translate-y-1/2 overlay-tier1"}
     [:div {:class "px-2 py-1 rounded-lg border transition-all duration-300"
            :style {:background-color (:bg-color colors)
                    :border-color (:border-color colors)}}
      [:span {:class "text-sm font-semibold text-white"}
       (str arrow (format-number (js/Math.abs change-percent) 2) "%")]]]))

(defn chart-overlay-period
  "Period selector in bottom-center - GLOBAL like currency"
  [current-period]
  [:div {:class "absolute bottom-2 inset-x-0 flex justify-center z-30"}
   [:button {:class "text-xs bg-blue-900/80 hover:bg-blue-800/80 border border-blue-600/60 rounded px-2 py-1 text-white overlay-tier3 cursor-pointer transition-colors"
             :on-click #(rf/dispatch [:chart/cycle-period])}
    current-period]])

(defn chart-overlay-low
  "Low price in bottom-right corner"
  [low-price currency-symbol]
  [:div {:class "absolute bottom-2 right-2 text-xs text-white/70 overlay-tier2"}
   (str currency-symbol (format-number low-price 3))])

;; Individual overlay components - used by card_v5.cljs
