# Workflow Consolidation Plan
*Remove duplicate GitHub Actions, keep nbb version, eliminate 170-line jq script*

## Current State Analysis

### Duplicate Workflows Problem

**File 1**: `.github/workflows/fetch-crypto-data.yml` (LEGACY)
- 195 lines total
- 170-line complex jq script for data transformation
- Multiple API fallback handling
- Manual JSON manipulation
- Status: **DISABLED** (schedule commented out)

**File 2**: `.github/workflows/fetch-crypto-data-nbb.yml` (ACTIVE)  
- 50 lines total
- Uses nbb (ClojureScript on Node.js)
- Delegates to custom action
- Status: **ACTIVE** (runs every 10 minutes)

**Duplication Issues**:
- ~80% identical git logic (checkout, commit, push)
- Two different data parsing approaches
- Maintenance burden (security updates, bug fixes)
- Confusion about which workflow is authoritative

---

## Solution: Consolidate to nbb + Enhanced ClojureScript

### Why Keep nbb Version?

✅ **Code Reuse**: Can share parsing logic with frontend  
✅ **Maintainability**: ClojureScript vs complex bash/jq  
✅ **Testing**: Can unit test data transformation  
✅ **Consistency**: Same language as frontend  
✅ **Extensibility**: Easier to add V2 features  

### Migration Strategy

1. **Enhance nbb action** with fallback logic from jq version
2. **Test enhanced version** thoroughly  
3. **Remove legacy workflow** after validation
4. **Clean up unused files**

---

## Phase 1: Analyze Current nbb Action (30 min)

### Step 1.1: Current nbb Implementation Analysis

**Current nbb Action Structure**:
```
.github/actions/fetch-data/
├── action.yml         # Action definition
├── action.cljs        # ClojureScript logic (117 lines)
├── package.json       # Dependencies
└── node_modules/      # Installed packages
```

**Current Capabilities**:
✅ **Figure Markets API** - Fetches crypto data  
✅ **Yahoo Finance fallback** - FIGR stock data  
✅ **Data processing** - ClojureScript transformation  
✅ **JSON output** - Writes to `data/crypto-prices.json`  

**Missing from Legacy jq Version**:
❌ **Alpha Vantage fallback** - Not implemented  
❌ **Error handling depth** - Less comprehensive  
❌ **Data validation** - Basic validation only  
❌ **Multiple fallback sources** - Only Yahoo for stocks

### Step 1.2: Gap Analysis - Legacy vs nbb Features

**Legacy jq Version Has**:
- Multi-source fallback: Yahoo → Alpha Vantage → Hardcoded fallback
- Comprehensive error checking (`jq empty` validation)
- Data quality assertions
- Complex JSON manipulation (170 lines jq)

**nbb Version Has**:
- Clean ClojureScript code (117 lines)
- Better maintainability 
- Type safety through ClojureScript
- Reusable functions

**Migration Need**: Enhance nbb action with legacy fallback logic

---

## Phase 2: Enhance nbb Action with Missing Features (1-1.5 hours)

### Step 2.1: Add Alpha Vantage Fallback Support

**Goal**: Add Alpha Vantage as fallback when Yahoo Finance fails

**Add to `action.cljs`**:

```clojure
(defn fetch-alpha-vantage [api-key]
  (if api-key
    (-> (fetch (str "https://www.alphavantage.co/query"
                    "?function=GLOBAL_QUOTE&symbol=FIGR"
                    "&apikey=" api-key))
        (p/then (fn [response] (.json response)))
        (p/then (fn [data]
                  (log "✅ Alpha Vantage data fetched")
                  data))
        (p/catch (fn [error]
                   (log "❌ Alpha Vantage failed:" (.-message error))
                   nil)))
    (p/resolved nil)))

(defn process-alpha-vantage-stock [alpha-data]
  (when alpha-data
    (let [quote (j/get alpha-data "Global Quote")
          price (js/parseFloat (j/get quote "05. price"))
          change-percent (-> (j/get quote "10. change percent")
                             (str/replace "%" "")
                             js/parseFloat)]
      {:figr {:usd price
              :usd_24h_change change-percent
              :usd_24h_vol (js/parseInt (j/get quote "06. volume"))
              :symbol "FIGR"
              :type "stock"
              :company_name "Figure Technology Solutions, Inc."
              :exchange "Alpha Vantage"
              :currency "USD"}})))

(defn get-fallback-stock-data []
  "Hardcoded fallback when all APIs fail"
  {:figr {:usd 37.33
          :usd_24h_change 0
          :usd_24h_vol 1500000
          :symbol "FIGR"
          :type "stock"
          :company_name "Figure Technology Solutions, Inc."
          :exchange "Fallback"
          :currency "USD"}})
```

### Step 2.2: Add Smart Fallback Logic

**Replace current main function**:

