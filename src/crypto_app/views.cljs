(ns crypto-app.views
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [crypto-app.state :as state]
            [crypto-app.portfolio :as portfolio]))

;; Utility functions for formatting
(defn format-number [n decimals]
  (.toLocaleString n "en-US"
                   #js{:minimumFractionDigits decimals
                       :maximumFractionDigits decimals}))

(defn format-price [price crypto-id]
  (let [decimals (if (= crypto-id "hash") 3 2)]
    (str "$" (format-number price decimals))))

(defn format-volume [vol]
  (cond
    (>= vol 1e9) (str "$" (format-number (/ vol 1e9) 2) "B")
    (>= vol 1e6) (str "$" (format-number (/ vol 1e6) 2) "M")
    :else (str "$" (format-number vol 0))))

(defn format-market-cap [cap]
  (format-volume cap))

;; Portfolio display components (compositional approach)
(defn portfolio-value-display [holding-value crypto-id]
  (when holding-value
    [:div {:class "bg-blue-500/10 border border-blue-500/20 rounded-lg p-3 mt-4"}
     [:div {:class "text-xs text-blue-400 uppercase mb-1 tracking-widest"} "Portfolio Value"]
     [:div {:class "text-lg font-bold text-blue-300 tabular-nums"} (format-price holding-value crypto-id)]]))

(defn portfolio-summary-header []
  (let [total-value (portfolio/get-total-portfolio-value @state/portfolio-atom @state/prices-atom)]
    (when (> total-value 0)
      [:div {:class "bg-gradient-to-r from-blue-500/10 to-purple-500/10 border border-blue-500/20 rounded-2xl p-6 mb-8"}
       [:div {:class "text-center"}
        [:div {:class "text-sm text-blue-400 uppercase mb-2 tracking-widest"} "Total Portfolio Value"]
        [:div {:class "text-4xl font-bold text-white tabular-nums"} (str "$" (format-number total-value 2))]]])))

