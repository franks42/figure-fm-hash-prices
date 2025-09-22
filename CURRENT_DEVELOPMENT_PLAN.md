# Current Development Plan - V4.0.0+

**Project**: Figure Markets Hash Prices Tracker  
**Current Version**: V4.0.0-production  
**Status**: Background charts implemented, planning next enhancements  
**Goal**: Subliminal market awareness tool (not professional trading platform)

## 🎯 **Project Vision**

Create a **quick, subliminal view** of market and portfolio performance. Users should understand their financial position and market trends in **<3 seconds** of viewing.

**Core Principle**: Subtle, at-a-glance information - not complex trading tools.

---

## 📊 **Current State (V4.1+)**

### ✅ **Completed Features**
- **V1/V2 Legacy Cleanup**: Removed 5,000+ lines of legacy code, unified to single V4 architecture
- **Chart Color Sentiment**: Green/red chart backgrounds based on price trend direction for instant visual feedback
- **Trend Line Visualization**: Dotted grey trend lines from start to end price showing overall direction
- **Background Price Charts**: 24h historical charts behind crypto card prices (uPlot library)
- **Robust Data Pipeline**: Fault-tolerant GitHub Actions, never fails on bad API data
- **Clear Stale Data Warnings**: Red borders/banners when API data is invalid
- **Multi-Currency System**: 10 global currencies with live conversion
- **Portfolio Management**: Persistent holdings with real-time valuations
- **iOS Widget Support**: Widget-optimized layouts ready for Playwright screenshots
- **Clean Codebase**: Zero clj-kondo warnings, consistent formatting, enhanced coding practices

### 🏗️ **Technical Architecture**
- **Frontend**: ClojureScript + Scittle (no-build) + Re-frame + uPlot  
- **Data Sources**: Figure Markets API (cryptos) + Yahoo Finance (FIGR stock)
- **Updates**: GitHub Actions every 10 minutes with multi-source fallback
- **Hosting**: GitHub Pages (serverless, zero-cost)
- **Charts**: Browser-direct Figure Markets historical API (CORS enabled)

### 📈 **Chart Implementation Status**
- ✅ **Working**: All crypto assets get background charts from Figure Markets API
- ✅ **Data Format**: OHLCV candles with volume information available
- ✅ **Coverage**: BTC, ETH (25+ points), HASH (12+ points), SOL/LINK/UNI/XRP (variable)
- ❌ **Missing**: FIGR stock (Yahoo Finance, CORS blocked)

---

## 📋 **Priority Roadmap**

### 🔥 **HIGH PRIORITY** (Core UX Issues)

#### 1. Chart & Card Layout Redesign  
**Goal**: Comprehensive chart redesign for better information density and professional appearance  
**Why**: Current layout is inefficient, missing key information positioning, lacks period selection  
**Design Specification**:
```
┌─────────────────────────────────────────┐
│ BTC                      [⚙️]     €1,250 │ ← Symbol (upper-left) + Settings (upper-right) + High (far-right)
│                                         │
│ $1,234.56 [USD]     📈📉 CHART    ▲2.34% │ ← Current price+currency (left-middle) + Change% (right-middle)
│                                         │
│[24H]                               €1,200 │ ← Period button (bottom-left) + Low (right-lower)  
└─────────────────────────────────────────┘
Bitcoin                                    ← Asset description (left-aligned)

Volume: $1.2M    Trades: 45    [FM]        ← Volume, Trades, Feed indicator (same row)
```
**Key Changes**:
- Square graph (width = height) for better visual balance
- Asset symbol (BTC/ETH/HASH) in upper-left corner for immediate identification  
- Settings button (⚙️) in upper-right corner for accessibility options
- High price in far-right corner with currency symbols (no "USD" suffix)
- Current price + currency button in left-middle for primary focus
- Low price in bottom-right corner with currency symbols
- Change percentage in right-middle for visual balance  
- Period selector [24H]/[1W]/[1M] button in bottom-left corner
- Asset description (Bitcoin, Ethereum, etc.) under chart, left-aligned
- Feed indicator moved to volume/trades row for consolidation
- All price overlays update with currency conversion

**Oracle Review - Implementation Guidance**:
- **Visual Hierarchy Critical**: Apply 3-tier system for <3s comprehension
  - Tier-1 (current price, ±%): Largest font, bold weight
  - Tier-2 (high/low): Small-caps, light grey
  - Tier-3 (volume/trades/feed): Micro-copy 11px
