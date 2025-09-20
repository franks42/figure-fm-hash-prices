# V3 Re-frame Migration Plan
*Build V3 with re-frame alongside working V2 - all client-side*

## V3 Strategy: Parallel Development

🎯 **Goal**: Create V3 with re-frame while keeping V2 fully functional  
📁 **Approach**: New files, new namespaces, copy/adapt from V2  
⚡ **Benefits**: No risk to V2, easy comparison, gradual migration  

```
Current V2 (Keep Working)    New V3 (Re-frame)
├── index.html               ├── index-v3.html  
├── src/crypto_app.cljs      ├── src/crypto_app_v3/
│   └── 6 atoms ✅           │   ├── core.cljs (init)
                             │   ├── events.cljs (state changes)
                             │   ├── subs.cljs (data access)
                             │   ├── views.cljs (UI components)  
                             │   └── effects.cljs (side effects)
```

---

## Phase 1: V3 Foundation (1-2 hours)

### Step 1.1: Create File Structure
```bash
mkdir -p src/crypto_app_v3
```

### Step 1.2: Copy V2 HTML → V3 HTML
**Create**: `index-v3.html`
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Crypto Price Tracker V3 (Re-frame)</title>
    
    <!-- COPY ENTIRE STYLING SECTION FROM V2 index.html -->
    <!-- Fonts, Tailwind, animations, custom CSS - everything identical -->
    
    <!-- V3 Scripts (Re-frame) -->
    <script defer src="https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.js"></script>
    <script defer crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
    <script defer crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.reagent.js"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.re-frame.js"></script>
</head>
<body class="bg-black text-white font-inter min-h-screen overflow-x-hidden bg-animated">
    <!-- COPY ENTIRE BODY STRUCTURE FROM V2 -->
    <!-- Same header, same styling, same div structure -->
    
    <div class="max-w-7xl mx-auto p-5">
        <header class="text-center mb-12 pt-8">
            <h1 class="text-5xl md:text-6xl font-black mb-3 bg-gradient-to-r from-neon-green via-neon-cyan to-neon-pink bg-clip-text text-transparent animate-glow">
                CRYPTO TRACKER V3
            </h1>
            <p class="text-gray-400 text-lg font-light">Real-time cryptocurrency prices - Re-frame Architecture</p>
        </header>
        <div id="app"></div>
    </div>

    <!-- V3 Module Loading -->
    <script type="application/x-scittle" src="src/crypto_app_v3/events.cljs"></script>
    <script type="application/x-scittle" src="src/crypto_app_v3/subs.cljs"></script>
    <script type="application/x-scittle" src="src/crypto_app_v3/views.cljs"></script>
    <script type="application/x-scittle" src="src/crypto_app_v3/effects.cljs"></script>
    <script type="application/x-scittle" src="src/crypto_app_v3/core.cljs"></script>
</body>
</html>
```

### Step 1.3: Basic Core Module
**Create**: `src/crypto_app_v3/core.cljs`
```clojure
(ns crypto-app-v3.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]))

(defn ^:export init []
  (println "🚀 V3 Re-frame Crypto Tracker Starting...")
  ;; Will add initialization here
  (rdom/render [:div "V3 Loading..."] (.getElementById js/document "app")))

;; Auto-start
(init)
```

**Test**: Visit `http://localhost:8000/index-v3.html` → Should show "V3 Loading..."

---

## Phase 2: Build Events & State (2-3 hours)

### Step 2.1: Events Module (Copy V2 Logic)
**Create**: `src/crypto_app_v3/events.cljs`
```clojure
(ns crypto-app-v3.events
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; Initial database
(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {:prices {}
     :ui {:loading? true
          :error nil
          :flash? false
          :initial-load-complete? false}
     :meta {:last-update nil
            :price-keys []}}))

;; Loading states
(rf/reg-event-db
  :set-loading
  (fn [db [_ loading?]]
    (assoc-in db [:ui :loading?] loading?)))

;; Error handling  
(rf/reg-event-db
  :set-error
  (fn [db [_ error]]
    (assoc-in db [:ui :error] error)))

(rf/reg-event-db
  :clear-error
  (fn [db _]
    (assoc-in db [:ui :error] nil)))

;; Flash animation
(rf/reg-event-db
  :trigger-flash
  (fn [db _]
    (assoc-in db [:ui :flash?] true)))

(rf/reg-event-db
  :clear-flash
  (fn [db _]
    (assoc-in db [:ui :flash?] false)))

;; Price data (copy V2 processing logic)
(rf/reg-event-db
  :update-prices
  (fn [db [_ prices-data]]
    (-> db
        (assoc :prices (:prices prices-data))
        (assoc-in [:meta :price-keys] (:price-keys prices-data))
        (assoc-in [:meta :last-update] (:last-update prices-data))
        (assoc-in [:ui :loading?] false))))
```

