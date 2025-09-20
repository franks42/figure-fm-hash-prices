;; Currency selector modal (copy V2 exactly)
(defn currency-selector-panel []
  (when @(rf/subscribe [:currency/show-selector?])
    (let [current-currency @(rf/subscribe [:currency/current])
          close-fn #(rf/dispatch [:currency/hide-selector])
          select-currency-fn (fn [currency-code]
                               (rf/dispatch [:currency/select currency-code]))]
      [modal-backdrop
       [modal-container
        [:div
         ;; Header
         [:div {:class "flex items-center justify-between mb-6"}
          [:div {:class "flex items-center"}
           [:span {:class "text-2xl mr-3"} "💱"]
           [:h2 {:class "text-xl font-bold text-white"} "Select Currency"]]
          [:button {:class "text-gray-400 hover:text-white text-2xl"
                    :on-click close-fn}
           "×"]]
         ;; Currency grid
         [:div {:class "grid grid-cols-2 gap-3 max-h-80 overflow-y-auto"}
          (doall (for [currency supported-currencies]
                   ^{:key (:code currency)}
                   [:button {:class (str "flex items-center justify-between p-3 rounded-lg border transition-colors "
                                         (if (= current-currency (:code currency))
                                           "bg-blue-600 border-blue-500 text-white"
                                           "bg-gray-800 border-gray-600 hover:bg-gray-700 text-gray-300"))
                             :on-click #(select-currency-fn (:code currency))}
                    [:div {:class "flex items-center"}
                     [:span {:class "text-lg mr-2"} (:symbol currency)]
                     [:div
                      [:div {:class "text-sm font-semibold"} (:code currency)]
                      [:div {:class "text-xs opacity-75"} (:name currency)]]]
                    (when (= current-currency (:code currency))
                      [:span {:class "text-blue-300"} "✓"])]))]]])))
