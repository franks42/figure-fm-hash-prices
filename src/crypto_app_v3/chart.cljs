(ns crypto-app-v3.chart
  (:require [reagent.core :as r]
            [re-frame.core :as rf]))

;; Simple working chart implementation
(defn hash-background-chart []
  (let [container-ref (r/atom nil)
        chart-instance (r/atom nil)]
    
    (r/create-class
     {:display-name "hash-chart"
      
      :component-did-mount
      (fn [_]
        (js/console.log "🎯 Chart component mounted"))
      
      :component-did-update
      (fn [_ _]
        (let [current-data @(rf/subscribe [:historical-data "hash"])]
          (js/console.log "🔄 Chart update - data:" (pr-str current-data) "instance:" (some? @chart-instance))
          (when (and current-data @container-ref js/uPlot (not @chart-instance) (vector? current-data) (= (count current-data) 2))
            (let [[times prices] current-data]
              (when (and (seq times) (seq prices))
                (js/console.log "📊 Creating chart in UPDATE with" (count times) "points")
                (let [instance (js/uPlot.
                                (clj->js {:width (.-offsetWidth @container-ref)
                                         :height 120
                                         :series [{}
                                                  {:stroke "#00ff88"
                                                   :fill "rgba(0,255,136,0.4)"
                                                   :width 4
                                                   :points {:show false}}]
                                         :axes [{:show false} {:show false}]
                                         :legend {:show false}
                                         :cursor {:show false}})
                                (clj->js [times prices])
                                @container-ref)]
                  (reset! chart-instance instance)
                  (js/console.log "✅ Chart created successfully!")))))))
      
      :reagent-render
      (fn []
        (let [current-data @(rf/subscribe [:historical-data "hash"])
              has-data? (and current-data (vector? current-data) (not-empty (first current-data)))]
          [:div {:class "absolute top-0 left-0 right-0 opacity-50 pointer-events-none"
                 :style {:height "120px"}
                 :ref (fn [el] (when el (reset! container-ref el)))}
           (when has-data?
             [:div {:class "absolute top-1 right-1 text-xs text-green-400"} "📈"])]))})))
