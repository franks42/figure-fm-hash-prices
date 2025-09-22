      :component-did-update  
      (fn [_ _]
        (let [current-data @(rf/subscribe [:historical-data crypto-id])]
          (js/console.log "🔄 V5 Chart update for" crypto-id)
          (when (and current-data (vector? current-data) (= (count current-data) 2))
            (let [[times prices] current-data]
              (when (and (seq times) (seq prices))
                (let [start-price (first prices)
                      end-price (last prices)
                      is-positive? (> end-price start-price)
                      stroke-color (if is-positive? "#00ff88" "#ff4d5a")
                      fill-color (if is-positive? "rgba(0,255,136,0.4)" "rgba(255,77,90,0.4)")
                      trend-data (let [n (count times)]
                                   (mapv (fn [i]
                                          (cond
                                            (= i 0) start-price
                                            (= i (dec n)) end-price
                                            :else nil)) (range n)))]
                  (if @chart-instance
                    ;; Update existing chart
                    (do 
                      (js/console.log "🔄 Updating existing V5 chart colors")
                      (.setData @chart-instance (clj->js [times trend-data prices]))
                      (.setSeries @chart-instance 2 (clj->js {:stroke stroke-color :fill fill-color :width 3 :points {:show false}})))
                    ;; Create new chart
                    (when (and @container-ref js/uPlot)
                      (js/console.log "📊 Creating new V5 square chart")
                      (let [size (min (.-offsetWidth @container-ref) (.-offsetHeight @container-ref))
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
                        (js/console.log "✅ V5 Square chart created successfully!"))))))))