- **Technical Approach**: HTML overlay divs (not canvas) for responsive fonts
- **Square Enforcement**: Use ResizeObserver for min(width,height) since no CSS container queries in Scittle
- **Performance**: Avoid CSS transitions on numeric updates for mobile Safari
- **Typography**: Mono-font (JetBrains Mono) for financial terminal aesthetic
- **Accessibility**: Add luminosity shift for color-blind users (lighter=up, darker=down)
- **Data Pipeline**: GitHub Actions pre-aggregation needed for multi-period support

**Implementation Challenges**:
- Multi-period data requires extending GitHub Actions to generate 1W/1M/3M aggregations
- Currency conversion must apply to historical high/low values, not just current price
- State management: Memoize derived series by [asset-id, period, fx-rates-hash]
- Mobile: Ensure 44×44px tap targets for period/currency selectors

**Oracle Implementation Strategy - Safe Step-by-Step Approach**:

**Phase 1: Foundation & Feature Flag**
- [ ] Add feature flag subscription `[:ui/new-layout?]` (URL param `?ui=v5` or localStorage)
- [ ] Create new namespaces (`crypto_app_v3.chart_v5.cljs`, `card_v5.cljs`, `layout_v5.cljs`) 
- [ ] No mutations to existing V4 files - parallel development only

**Phase 2: HASH Prototype (Single Asset)**
- [ ] Static HTML overlay layout with CSS tier classes (.overlay-tier1/2/3)
- [ ] ResizeObserver hook for square enforcement `min(width, height)`
- [ ] Port existing uPlot integration (24h data only initially)
- [ ] Connect to current re-frame subscriptions (`:prices`, `:historical-data`)
- [ ] Overlay tiers with live data binding (symbol, price, high/low, change%)

**Phase 3: Data Pipeline & Period Selection**
- [ ] Extend GitHub Actions for multi-period JSON generation (`asset-id_period.json`)
- [ ] Memoized re-frame subscription `:series-by-asset-period` by `[asset-id, period, fx-rates-hash]`
- [ ] Period selector button functionality (24H → 1W → 1M → 3M → 6M → 1Y)
- [ ] Currency conversion for historical high/low values

**Phase 4: Quality Assurance**
- [ ] Playwright visual regression testing (`ui_v5.spec.ts`)  
- [ ] Screenshot baseline for HASH card comparisons
- [ ] Performance tuning with larger datasets (1M period, 100+ points)
- [ ] Mobile Safari testing (tap targets, ResizeObserver performance)

**Phase 5: Mass Rollout**
- [ ] Extend to BTC & ETH assets with proven HASH template
- [ ] Professional terminal grid layout (`layout_v5.cljs`)
- [ ] Staging deployment with feature flag default flip
- [ ] User feedback collection and refinement

**Phase 6: Legacy Cleanup**
- [ ] Remove V4 components after two stable deployments
- [ ] Delete feature flag once fully migrated
- [ ] Code cleanup and optimization

**Risk Mitigation**:
- Zero edits to current working files until replacement is proven
- Feature flag enables instant rollback capability  
- Visual regression tests prevent silent UI drift
- Each phase has clear success criteria and safe commit boundaries

#### 2. Portfolio Performance Card  
**Goal**: Dedicated portfolio card showing aggregated performance like individual assets  
**Why**: Users need subliminal awareness of overall portfolio health and trends  
**Features**: 
- 24h high/low/change% for entire portfolio
- Background chart with green/red sentiment and trend line
- Same visual treatment as asset cards for consistency
- Real-time portfolio value tracking over time
**Dependencies**: Requires historical data subscriptions and metrics calculations
**Implementation**: 
- Create `:portfolio-historical` subscription aggregating asset histories
- Calculate portfolio metrics (sum quantity × price at each time point)
- Reuse existing background-chart component with `:portfolio` pseudo-ID
- Portfolio card component with familiar asset card UX

#### 2. Auto-feed Selection (was #1)
**Goal**: Automatically choose best data source per asset based on liquidity  
**Why**: Figure Markets has low liquidity for major cryptos vs established exchanges  
**Implementation**: 
- Auto-rank feeds by liquidity thresholds and freshness
- Fallback logic when Figure Markets volume < threshold → CoinGecko
- Show feed source as small indicator pill ("FM" / "CG")
- No manual selection required - smart defaults

