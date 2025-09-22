;; V5 Portfolio panel using re-frame events
(defn portfolio-panel-v5 []
  (when-let [crypto-id @(rf/subscribe [:portfolio/panel-crypto-id])]
    (let [symbol (get-crypto-symbol crypto-id)
          icon (get-crypto-icon crypto-id)
          current-quantity @(rf/subscribe [:portfolio/qty crypto-id])
          holding-value @(rf/subscribe [:portfolio/value crypto-id])
          close-fn #(rf/dispatch [:portfolio/hide-panel])
          save-fn #(do
                     (let [input-val (-> js/document (.getElementById "quantity-input") .-value js/parseFloat)]
                       (if (and input-val (not (js/isNaN input-val)) (> input-val 0))
                         (rf/dispatch [:portfolio/set-qty crypto-id input-val])
                         (rf/dispatch [:portfolio/remove crypto-id]))
                       (rf/dispatch [:portfolio/hide-panel])))
          header [modal-header icon symbol close-fn]
          holdings [holdings-display current-quantity holding-value crypto-id]
          form [input-form current-quantity save-fn close-fn]
          content [:div {:class "space-y-4"} holdings form]]
      [modal-backdrop
       [modal-container
        [:div header content]]])))

;; Unified portfolio panel that works with both V4 and V5
(defn unified-portfolio-panel []
  (let [new-layout? @(rf/subscribe [:ui/new-layout?])]
    (if new-layout?
      [portfolio-panel-v5]
      [portfolio-panel])))
