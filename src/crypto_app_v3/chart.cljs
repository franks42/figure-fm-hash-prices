(ns crypto-app-v3.chart
  (:require [reagent.core :as r]
            [re-frame.core :as rf]))

;; Oracle's minimal working chart implementation
(defn hash-background-chart []
  (let [chart-data @(rf/subscribe [:historical-data "hash"])
        container-ref (r/atom nil)
        chart-instance (r/atom nil)]
    
    (r/create-class
     {:display-name "hash-chart"
      
      :component-did-mount
      (fn [_]
        (js/console.log "🎯 Chart component mounted - data available:" (some? chart-data) "data type:" (type chart-data))
        (js/console.log "🔍 Raw chart-data:" (pr-str chart-data))
        (js/console.log "🔍 Chart-data as js:" (clj->js chart-data))
        ;; Try to create chart immediately if data is already available
        (js/setTimeout
         (fn []
           (when (and chart-data @container-ref js/uPlot (not @chart-instance))
             (let [[times prices] chart-data]
               (when (and (seq times) (seq prices))
                 (js/console.log "📊 Creating chart immediately with" (count times) "points")
                 (let [instance (js/uPlot.
                                 (clj->js {:width (.-offsetWidth @container-ref)
                                          :height 120
                                          :series [{}  ; Time series
                                                   {:stroke "#00ff88"  ; Bright green
                                                    :fill "rgba(0,255,136,0.4)"
                                                    :width 4  ; Thick line for visibility
                                                    :points {:show false}}]
                                          :axes [{:show false} {:show false}]
                                          :legend {:show false}
                                          :cursor {:show false}})
                                 (clj->js [times prices])  ; Feed data immediately
                                 @container-ref)]
                   (reset! chart-instance instance)
                   (js/console.log "✅ Chart created on mount with data"))))))
         50))
      
      :component-did-update
      (fn [_ _]
        (let [current-data @(rf/subscribe [:historical-data "hash"])]
          (js/console.log "🔄 Chart update - data available:" (some? current-data) "instance:" (some? @chart-instance))
          (when (and current-data @container-ref js/uPlot (not @chart-instance) (vector? current-data) (= (count current-data) 2))
            ;; Create chart only when data is available
            (let [[times prices] current-data]
              (when (and (seq times) (seq prices))
                (js/console.log "📊 Creating chart in UPDATE with" (count times) "points")
                (let [instance (js/uPlot.
                                (clj->js {:width (.-offsetWidth @container-ref)
                                         :height 120
                                         :series [{}  ; Time axis
                                                  {:stroke "#00ff88"  ; Bright green
                                                   :fill "rgba(0,255,136,0.4)"
                                                   :width 4  ; Extra thick for visibility
                                                   :points {:show false}}]
                                         :axes [{:show false} {:show false}]
                                         :legend {:show false}
                                         :cursor {:show false}})
                                (clj->js [times prices])  ; Feed data immediately
                                @container-ref)]
                  (reset! chart-instance instance)
                  (js/console.log "✅ Chart rendered in UPDATE with real data!"))))))
      
      :reagent-render
      (fn []
        ;; Force re-render by dereferencing current data  
        (let [current-data @(rf/subscribe [:historical-data "hash"])
              has-data? (and current-data (vector? current-data) (not-empty (first current-data)))]
          (js/console.log "🔄 Chart render - current data:" (pr-str current-data) "has-data?" has-data?)
          [:div {:class "absolute top-0 left-0 right-0 opacity-50 pointer-events-none"
                 :style {:height "120px"}
                 :ref (fn [el] (when el (reset! container-ref el)))}
           (when has-data?
             [:div {:class "absolute top-1 right-1 text-xs text-green-400"} "📈"])]))})))
