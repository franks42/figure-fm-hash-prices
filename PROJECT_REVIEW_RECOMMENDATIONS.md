# Project Review & Recommendations
*Generated: September 19, 2025*

## Executive Summary

The cryptocurrency price tracker delivers an impressive "static-site + live data" experience with minimal infrastructure. However, there's room to harden reliability, shrink maintenance cost, and make future feature work easier.

**Overall Grade: B+** - Solid architecture with smart no-build approach, but needs cleanup for production scaling.

---

## 🏆 Key Strengths

### Frontend Architecture
- **No-build ClojureScript**: Scittle + CDN approach eliminates build complexity
- **Fine-grained state management**: Smart use of separate atoms prevents unnecessary re-renders  
- **Visual polish**: Sophisticated animations, scan lines, and update indicators
- **Good separation of concerns**: Emerging modular structure (`state.cljs`, `effects.cljs`, `views.cljs`)

### Data Pipeline
- **Resilient fetching**: Multi-source fallback (Figure Markets → Yahoo Finance → Alpha Vantage)
- **Clean separation**: Data in `data-updates` branch, code in `main` for optimal caching
- **Comprehensive data**: Supports both crypto and stock data with proper type handling
- **Flat JSON schema**: Numeric types preserved, no string parsing needed in UI

### DevOps
- **Zero-maintenance hosting**: GitHub Pages with Actions-driven updates
- **Good documentation**: Comprehensive AGENTS.md with build commands and conventions

---

## ⚠️ Critical Issues to Address

### High Priority
1. **Duplicate workflows** - Two GitHub Actions with 80% duplicate logic
2. **Module inconsistency** - HTML loads 5 scripts but only 1 contains code
3. **Branch bloat** - Force-pushing JSON every 10min = ~200MB/month growth
4. **Fragile diff algorithm** - Only compares 5 fields, may miss API schema changes

### Medium Priority  
5. **State management scalability** - Six top-level atoms will become unwieldy
6. **Error handling** - Shows loading states unnecessarily on background fetches
7. **Manual Tailwind safelist** - Maintenance burden as classes grow
8. **Missing accessibility** - Colored arrows lack ARIA labels

---

## 🎯 Prioritized Action Plan

### Phase 1: Immediate Fixes (4-5 hours)
1. **Consolidate workflows** (1-2h)
   - Remove duplicate Actions, keep `nbb` version only
   - Reuse frontend parsing code in ClojureScript vs 170-line jq script

2. **Add CI/testing** (2-3h)  
   - Enforce `clj-kondo` linting in CI
   - Add Playwright smoke test (9 cards render, colors follow price direction)
   - Add unit tests for parsing/diffing functions

3. **Fix state management** (2h)
   - Extract pure parsing functions from side effects
   - Improve error handling (skeleton until first success, then fallback banners)
   - Use `js/requestIdleCallback` for 30s polling

4. **Accessibility improvements** (0.5h)
   - Add `role="img"` and `aria-label` to price arrows
   - Fix loading state logic

### Phase 2: Architectural Improvements (3-4 hours)
5. **Address repo bloat**
   - Implement `--depth 1` checkout or immutable paths
   - Add retention policy for old data

6. **Improve observability**
   - Add `/status.json` endpoint with last update time
   - Add status badge to README

7. **Performance evaluation**
   - Document ADR on Scittle vs shadow-cljs decision
   - Evaluate build pipeline for tree-shaking (CDN: ~250KB → Build: ~60KB)

### Phase 3: Future Features (6+ hours)  
8. **Scale state management**
   - Migrate to re-frame or equivalent event architecture
   - Implement folder-per-feature layout (`prices/`, `portfolio/`, `ui/`)

9. **Enhanced resilience**
   - Add semantic validation (price > 0 assertions)
   - Implement proper fallback strategies

---

## 📋 Detailed Technical Recommendations

### Frontend (ClojureScript)

**State Management**
```clojure
;; Current: Six separate atoms
(def prices-atom (r/atom {}))
(def loading-atom (r/atom true))
;; ... 4 more

;; Recommended: Event-driven architecture
(reg-event-db ::fetch-success [...])
(reg-sub ::prices [...])
```

**Diff Algorithm Fix**
```clojure
;; Current: Manual field comparison (fragile)
:when (or (not= old-price new-price)
          (not= old-change new-change)
          ;; ... only 5 fields

;; Recommended: Structural comparison
:when (not= (js/JSON.stringify old-data) 
            (js/JSON.stringify new-data))
```

**Accessibility**
```clojure
;; Add to price arrows
[:span {:class "mr-1.5 text-base"
        :role "img"
        :aria-label (if positive? "Price up" "Price down")} 
 arrow]
```

### Data Pipeline (GitHub Actions)

**Consolidate to Single Workflow**
```yaml
# Keep only fetch-crypto-data-nbb.yml
# Remove fetch-crypto-data.yml (170-line jq script)
# Reuse ClojureScript parsing logic
```

**Branch Management**
```bash
# Current: Force push grows repo indefinitely
git push origin data-updates-temp:data-updates --force

# Recommended: Shallow checkout
git checkout --depth 1 -b data-updates-temp
```

**Error Handling**
```yaml
# Add semantic validation
- name: Validate data quality
  run: |
    if [ "$(jq '.hash.usd > 0' data/crypto-prices.json)" != "true" ]; then
      echo "❌ Invalid HASH price"
      exit 1
    fi
```

### Project Structure

**Module Organization**
```
src/crypto_app/
├── core.cljs          # App initialization
├── state.cljs         # State management  
├── effects.cljs       # Side effects
├── views.cljs         # UI components
└── utils.cljs         # Helper functions
```

**CI Pipeline**
```yaml
name: Quality Checks
on: [push, pull_request]
jobs:
  lint:
    - run: clj-kondo --lint src/
  test:
    - run: nbb test/unit_test.cljs
  integration:
    - run: playwright test
```

---

## 🔄 Migration Strategy

1. **Week 1**: Fix critical issues (duplicate workflows, testing)
2. **Week 2**: Improve state management and error handling  
3. **Week 3**: Address scalability (re-frame migration planning)
4. **Week 4**: Performance evaluation and build pipeline decision

---

## 📊 Success Metrics

- **Reliability**: Zero failed deployments for 30 days
- **Performance**: Page load under 2s on 3G
- **Maintainability**: New features deployable in <1 hour
- **Accessibility**: WCAG 2.1 AA compliance
- **Repository health**: Repo size growth <10MB/month

---

## 📚 Additional Resources

- [Re-frame Documentation](https://day8.github.io/re-frame/)
- [ClojureScript Testing Guide](https://clojurescript.org/guides/testing)
- [Playwright Testing Patterns](https://playwright.dev/docs/best-practices)
- [GitHub Actions Best Practices](https://docs.github.com/en/actions/learn-github-actions/security-hardening-for-github-actions)