;; UI Components
(defn crypto-card [crypto-id]
  (let [data @(r/cursor state/prices-atom [crypto-id])
        name (str/upper-case crypto-id)
        symbol (get state/crypto-symbols (keyword crypto-id) (str/upper-case crypto-id))
        icon (get state/crypto-icons (keyword crypto-id) "◆")
        price (get data "usd")
        change (get data "usd_24h_change")
        volume (get data "usd_24h_vol")
        bid (get data "bid")
        ask (get data "ask")
        trades-24h (get data "trades_24h")
        ;; Enhanced FIGR stock data
        is-stock? (= (get data "type") "stock")
        company-name (get data "company_name")
        exchange (get data "exchange")
        open-price (get data "open")
        day-high (get data "day_high")
        day-low (get data "day_low")
        fifty-two-week-high (get data "fifty_two_week_high")
        fifty-two-week-low (get data "fifty_two_week_low")
        previous-close (get data "previous_close")
        ;; Portfolio data
        quantity @(r/cursor state/portfolio-atom [crypto-id])
        holding-value (when quantity (portfolio/calculate-holding-value quantity price))
        ;; Display logic
        positive? (>= (or change 0) 0)
        arrow (if positive? "▲" "▼")
        change-classes (if positive?
                         "bg-neon-green/10 text-neon-green border-neon-green/20"
                         "bg-neon-red/10 text-neon-red border-neon-red/20")]
    [:div {:class "relative bg-white/[0.03] border border-white/10 rounded-3xl p-6 backdrop-blur-lg transition-all duration-300 ease-out hover:scale-[1.02] hover:-translate-y-1 hover:bg-white/[0.06] hover:border-white/20 hover:shadow-2xl hover:shadow-purple-500/10 scan-line overflow-hidden animate-fade-in"}
     [:div {:class "flex items-center justify-between mb-5"}
      [:div {:class "flex items-center"}
       [:div {:class "w-11 h-11 rounded-xl mr-4 flex items-center justify-center text-2xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 border border-white/10"}
        icon]
       [:div
        [:div {:class "text-lg font-semibold text-white tracking-wide"}
         (if is-stock? company-name name)
         [:span {:class "text-sm text-gray-500 ml-2 uppercase"} symbol]]
        (when (and is-stock? exchange)
          [:div {:class "text-xs text-gray-500 mt-0.5"} exchange])]]]
     [:div {:class "text-4xl font-bold mb-4 tabular-nums tracking-tight"} (format-price price crypto-id)]
     [:div {:class (str "inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold mb-5 border " change-classes)}
      [:span {:class "mr-1.5 text-base"} arrow]
      (str (format-number (js/Math.abs (or change 0)) 2) "%")]
     ;; Main stats grid - different for stocks vs crypto
     (if is-stock?
       ;; Stock-specific stats
       [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Open"]
         [:div {:class "text-base font-semibold text-white tabular-nums"}
          (if open-price (format-price open-price crypto-id) "N/A")]]
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Volume"]
         [:div {:class "text-base font-semibold text-white tabular-nums"} (format-volume (or volume 0))]]]
       ;; Crypto-specific stats  
       [:div {:class "grid grid-cols-2 gap-4 mt-5 pt-5 border-t border-white/5"}
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Volume"]
         [:div {:class "text-base font-semibold text-white tabular-nums"} (format-volume (or volume 0))]]
        [:div {:class "flex flex-col"}
         [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "24h Trades"]
         [:div {:class "text-base font-semibold text-white tabular-nums"} (format-number (or trades-24h 0) 0)]]])
     ;; Bid/Ask for crypto or 52-week range for stocks
     (if is-stock?
       ;; 52-week high/low for stocks
       (when (and fifty-two-week-high fifty-two-week-low)
         [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "52W High"]
           [:div {:class "text-sm font-semibold text-green-400 tabular-nums"} (format-price fifty-two-week-high crypto-id)]]
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "52W Low"]
           [:div {:class "text-sm font-semibold text-red-400 tabular-nums"} (format-price fifty-two-week-low crypto-id)]]])
       ;; Bid/Ask for crypto
       (when (and bid ask (> bid 0) (> ask 0))
         [:div {:class "grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-white/5"}
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Bid"]
           [:div {:class "text-sm font-semibold text-green-400 tabular-nums"} (format-price bid crypto-id)]]
          [:div {:class "flex flex-col"}
           [:div {:class "text-xs text-gray-500 uppercase mb-1.5 tracking-widest"} "Ask"]
           [:div {:class "text-sm font-semibold text-red-400 tabular-nums"} (format-price ask crypto-id)]]]))
     ;; Portfolio value display  
      (portfolio-value-display holding-value crypto-id)
      ;; Portfolio button
     [:div {:class "flex mt-4"}
      [:button {:class "flex-1 bg-white/[0.05] hover:bg-white/[0.10] border border-white/20 rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
                :onClick #(reset! state/show-portfolio-panel crypto-id)}
       "📊 Portfolio"]]]))

;; Portfolio modal components (compositional approach)
(defn modal-backdrop [content]
  [:div {:class "fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"}
   content])

(defn modal-container [content]
  [:div {:class "bg-gray-900 border border-white/20 rounded-2xl p-6 max-w-md w-full"}
   content])

(defn modal-header [icon symbol close-fn]
  [:div {:class "flex items-center justify-between mb-6"}
   [:div {:class "flex items-center"}
    [:span {:class "text-2xl mr-3"} icon]
    [:h2 {:class "text-xl font-bold text-white"} symbol " Portfolio"]]
   [:button {:class "text-gray-400 hover:text-white text-2xl"
             :onClick close-fn}
    "×"]])

(defn holdings-display [current-quantity holding-value crypto-id]
  [:div {:class "text-center"}
   [:div {:class "text-sm text-gray-400"} "Current Holdings"]
   [:div {:class "text-2xl font-bold text-white"} (format-number current-quantity 4)]
   (when holding-value
     [:div {:class "text-lg text-gray-300 mt-1"} (format-price holding-value crypto-id)])])

(defn quantity-input [current-quantity]
  [:div {:class "flex flex-col space-y-2"}
   [:label {:class "text-sm text-gray-400"} "Enter Quantity:"]
   [:input {:type "number" 
            :step "0.0001"
            :placeholder "0.0000"
            :class "bg-gray-800 border border-white/20 rounded-lg px-3 py-2 text-white text-center focus:outline-none focus:border-white/40"
            :defaultValue current-quantity
            :id "quantity-input"}]])