#### 3. Portfolio Value Consolidation (was #2) 
**Goal**: Single clean portfolio component (value + button combined)  
**Why**: Currently scattered display, hard to quickly assess portfolio status  
**Implementation**: Merge portfolio-value-display + portfolio-button into one component

#### 4. Fix FIGR Feed Indicator (was #3)
**Goal**: Show correct data source for FIGR (Yahoo Finance, not Figure Markets)  
**Why**: Currently shows wrong/confusing feed indicator  
**Implementation**: Conditional data source display based on asset type

### 📊 **MEDIUM PRIORITY** (Enhanced Subliminal Features)

#### 4. Chart Color Sentiment  
**Goal**: Green charts for positive trends, red for negative  
**Why**: Instant visual mood assessment across all assets  
**Implementation**: 
- Calculate price direction (current vs 24h ago)
- Dynamic chart colors based on positive/negative change
- Subtle but clear visual indicators

#### 5. FIGR Historical Charts
**Goal**: Add FIGR stock historical data via GitHub Actions  
**Why**: Complete chart coverage for all tracked assets  
**Implementation**: 
- Extend GitHub Actions to fetch Yahoo Finance historical
- Store FIGR candle data in repo
- Apply same chart rendering to FIGR cards

#### 6. Time Period Selector
**Goal**: Switch between 24h/1w/1m/3m/6m/1y chart periods  
**Why**: Different time horizons for market assessment  
**Implementation**: 
- Button UI like currency selector
- Update all charts simultaneously  
- Maintain same data sources per period

### 🎨 **LOW PRIORITY** (Advanced Features)

#### 7. Settings Modal & Accessibility Options
**Goal**: User-configurable accessibility settings for color-blind friendly alternatives  
**Why**: Support 8% of users with color vision deficiency, improve overall accessibility  
**Implementation**:
- Settings button (⚙️) in upper-right corner of each card
- Modal overlay with accessibility options
- Color-blind friendly palette toggle (red/green → blue/orange or other alternatives)
- Settings persistence via localStorage  
- Applies to chart backgrounds, change indicators, and all color-coded elements
- Optional: High contrast mode, reduced motion preferences
**Technical**:
- CSS custom properties for color theme switching
- React portal or similar for modal overlay
- localStorage key: `crypto-tracker-accessibility-settings`

#### 8. USD Volume Estimation
**Goal**: Show meaningful volume comparisons across assets  
**Formula**: `Token Volume × (High + Low) / 2`  
**Why**: USD amounts more intuitive than token counts

#### 9. Volume Trend Charts  
**Goal**: Subliminal trading activity assessment over 1w/1m/3-6m  
**Why**: Gauge Figure Markets adoption vs established exchanges

#### 10. Osmosis DEX Integration
**Goal**: Alternative HASH price feed from Cosmos DEX  
**Why**: Cross-exchange price validation, DeFi market comparison

---

## 🏗️ **Implementation Guidelines**

### **Development Workflow**
1. **ALWAYS run clj-kondo** before any commit
2. **ALWAYS run cljfmt** for consistent formatting  
3. **Use Oracle** for complex design decisions and debugging
4. **Test locally** before pushing to production
5. **Maintain subliminal UX focus** - avoid over-engineering

### **Technical Constraints**
- **Browser-only environment** (Scittle limitations)
- **CORS dependencies** (some APIs blocked)  
- **No build step** (must work with CDN libraries)
- **GitHub Pages hosting** (static files only)

### **Code Quality Standards**  
- Zero clj-kondo errors required for all commits
- Consistent formatting with cljfmt
- Clear logging for debugging complex features
- Fallback handling for all external API calls

---

## 🎯 **Next AI Agent Onboarding**

### **Quick Start**
1. **Read AGENTS.md** - Current commands and architecture
2. **Check this document** - Current priorities and context
3. **Run `python3 -m http.server 8000`** - Test local setup
4. **Review todo list** with `todo_read` tool

### **Key Context**
- **V4 is production** - no more migration, focus on enhancements
- **Charts work** - build on existing uPlot implementation  
- **Oracle available** - use for complex decisions and debugging
- **User wants subliminal UX** - quick assessment, not trading platform

### **Current Focus**
Start with **High Priority items** - market feed selector enables core goal of comparing Figure Markets vs established exchanges for liquidity assessment.

---

*Last Updated: 2025-09-21*  
*Next Review: After completing High Priority items*