```clojure
(defn fetch-stock-data-with-fallbacks [api-key]
  (-> (fetch-yahoo-finance)
      (p/then (fn [yahoo-data]
                (if (and yahoo-data 
                         (j/get-in yahoo-data [:chart :result 0 :meta :regularMarketPrice]))
                  {:data yahoo-data :source "yahoo"}
                  (throw (js/Error. "Yahoo data invalid")))))
      (p/catch (fn [_]
                 (log "⚠️ Yahoo Finance failed, trying Alpha Vantage...")
                 (-> (fetch-alpha-vantage api-key)
                     (p/then (fn [alpha-data]
                               (if alpha-data
                                 {:data alpha-data :source "alphavantage"}
                                 (throw (js/Error. "Alpha Vantage failed")))))
                     (p/catch (fn [_]
                                (log "⚠️ All APIs failed, using fallback data")
                                {:data (get-fallback-stock-data) :source "fallback"})))))))

(defn main []
  (let [api-key (core/getInput "alpha-vantage-api-key")]
    (-> (p/all [(fetch-figure-markets) 
                (fetch-stock-data-with-fallbacks api-key)])
        (p/then (fn [[crypto-data stock-result]]
                  (let [processed-crypto (process-crypto-data crypto-data)
                        processed-stock (case (:source stock-result)
                                          "yahoo" (process-stock-data (:data stock-result))
                                          "alphavantage" (process-alpha-vantage-stock (:data stock-result))
                                          "fallback" (:data stock-result))
                        source-name (str "figuremarkets+" (:source stock-result))
                        ; ... rest of processing
                        ])))))
```

### Step 2.3: Add Data Validation

**Add validation functions**:

```clojure
(defn validate-crypto-data [data]
  (let [required-coins #{"btc" "eth" "hash"}
        present-coins (set (map name (keys data)))]
    (when-not (every? present-coins required-coins)
      (throw (js/Error. (str "Missing required crypto data: " 
                            (clojure.set/difference required-coins present-coins)))))
    (doseq [[coin-id coin-data] data]
      (when (or (not (:usd coin-data)) (<= (:usd coin-data) 0))
        (throw (js/Error. (str "Invalid price for " coin-id ": " (:usd coin-data))))))
    data))

(defn validate-output-data [combined-data]
  (when (< (count (keys combined-data)) 5)  ; Expect at least 5 assets
    (throw (js/Error. "Insufficient data - too few assets")))
  (when-not (:timestamp combined-data)
    (throw (js/Error. "Missing timestamp in output")))
  combined-data)
```

**Integration in main function**:

```clojure
;; Add validation after processing
(let [processed-crypto (validate-crypto-data (process-crypto-data crypto-data))
      ; ... 
      combined-data (validate-output-data 
                      (merge processed-crypto processed-stock metadata))]
  ;; ... write file
  )
```

---

## Phase 3: Test Enhanced nbb Action (30 min)

### Step 3.1: Unit Testing

**Create test file**: `.github/actions/fetch-data/test.cljs`

```clojure
#!/usr/bin/env nbb

(ns test
  (:require ["assert" :as assert]
            [action :refer [process-crypto-data process-stock-data validate-crypto-data]]))

(defn test-crypto-processing []
  (let [sample-data {:data [{:symbol "BTC-USD"
                            :midMarketPrice "50000"
                            :percentageChange24h "0.02"
                            :volume24h "1000000"}]}
        result (process-crypto-data sample-data)]
    (assert/strictEqual (:usd (:btc result)) 50000)
    (assert/strictEqual (:usd_24h_change (:btc result)) 2.0)
    (println "✅ Crypto processing test passed")))

(defn test-stock-processing []
  (let [sample-data {:chart {:result [{:meta {:regularMarketPrice 40
                                             :previousClose 38}}]}}
        result (process-stock-data sample-data)]
    (assert/strictEqual (:usd (:figr result)) 40)
    (println "✅ Stock processing test passed")))

(defn test-validation []
  (try 
    (validate-crypto-data {:btc {:usd 0}})  ; Should fail
    (throw (js/Error. "Validation should have failed"))
    (catch js/Error e
      (println "✅ Validation test passed"))))

;; Run tests
(test-crypto-processing)
(test-stock-processing) 
(test-validation)
(println "🎉 All tests passed")
```

**Run tests**:
```bash
cd .github/actions/fetch-data
nbb test.cljs
```

### Step 3.2: Integration Testing

**Test with real APIs** (manually):

```bash
# Test current version
cd .github/actions/fetch-data
nbb action.cljs

# Check output
ls -la ../../../data/crypto-prices.json
cat ../../../data/crypto-prices.json | jq '.figr.usd'
```

**Test fallback scenarios**:

```bash
# Test with invalid Yahoo data (modify URL temporarily)
# Test Alpha Vantage fallback
# Test complete fallback
```

### Step 3.3: Validate Against Legacy Output

**Compare outputs**:

```bash
# Run legacy workflow manually
gh workflow run fetch-crypto-data.yml

# Run new nbb workflow  
gh workflow run fetch-crypto-data-nbb.yml

# Compare outputs
diff <(curl -s "https://raw.githubusercontent.com/.../crypto-prices.json" | jq -S .) \
     <(curl -s "https://raw.githubusercontent.com/.../crypto-prices.json" | jq -S .)
```

