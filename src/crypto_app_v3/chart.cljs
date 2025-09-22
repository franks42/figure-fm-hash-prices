(ns crypto-app-v3.chart
  (:require [reagent.core :as r]
            [re-frame.core :as rf]))

;; Color calculation for price sentiment
(defn calculate-chart-colors
  "Calculate chart colors based on price sentiment (positive = green, negative = red)"
  [prices]
  (if (and (seq prices) (>= (count prices) 2))
    (let [start-price (first prices)
          end-price (last prices)
          is-positive? (> end-price start-price)]
      (if is-positive?
        {:stroke "#00ff88" :fill "rgba(0,255,136,0.4)"}  ; Green for positive
        {:stroke "#ff4d5a" :fill "rgba(255,77,90,0.4)"})) ; Red for negative
    {:stroke "#00ff88" :fill "rgba(0,255,136,0.4)"}))     ; Default green

;; Generic background chart for any crypto asset
(defn background-chart [crypto-id]
  (let [container-ref (r/atom nil)
        chart-instance (r/atom nil)]

    (r/create-class
     {:display-name "hash-chart"

      :component-did-mount
      (fn [_]
        (js/console.log "🎯 Chart component mounted"))

      :component-did-update
      (fn [_ _]
        (let [current-data @(rf/subscribe [:historical-data crypto-id])]
          (js/console.log "🔄 Chart update - data:" (pr-str current-data) "instance:" (some? @chart-instance))
          (when (and current-data @container-ref js/uPlot (not @chart-instance) (vector? current-data) (= (count current-data) 2))
            (let [[times prices] current-data]
              (when (and (seq times) (seq prices))
                (js/console.log "📊 Creating chart for" crypto-id "with" (count times) "points")
                (let [colors (calculate-chart-colors prices)
                      ;; Create trend line data (start and end points only)
                      _trend-times [(first times) (last times)]
                      trend-prices [(first prices) (last prices)]
                      instance (js/uPlot.
                                (clj->js {:width (.-offsetWidth @container-ref)
                                          :height 120
                                          :series [{}
                                                   {:stroke (:stroke colors)
                                                    :fill (:fill colors)
                                                    :width 4
                                                    :points {:show false}}
                                                   {:stroke "rgba(128,128,128,0.6)"
                                                    :width 2
                                                    :dash [8, 4]
                                                    :fill "rgba(0,0,0,0)"
                                                    :points {:show false}}]
                                          :axes [{:show false} {:show false}]
                                          :legend {:show false}
                                          :cursor {:show false}})
                                (clj->js [times prices trend-prices])
                                @container-ref)]
                  (reset! chart-instance instance)
                  (js/console.log "✅ Chart created successfully with sentiment colors!")))))))

      :reagent-render
      (fn []
        (let [current-data @(rf/subscribe [:historical-data crypto-id])
              has-data? (and current-data (vector? current-data) (not-empty (first current-data)))
              [_ prices] (or current-data [[] []])
              is-positive? (and prices (>= (count prices) 2) (> (last prices) (first prices)))]
          [:div {:class "absolute top-0 left-0 right-0 opacity-50 pointer-events-none"
                 :style {:height "120px"}
                 :ref (fn [el] (when el (reset! container-ref el)))}
           (when has-data?
             [:div {:class (str "absolute top-1 right-1 text-xs "
                                (if is-positive? "text-green-400" "text-red-400"))}
              (if is-positive? "📈" "📉")])]))})))
