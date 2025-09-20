# Re-frame Migration Plan
*Step-by-step migration from multiple atoms to re-frame*

## Overview

Migrate from 6 separate atoms to a single re-frame app-db while maintaining full functionality. Each step includes validation and rollback procedures.

```clojure
;; Current state (6 atoms)
(def prices-atom (r/atom {}))
(def price-keys-atom (r/atom []))
(def last-update-atom (r/atom nil))
(def loading-atom (r/atom true))
(def error-atom (r/atom nil))
(def initial-load-complete (r/atom false))
(def update-flash-atom (r/atom false))

;; Target state (1 app-db)
{:prices {:btc {...} :eth {...}}
 :ui {:loading? false :error nil :flash? false :initial-load-complete? true}
 :meta {:last-update "2025-09-19" :price-keys [:btc :eth :hash]}}
```

---

## Phase 1: Foundation Setup (1-2 hours)

### Step 1.1: Add Re-frame Dependencies
**Goal**: Set up re-frame alongside existing code without breaking anything

**Changes**:
```clojure
;; Add to crypto_app.cljs after existing requires
(:require [clojure.string :as str]
          [reagent.core :as r]
          [reagent.dom :as rdom]
          [re-frame.core :as rf])   ;; <- ADD THIS
```

**HTML Changes**:
```html
<!-- Add to index.html after reagent -->
<script defer src="https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.re-frame.js"></script>
```

**Validation**:
- [ ] App still loads normally
- [ ] Console shows no errors
- [ ] All price updates work as before

**Test Script**:
```clojure
;; Add to end of crypto_app.cljs temporarily
(js/console.log "Re-frame available:" (exists? rf/dispatch))
```

### Step 1.2: Initialize App-DB
**Goal**: Create the app-db structure without using it yet

**Add to crypto_app.cljs**:
```clojure
;; Initialize empty app-db (coexists with atoms)
(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    {:prices {}
     :ui {:loading? true
          :error nil
          :flash? false  
          :initial-load-complete? false}
     :meta {:last-update nil
            :price-keys []}}))

;; Initialize on startup
(rf/dispatch-sync [::initialize-db])
```

**Validation**:
- [ ] App-db is created: `@re-frame.db/app-db` in browser console
- [ ] Existing functionality unaffected
- [ ] No console errors

---

## Phase 2: Migrate Loading States (2 hours)

### Step 2.1: Add Loading Event Handlers
**Goal**: Create re-frame handlers for loading state

**Add to crypto_app.cljs**:
```clojure
;; Events for loading state
(rf/reg-event-db
  ::set-loading
  (fn [db [_ loading?]]
    (assoc-in db [:ui :loading?] loading?)))

;; Subscription for loading state
(rf/reg-sub
  ::loading?
  (fn [db]
    (get-in db [:ui :loading?])))

;; Bridge effect: sync re-frame → atom (temporary)
(rf/reg-fx
  ::sync-loading-atom
  (fn [loading?]
    (reset! loading-atom loading?)))

;; Enhanced event that updates both
(rf/reg-event-fx
  ::set-loading-with-sync
  (fn [cofx [_ loading?]]
    {:db (assoc-in (:db cofx) [:ui :loading?] loading?)
     ::sync-loading-atom loading?}))
```

### Step 2.2: Update Components to Use Subscription
**Goal**: Components read from re-frame instead of atom

**Replace in app-component**:
```clojure
;; OLD
(defn app-component []
  (let [last-update @last-update-atom
        loading @loading-atom          ;; <- Remove this
        error @error-atom
        flash @update-flash-atom]

;; NEW  
(defn app-component []
  (let [last-update @last-update-atom
        loading @(rf/subscribe [::loading?])  ;; <- Use subscription
        error @error-atom
        flash @update-flash-atom]
```

### Step 2.3: Update Fetch Function
**Goal**: Use re-frame events instead of direct atom updates

**Replace in fetch-crypto-data**:
```clojure
;; OLD
(when (not @initial-load-complete)
  (when (not @loading-atom)
    (reset! loading-atom true)))

;; NEW
(when (not @initial-load-complete)
  (rf/dispatch [::set-loading-with-sync true]))
```