(defn action-buttons [save-fn cancel-fn]
  [:div {:class "flex space-x-2"}
   [:button {:class "flex-1 bg-green-600 hover:bg-green-700 text-white rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
             :onClick save-fn}
    "Save"]
   [:button {:class "flex-1 bg-gray-600 hover:bg-gray-700 text-white rounded-lg px-4 py-2 text-sm font-semibold transition-colors"
             :onClick cancel-fn}
    "Cancel"]])

(defn input-form [current-quantity save-fn cancel-fn]
  [:div {:class "border-t border-white/10 pt-4 space-y-3"}
   (quantity-input current-quantity)
   (action-buttons save-fn cancel-fn)])

(defn portfolio-panel []
  (when-let [crypto-id @state/show-portfolio-panel]
    (let [symbol (get state/crypto-symbols (keyword crypto-id) (str/upper-case crypto-id))
          icon (get state/crypto-icons (keyword crypto-id) "◆")
          current-quantity (get @state/portfolio-atom crypto-id 0)
          price (get-in @state/prices-atom [crypto-id "usd"] 0)
          holding-value (when (> current-quantity 0) (portfolio/calculate-holding-value current-quantity price))
          close-fn #(reset! state/show-portfolio-panel nil)
          save-fn #(do
                     (js/console.log "🔴 SAVE BUTTON CLICKED!")
                     (let [input-val (-> js/document (.getElementById "quantity-input") .-value js/parseFloat)]
                       (js/console.log "🔴 Input value:" input-val)
                       (when-not (js/isNaN input-val)
                         (js/console.log "🔴 Updating portfolio atom...")
                         (swap! state/portfolio-atom assoc crypto-id input-val)
                         (js/console.log "🔴 Portfolio atom after update:" @state/portfolio-atom)
                         (js/console.log "🔴 Calling persist-portfolio...")
                         (state/persist-portfolio)
                         (js/console.log "🔴 Closing modal...")
                         (reset! state/show-portfolio-panel nil))))
          
          header (modal-header icon symbol close-fn)
          holdings (holdings-display current-quantity holding-value crypto-id)
          form (input-form current-quantity save-fn close-fn)
          content [:div {:class "space-y-4"} holdings form]]
      
      (modal-backdrop 
        (modal-container 
          [:div header content])))))

(defn app-component []
  (let [last-update @state/last-update-atom
        loading @state/loading-atom
        error @state/error-atom
        flash @state/update-flash-atom]
    [:div
     (cond
       error [:div {:class "bg-neon-red/10 border border-neon-red/20 text-neon-red p-5 rounded-xl my-5 text-center"}
              "Failed to load market data: " error
              [:br]
              "Retrying in 30 seconds..."]
       loading [:div {:class "text-center text-gray-400 text-xl py-24"}
                [:div {:class "inline-block w-10 h-10 border-3 border-gray-700 border-t-neon-green rounded-full animate-spin mb-5"}]
                [:div "Loading market data..."]]
       :else [:div
       (portfolio-summary-header)
               [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 mb-10"}
               (let [price-keys @state/price-keys-atom
                     sorted-keys (sort-by (fn [crypto-id]
                                            (cond
                                              (= crypto-id "hash") "0-hash"  ; Put HASH first
                                              (= crypto-id "figr") "1-figr"  ; Put FIGR second
                                              :else crypto-id)) price-keys)]
                 (doall (for [crypto-id sorted-keys]
                          ^{:key crypto-id} [crypto-card crypto-id])))]
              (when last-update
                [:div {:class "text-center mt-15 pb-10"}
                 [:div {:class (str "inline-flex items-center px-6 py-3 rounded-full text-gray-400 text-sm transition-all duration-300 "
                                    (if flash
                                      "bg-neon-green/20 border border-neon-green/40 text-neon-green"
                                      "bg-white/[0.03] border border-white/10"))}
                  [:span {:class (str "w-2 h-2 rounded-full mr-2.5 "
                                      (if flash
                                        "bg-neon-green animate-ping"
                                        "bg-neon-green animate-pulse-dot"))}]
                  "Last updated: " last-update]])])
     [portfolio-panel]]))