### Step 2.2: Subscriptions (Data Access)
**Create**: `src/crypto_app_v3/subs.cljs`
```clojure
(ns crypto-app-v3.subs
  (:require [re-frame.core :as rf]))

;; UI state subscriptions
(rf/reg-sub
  :loading?
  (fn [db]
    (get-in db [:ui :loading?])))

(rf/reg-sub
  :error-message
  (fn [db]
    (get-in db [:ui :error])))

(rf/reg-sub
  :flash-active?
  (fn [db]
    (get-in db [:ui :flash?])))

;; Data subscriptions
(rf/reg-sub
  :prices
  (fn [db]
    (:prices db)))

(rf/reg-sub
  :last-update
  (fn [db]
    (get-in db [:meta :last-update])))

(rf/reg-sub
  :price-keys
  (fn [db]
    (get-in db [:meta :price-keys])))

;; Derived data
(rf/reg-sub
  :sorted-price-keys
  :<- [:price-keys]
  (fn [price-keys]
    (sort-by (fn [crypto-id]
               (cond
                 (= crypto-id "hash") "0-hash"
                 (= crypto-id "figr") "1-figr"
                 :else crypto-id)) price-keys)))
```

---

## Phase 3: Build Views (Copy V2 Components) (2-3 hours)

### Step 3.1: Views Module (Steal V2 Components)
**Create**: `src/crypto_app_v3/views.cljs`
```clojure
(ns crypto-app-v3.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; COPY format functions from V2 exactly
(defn format-number [n decimals]
  (.toLocaleString n "en-US"
                   #js{:minimumFractionDigits decimals
                       :maximumFractionDigits decimals}))

(defn format-price [price crypto-id]
  (let [decimals (if (= crypto-id "hash") 3 2)]
    (str "$" (format-number price decimals))))

;; COPY V2 crypto icons and symbols
(def crypto-icons
  {:btc "₿" :eth "Ξ" :link "⬡" :sol "◎" :uni "🦄" 
   :xrp "💰" :hash "🔗" :figr_heloc "🏠" :figr "📈"})

(def crypto-symbols
  {:btc "BTC" :eth "ETH" :link "LINK" :sol "SOL" :uni "UNI"
   :xrp "XRP" :hash "HASH" :figr_heloc "FIGR_HELOC" :figr "FIGR"})

;; COPY V2 crypto-card component but use subscriptions
(defn crypto-card [crypto-id]
  (let [prices @(rf/subscribe [:prices])
        data (get prices crypto-id)
        name (str/upper-case crypto-id)
        symbol (get crypto-symbols (keyword crypto-id) (str/upper-case crypto-id))
        icon (get crypto-icons (keyword crypto-id) "◆")
        ;; ... COPY rest of V2 crypto-card logic exactly
        ]
    ;; COPY V2 HTML structure exactly
    [:div {:class "relative bg-white/[0.03] border border-white/10 rounded-3xl p-6..."} 
     ;; ... exact copy of V2 structure
     ]))

;; Main app component  
(defn app []
  (let [loading? @(rf/subscribe [:loading?])
        error @(rf/subscribe [:error-message])
        flash? @(rf/subscribe [:flash-active?])
        last-update @(rf/subscribe [:last-update])
        sorted-keys @(rf/subscribe [:sorted-price-keys])]
    
    [:div
     (cond
       error [:div {:class "bg-neon-red/10 border border-neon-red/20 text-neon-red p-5 rounded-xl my-5 text-center"}
              "Failed to load market data: " error]
       loading? [:div {:class "text-center text-gray-400 text-xl py-24"}
                [:div {:class "inline-block w-10 h-10 border-3 border-gray-700 border-t-neon-green rounded-full animate-spin mb-5"}]
                [:div "Loading market data..."]]
       :else [:div
              [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 mb-10"}
               (doall (for [crypto-id sorted-keys]
                        ^{:key crypto-id} [crypto-card crypto-id]))]
              ;; COPY V2 last-update footer exactly
              ])]))
```

### Step 3.2: Update Core to Use Views
**Update**: `src/crypto_app_v3/core.cljs`
```clojure
(ns crypto-app-v3.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [crypto-app-v3.events]
            [crypto-app-v3.subs]
            [crypto-app-v3.views :as views]))

(defn ^:export init []
  (println "🚀 V3 Re-frame Crypto Tracker Starting...")
  (rf/dispatch-sync [:initialize-db])
  (rdom/render [views/app] (.getElementById js/document "app")))

;; Auto-start
(init)
```

---

## Phase 4: Build Effects (Copy V2 Data Fetching) (2-3 hours)