**Validation Tests**:
- [ ] Loading spinner appears on first load
- [ ] Loading spinner disappears after data loads
- [ ] Background fetches don't show spinner
- [ ] App-db shows correct loading state in dev tools

**Test Script**:
```clojure
;; Run in browser console during loading
@re-frame.db/app-db  // Check :ui :loading? value
```

---

## Phase 3: Migrate Error States (1.5 hours)

### Step 3.1: Add Error Event Handlers

```clojure
;; Error state events
(rf/reg-event-db
  ::set-error
  (fn [db [_ error]]
    (assoc-in db [:ui :error] error)))

(rf/reg-event-db
  ::clear-error
  (fn [db _]
    (assoc-in db [:ui :error] nil)))

;; Error subscription
(rf/reg-sub
  ::error-message
  (fn [db]
    (get-in db [:ui :error])))

;; Bridge effect (temporary)
(rf/reg-fx
  ::sync-error-atom
  (fn [error]
    (reset! error-atom error)))

;; Combined event
(rf/reg-event-fx
  ::set-error-with-sync
  (fn [cofx [_ error]]
    {:db (assoc-in (:db cofx) [:ui :error] error)
     ::sync-error-atom error}))
```

### Step 3.2: Update Components and Fetch Function

**Component change**:
```clojure
;; In app-component
error @(rf/subscribe [::error-message])  ;; <- Use subscription
```

**Fetch function change**:
```clojure
;; In fetch-crypto-data catch block
(rf/dispatch [::set-error-with-sync (.-message error)])

;; In success block  
(rf/dispatch [::clear-error])
```

**Validation**:
- [ ] Error messages display correctly
- [ ] Errors clear on successful fetch
- [ ] Network errors trigger error state

---

## Phase 4: Migrate Flash States (1 hour)

### Step 4.1: Flash Event System

```clojure
;; Flash events
(rf/reg-event-db
  ::set-flash
  (fn [db [_ flash?]]
    (assoc-in db [:ui :flash?] flash?)))

(rf/reg-sub
  ::flash-active?
  (fn [db]
    (get-in db [:ui :flash?])))

;; Auto-clearing flash event
(rf/reg-event-fx
  ::trigger-flash
  (fn [cofx _]
    {:db (assoc-in (:db cofx) [:ui :flash?] true)
     ::sync-flash-atom true
     :dispatch-later [{:ms 800 :dispatch [::clear-flash]}]}))

(rf/reg-event-fx
  ::clear-flash
  (fn [cofx _]
    {:db (assoc-in (:db cofx) [:ui :flash?] false)
     ::sync-flash-atom false}))

;; Bridge effect
(rf/reg-fx
  ::sync-flash-atom
  (fn [flash?]
    (reset! update-flash-atom flash?)))
```

### Step 4.2: Update Usage

```clojure
;; Replace flash logic in fetch-crypto-data
;; OLD
(reset! update-flash-atom true)
(js/setTimeout #(reset! update-flash-atom false) 800)

;; NEW
(rf/dispatch [::trigger-flash])
```

**Validation**:
- [ ] Green flash appears on data updates
- [ ] Flash automatically clears after 800ms
- [ ] Flash timing is consistent

---

## Phase 5: Migrate Price Data (3 hours)

### Step 5.1: Price Event Handlers

```clojure
;; Price events (most complex)
(rf/reg-event-db
  ::update-prices
  (fn [db [_ {:keys [prices timestamp]}]]
    (-> db
        (assoc :prices prices)
        (assoc-in [:meta :price-keys] (keys prices))
        (assoc-in [:meta :last-update] timestamp))))

;; Price subscriptions
(rf/reg-sub
  ::prices
  (fn [db]
    (:prices db)))

(rf/reg-sub
  ::price-keys
  (fn [db]
    (get-in db [:meta :price-keys])))

(rf/reg-sub
  ::last-update
  (fn [db]
    (get-in db [:meta :last-update])))

(rf/reg-sub
  ::sorted-price-keys
  :<- [::price-keys]
  (fn [price-keys]
    (sort-by (fn [crypto-id]
               (cond
                 (= crypto-id "hash") "0-hash"
                 (= crypto-id "figr") "1-figr"  
                 :else crypto-id)) price-keys)))
```

