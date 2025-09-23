(ns crypto-app-v3.v5-mount
  (:require [reagent.dom :as rdom]
            [crypto-app-v3.card-v5 :as card-v5]
            [crypto-app-v3.views :as views]))

;; V5 prototype mounting - separate from main app to avoid circular dependencies

(defn mount-v5-prototype!
  "Mount V5 prototype with modals in separate container"
  []
  ;; Mount main V5 content
  (when-let [container (.getElementById js/document "v5-prototype")]
    (rdom/render [card-v5/v5-prototype-section] container))

  ;; Mount modals in dedicated root to avoid CSS clipping
  (when-let [modal-root (.getElementById js/document "modal-root")]
    (rdom/render [:<>
                  [views/currency-selector-panel]
                  [views/unified-portfolio-panel]] modal-root)))

;; Auto-mount when this namespace loads
(mount-v5-prototype!)
