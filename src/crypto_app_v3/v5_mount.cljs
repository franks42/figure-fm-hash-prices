(ns crypto-app-v3.v5-mount
  (:require [reagent.dom :as rdom]
            [crypto-app-v3.card-v5 :as card-v5]
            [crypto-app-v3.views :as views]))

;; V5 prototype mounting - separate from main app to avoid circular dependencies

(defn mount-v5-prototype!
  "Mount V5 prototype with modals in separate container"
  []
  (when-let [container (.getElementById js/document "v5-prototype")]
    (rdom/render [:<>
                  [card-v5/v5-prototype-section]
                  [views/currency-selector-panel]
                  [views/unified-portfolio-panel]] container)))

;; Auto-mount when this namespace loads
(mount-v5-prototype!)