### Step 5.2: Selective Update Logic

```clojure
;; Smart diffing event  
(rf/reg-event-fx
  ::smart-price-update
  (fn [{:keys [db]} [_ new-data]]
    (let [old-prices (:prices db)
          new-prices (dissoc new-data "timestamp" "source" "last_update")
          changes (into {}
                        (for [[coin-id new-coin-data] new-prices
                              :let [old-coin-data (get old-prices coin-id)]
                              :when (not= (js/JSON.stringify old-coin-data)
                                          (js/JSON.stringify new-coin-data))]
                          [coin-id new-coin-data]))]
      (if (seq changes)
        {:db (-> db
                 (update :prices merge changes)
                 (assoc-in [:meta :price-keys] (keys new-prices))
                 (assoc-in [:meta :last-update] (get new-data "last_update")))
         ::sync-atoms-after-price-update [changes (keys new-prices) (get new-data "last_update")]
         :dispatch [::trigger-flash]}
        {:db (assoc-in db [:meta :last-update] (get new-data "last_update"))
         ::sync-atoms-after-price-update [nil (keys new-prices) (get new-data "last_update")]}))))

;; Bridge effect for prices (complex, temporary)
(rf/reg-fx
  ::sync-atoms-after-price-update
  (fn [[changes price-keys timestamp]]
    (when changes
      (swap! prices-atom merge changes))
    (reset! price-keys-atom price-keys) 
    (reset! last-update-atom timestamp)))
```

### Step 5.3: Update Components

```clojure
;; Update crypto-card to use cursor from re-frame
(defn crypto-card [crypto-id]
  (let [prices @(rf/subscribe [::prices])
        data (get prices crypto-id)  ;; Direct access instead of cursor
        ;; ... rest unchanged
        
;; Update app-component        
(defn app-component []
  (let [last-update @(rf/subscribe [::last-update])
        loading @(rf/subscribe [::loading?])
        error @(rf/subscribe [::error-message])
        flash @(rf/subscribe [::flash-active?])]
    ;; ... 
    (let [price-keys @(rf/subscribe [::sorted-price-keys])]
      (doall (for [crypto-id price-keys] ...)))))
```

### Step 5.4: Update Fetch Function

```clojure
;; Replace complex diffing logic in fetch-crypto-data
;; OLD: ~40 lines of manual diffing and atom updates

;; NEW: Single dispatch
(rf/dispatch [::smart-price-update js-data])
```

**Validation**:
- [ ] Price updates work correctly
- [ ] Selective updates still function (no unnecessary re-renders)
- [ ] Sorting order maintained (HASH first, FIGR second)
- [ ] Timestamps update properly

---

## Phase 6: Remove Atom Bridges (1 hour)

### Step 6.1: Remove Bridge Effects

**Goal**: Remove all `::sync-*-atom` effects and their registrations

```clojure
;; DELETE these effect registrations:
;; (rf/reg-fx ::sync-loading-atom ...)
;; (rf/reg-fx ::sync-error-atom ...)  
;; (rf/reg-fx ::sync-flash-atom ...)
;; (rf/reg-fx ::sync-atoms-after-price-update ...)

;; SIMPLIFY events to not use bridge effects:
(rf/reg-event-db ::set-loading [...])  ;; Remove -with-sync versions
(rf/reg-event-db ::set-error [...])
```

### Step 6.2: Remove Atom Declarations  

```clojure
;; DELETE these lines:
;; (def prices-atom (r/atom {}))
;; (def price-keys-atom (r/atom []))
;; (def last-update-atom (r/atom nil))  
;; (def loading-atom (r/atom true))
;; (def error-atom (r/atom nil))
;; (def initial-load-complete (r/atom false))
;; (def update-flash-atom (r/atom false))
```

### Step 6.3: Replace Remaining Atom References

```clojure
;; Find and replace any remaining atom references:
;; @initial-load-complete → @(rf/subscribe [::initial-load-complete?])

;; Add missing subscription:
(rf/reg-sub
  ::initial-load-complete?
  (fn [db]
    (get-in db [:ui :initial-load-complete?])))
```