### Step 4.1: Effects Module (Copy V2 fetch logic)
**Create**: `src/crypto_app_v3/effects.cljs`
```clojure
(ns crypto-app-v3.effects
  (:require [re-frame.core :as rf]))

;; HTTP effect (copy V2 fetch logic exactly)
(rf/reg-fx
  :http-get
  (fn [{:keys [url on-success on-failure]}]
    (-> (js/fetch (str url "?t=" (js/Date.now)))
        (.then (fn [response] (.json response)))
        (.then (fn [data] 
                 (rf/dispatch [on-success (js->clj data :keywordize-keys false)])))
        (.catch (fn [error]
                  (rf/dispatch [on-failure (.-message error)]))))))

;; Timer effects
(rf/reg-fx
  :set-timeout
  (fn [{:keys [timeout event]}]
    (js/setTimeout #(rf/dispatch event) timeout)))

;; Interval for polling
(rf/reg-fx
  :set-interval
  (fn [{:keys [interval event]}]
    (js/setInterval #(rf/dispatch event) interval)))

;; Events that trigger effects
(rf/reg-event-fx
  :fetch-crypto-data
  (fn [cofx _]
    {:http-get {:url "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json"
                :on-success :fetch-success
                :on-failure :fetch-failure}}))

(rf/reg-event-fx
  :fetch-success
  (fn [cofx [_ data]]
    (let [prices (dissoc data "timestamp" "source" "last_update")
          price-keys (keys prices)
          last-update (get data "last_update")]
      {:db (-> (:db cofx)
               (assoc :prices prices)
               (assoc-in [:meta :price-keys] price-keys)
               (assoc-in [:meta :last-update] last-update)
               (assoc-in [:ui :loading?] false)
               (assoc-in [:ui :error] nil))
       :dispatch [:trigger-flash]
       :set-timeout {:timeout 800 :event [:clear-flash]}})))

(rf/reg-event-fx
  :fetch-failure
  (fn [cofx [_ error]]
    {:db (-> (:db cofx)
             (assoc-in [:ui :loading?] false)
             (assoc-in [:ui :error] error))}))

;; Start polling
(rf/reg-event-fx
  :start-polling
  (fn [cofx _]
    {:set-interval {:interval 30000 :event [:fetch-crypto-data]}}))
```

### Step 4.2: Wire Up Data Fetching
**Update**: `src/crypto_app_v3/core.cljs`
```clojure
(defn ^:export init []
  (println "🚀 V3 Re-frame Crypto Tracker Starting...")
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch [:fetch-crypto-data])
  (rf/dispatch [:start-polling])
  (rdom/render [views/app] (.getElementById js/document "app")))
```

---

## Phase 5: Testing & Comparison (1 hour)

### Step 5.1: Side-by-Side Testing
```bash
# V2 (Original)
http://localhost:8000/index.html

# V3 (Re-frame)  
http://localhost:8000/index-v3.html
```

**Validation Checklist**:
- [ ] V3 looks identical to V2
- [ ] V3 loads data successfully
- [ ] V3 updates every 30 seconds
- [ ] V3 shows loading states
- [ ] V3 handles errors gracefully
- [ ] V3 flash animations work
- [ ] V2 still works perfectly

### Step 5.2: Performance Comparison
**Test in DevTools**:
- React DevTools: Check re-render frequency
- Network tab: Compare API calls
- Console: Check for errors/warnings

---

## Success Metrics

### Code Quality
- **Modularity**: Clean separation of concerns
- **Testability**: Pure functions, clear event flow
- **Maintainability**: Easy to add features

### Feature Parity  
- [ ] **Visual**: Identical appearance to V2
- [ ] **Functional**: All V2 features working
- [ ] **Performance**: Same or better than V2

### Future Benefits
- **Time Travel Debugging**: Built into re-frame
- **Easy Feature Addition**: Event-driven architecture
- **State Inspection**: Clear app-db structure
- **Undo/Redo**: Can be added easily

---

## Migration Timeline

| Phase | Task | Time | Risk |
|-------|------|------|------|
| 1 | Foundation & HTML | 1-2h | Low |
| 2 | Events & Subs | 2-3h | Medium |
| 3 | Views (Copy V2) | 2-3h | Low |
| 4 | Effects & Data | 2-3h | Medium |
| 5 | Testing | 1h | Low |

**Total: 8-12 hours over 2-3 days**

---

## Rollback Strategy

✅ **Zero Risk**: V2 remains completely untouched  
✅ **Easy Comparison**: Both versions available  
✅ **Gradual Adoption**: Can switch users slowly  
✅ **Quick Rollback**: Just point to index.html  

The V3 approach eliminates migration risk while providing all the benefits of re-frame architecture!