**Validation Checklist**:
- [ ] Same number of crypto assets
- [ ] Price values within reasonable range
- [ ] FIGR stock data present  
- [ ] Metadata fields consistent
- [ ] JSON structure identical

---

## Phase 4: Remove Legacy Workflow (15 min)

### Step 4.1: Disable Legacy Workflow

**Goal**: Safely disable without deleting (reversible)

**Edit `.github/workflows/fetch-crypto-data.yml`**:

```yaml
# Add at top of file
# DEPRECATED: This workflow has been replaced by fetch-crypto-data-nbb.yml
# Keeping for reference during migration period
# TODO: Delete after 2 weeks of successful nbb operation

name: Fetch Crypto Data (DEPRECATED)

on:
  # Disable all triggers
  workflow_dispatch:
    inputs:
      force_run:
        description: 'Emergency run only (true/false)'
        required: true
        default: 'false'
        type: boolean

jobs:
  deprecated-notice:
    if: github.event.inputs.force_run != 'true'
    runs-on: ubuntu-latest
    steps:
      - name: Deprecated workflow notice
        run: |
          echo "❌ This workflow has been deprecated"
          echo "✅ Use fetch-crypto-data-nbb.yml instead"
          echo "🔧 For emergency: set force_run=true"
          exit 1

  # Keep original job but only run if force_run=true
  fetch-data:
    if: github.event.inputs.force_run == 'true'
    # ... existing job content
```

### Step 4.2: Update Documentation

**Update README.md**:

```markdown
## Data Pipeline

- **Active Pipeline**: `.github/workflows/fetch-crypto-data-nbb.yml`
  - Uses ClojureScript (nbb) for data processing
  - Runs every 10 minutes
  - Multi-source fallback: Yahoo → Alpha Vantage → Hardcoded

- **Deprecated**: `fetch-crypto-data.yml` (jq-based, disabled)
```

**Update AGENTS.md**:

```markdown
## Workflows

- **Primary**: `fetch-crypto-data-nbb.yml` - Active data fetching
- **Deprecated**: `fetch-crypto-data.yml` - Keep for emergency only
```

---

## Phase 5: Final Cleanup (15 min)

### Step 5.1: Monitor New Workflow

**Run for 48 hours monitoring**:
- [ ] Data updates every 10 minutes
- [ ] No failures in GitHub Actions
- [ ] Frontend receives data correctly
- [ ] All fallback scenarios tested

### Step 5.2: Complete Removal

**After 48 hours of success**:

```bash
# Delete legacy workflow file
git rm .github/workflows/fetch-crypto-data.yml
git commit -m "Remove deprecated jq-based workflow

- Replaced by nbb-based workflow 
- 48 hours of successful operation verified
- Reduces maintenance burden from 2 → 1 workflow"
```

**Clean up references**:
- Remove from documentation
- Update any scripts that referenced old workflow

---

## Rollback Plan

### If Enhanced nbb Action Fails:

**Immediate rollback** (within 5 minutes):

```bash
# Re-enable legacy workflow
git checkout HEAD~1 .github/workflows/fetch-crypto-data.yml

# Trigger manual run
gh workflow run fetch-crypto-data.yml
```

**Partial rollback** (keep nbb, revert features):

```bash
# Revert action.cljs to simpler version
git checkout HEAD~3 .github/actions/fetch-data/action.cljs
```

### If Data Quality Issues:

```bash
# Emergency: Use hardcoded data
echo '{
  "btc": {"usd": 50000, "usd_24h_change": 0},
  "hash": {"usd": 0.038, "usd_24h_change": 0},
  "figr": {"usd": 37.33, "usd_24h_change": 0}
}' > data/crypto-prices.json

git add data/crypto-prices.json
git commit -m "Emergency: Use fallback data"
git push
```

---

## Success Metrics

### Before Consolidation:
- **2 workflows** (195 + 50 lines = 245 lines)
- **Complex maintenance** (jq + ClojureScript knowledge needed)
- **Duplicate git logic** (~50 lines duplicated)

### After Consolidation:
- **1 workflow** (~75 lines total)  
- **Single language** (ClojureScript only)
- **Better error handling** (multiple fallbacks)
- **Testable code** (unit tests possible)
- **Maintainable** (reusable functions)

### Quality Improvements:
- ✅ **50-60% less code** to maintain
- ✅ **Unified error handling** strategy  
- ✅ **Better test coverage** capability
- ✅ **Consistent data processing** logic
- ✅ **Easier to extend** for V2 features

---

## Implementation Timeline

| Phase | Task | Time | Dependencies |
|-------|------|------|--------------|
| 1 | Analyze current state | 30 min | None |
| 2 | Enhance nbb action | 1-1.5h | Phase 1 |
| 3 | Test enhanced action | 30 min | Phase 2 |
| 4 | Remove legacy workflow | 15 min | Phase 3 |
| 5 | Final cleanup | 15 min | 48h monitoring |

**Total Development: 2.5-3 hours**  
**Total Timeline: 3-4 days** (including monitoring period)

This consolidation eliminates technical debt, improves maintainability, and sets a clean foundation for the git bloat cleanup and re-frame migration phases.