**Validation**:
- [ ] All functionality identical to Phase 5
- [ ] No console errors
- [ ] App-db is single source of truth
- [ ] No atom references remain

---

## Phase 7: Cleanup & Optimization (1 hour)

### Step 7.1: Organize Code Structure

**Create new namespaces**:
```clojure
;; crypto_app/events.cljs
(ns crypto-app.events
  (:require [re-frame.core :as rf]))

;; Move all reg-event-db, reg-event-fx here

;; crypto_app/subs.cljs  
(ns crypto-app.subs
  (:require [re-frame.core :as rf]))

;; Move all reg-sub here

;; crypto_app/effects.cljs
(ns crypto-app.effects  
  (:require [re-frame.core :as rf]))

;; Move HTTP, DOM effects here
```

### Step 7.2: Add Dev Tools

```clojure
;; Add to main namespace
(when ^boolean goog.DEBUG
  (enable-console-print!)
  (println "Re-frame dev mode"))
```

**Validation**:
- [ ] Code is well-organized
- [ ] All tests pass
- [ ] Performance is same or better
- [ ] Ready for V2 features

---

## Testing Strategy

### Automated Tests (Add throughout migration)

```clojure
;; test/crypto_app/events_test.cljs
(ns crypto-app.events-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [crypto-app.events :as events]))

(deftest set-loading-test
  (let [initial-db {:ui {:loading? false}}
        result (events/set-loading initial-db [::events/set-loading true])]
    (is (= true (get-in result [:ui :loading?])))))

(deftest price-update-test
  (let [initial-db {:prices {:btc {:usd 100}}}
        new-data {:btc {:usd 110} :eth {:usd 200}}
        result (events/update-prices initial-db [::events/update-prices {:prices new-data}])]
    (is (= 110 (get-in result [:prices :btc :usd])))
    (is (= 200 (get-in result [:prices :eth :usd])))))
```

### Manual Testing Checklist (Run after each phase)

- [ ] **Loading States**: Spinner shows/hides correctly
- [ ] **Error Handling**: Network errors display red banner
- [ ] **Price Updates**: All crypto cards update with new data
- [ ] **Flash Animation**: Green flash on updates, auto-clears
- [ ] **Sorting**: HASH first, FIGR second, others alphabetical
- [ ] **Performance**: No unnecessary re-renders (check React DevTools)
- [ ] **Responsiveness**: Mobile layout works
- [ ] **Accessibility**: Screen reader compatibility

### Rollback Plan

**If issues arise during any phase:**

1. **Git rollback**: `git checkout HEAD~1`
2. **Disable re-frame**: Comment out re-frame script in HTML
3. **Restore atoms**: Uncomment atom declarations
4. **Revert subscriptions**: Change back to `@atom-name`

---

## Timeline & Effort

| Phase | Time Estimate | Risk Level | Dependencies |
|-------|---------------|------------|--------------|
| 1. Foundation | 1-2 hours | Low | None |
| 2. Loading States | 2 hours | Low | Phase 1 |
| 3. Error States | 1.5 hours | Low | Phase 2 |
| 4. Flash States | 1 hour | Medium | Phase 3 |
| 5. Price Data | 3 hours | High | Phase 4 |
| 6. Remove Bridges | 1 hour | Medium | Phase 5 |
| 7. Cleanup | 1 hour | Low | Phase 6 |

**Total: 9.5-11.5 hours over 2-3 days**

---

## Success Metrics

### After Migration:
- [ ] **Code Reduction**: ~50 lines fewer (less coordination logic)
- [ ] **Testability**: Pure functions, easy unit tests  
- [ ] **Debugging**: Time-travel debugging available
- [ ] **Maintainability**: Clear event flow, single state source
- [ ] **Performance**: Same or better (fewer subscriptions than atoms)
- [ ] **Ready for V2**: Multi-currency, portfolio features easy to add

### V2 Features Enabled:
- [ ] **Currency Selection**: Single event changes entire app currency
- [ ] **Portfolio Management**: Derived state for total values
- [ ] **Settings Persistence**: Unified app state → localStorage
- [ ] **Undo/Redo**: Built-in with re-frame interceptors

This migration sets the foundation for your V2 multi-currency interface while maintaining all current functionality.
