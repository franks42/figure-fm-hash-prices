# Current Development Plan - V4.0.0+

**Project**: Figure Markets Hash Prices Tracker  
**Current Version**: V5.0.0-prototype  
**Status**: V5 with FIGR charts + persistence complete, CRITICAL scalability issue identified  
**Goal**: Subliminal market awareness tool (not professional trading platform)

## 🚨 **CRITICAL SCALABILITY ISSUE: API Key Limits**

**Status**: ✅ FIGR charts working beautifully, ❌ **Won't scale beyond ~10 users**

**The Problem:**
- Twelve Data free tier: **800 calls/day per API key**
- Current implementation: **Hardcoded shared key**  
- Each V5 user: ~3 API calls (24H/1W/1M periods) + refresh cycles
- With 1-hour caching: ~72 calls/user/day worst case
- **Current capacity: Only 10-12 concurrent users maximum**

**The Solution (High Priority):**
```
🎯 User API Key Configuration System
├── Settings panel for personal Twelve Data API key input
├── localStorage persistence of user's personal key  
├── Graceful fallback to demo/shared key for new users
├── Clear rate limit messaging and free API key instructions
└── Result: Each user gets 800 calls/day = scales to hundreds of users
```

**Implementation Needed:**
1. Settings UI component for API key management
2. Twelve Data API key validation endpoint testing
3. Dynamic key injection in `TWELVE_DATA_API_KEY` constant
4. Error handling for invalid/expired user keys
5. Documentation for obtaining free Twelve Data keys

**Impact**: Without this, FIGR charts will fail once usage grows beyond a few users.

## 🚀 **BREAKTHROUGH: Direct API Strategy Viable**

**New Discovery (2025-09-23):** Real-time data can be fetched directly from APIs, potentially eliminating GitHub Actions complexity.

### **Direct API Capabilities CONFIRMED:**

**Figure Markets API - PRODUCTION READY:**
- ✅ **CORS enabled**: `Access-Control-Allow-Origin: *`
- ✅ **Complete data**: All V5 fields (price, 24h change %, volume, high/low, trades)  
- ✅ **Rate limits**: ~60 req/min (sufficient for 30-sec polling)
- ✅ **Coverage**: HASH, BTC, ETH fully supported
- ✅ **No authentication**: Public API, no keys required

**Twelve Data API - PRODUCTION READY:**
- ✅ **CORS enabled**: Works directly from browser
- ✅ **FIGR real-time**: `/price` and `/quote` endpoints working
  - Current price: $43.09 ✅ 
  - Full market data: volume, high/low, change%, 52-week ✅
  - Real-time updates available ✅
- ✅ **Historical charts**: Already working in V5
- ✅ **Rate limits**: 800 req/day free tier (sufficient for real-time + charts)

### **Strategic Architecture Pivot:**

**CURRENT (Complex):**
```
GitHub Actions (10min) → JSON file → GitHub Pages → V5 App
                     ↘ Git commits, branch management, deploy delays
```

**PROPOSED (Simple):**
```  
V5 App → Direct APIs → Real-time data
      ↘ GitHub Actions as backup only (optional)
```

### **Development & Deployment Advantages:**

**Immediate Benefits:**
- **Faster development**: No intermediate JSON file coordination
- **Real-time data**: 30-second updates vs 10-minute batches
- **Simpler deployment**: No GitHub Actions dependency
- **Cleaner architecture**: Direct API → UI, no file intermediary
- **Better debugging**: Direct API errors vs file generation issues

**Eliminated Complexity:**
- GitHub repo branch management (data-updates)
- JSON file validation and commit logic
- GitHub Actions deployment coordination
- File-based data flow "hack" mechanism

### **Backup Strategy Evolution:**

**Current Issues with GitHub Actions Backup:**
- Clunky git-based storage mechanism
- Branch management complexity  
- Deployment coupling between data and code
- File-based communication is inelegant

**Proposed: Supabase Backup Strategy:**
- **Clean API**: RESTful backup data storage
- **Real-time sync**: Instant backup updates
- **Better reliability**: Database vs git file system
- **Easier fetching**: Standard HTTP API vs GitHub raw URLs
- **Decoupled**: Data storage independent of code repository

**Backup Architecture:**
```
Primary: V5 → Direct APIs (real-time)
Backup:  V5 → Supabase API (when primary fails)
Writer:  GitHub Actions → Supabase (every 10min, as backup only)
```

### **Transparent Data Source Strategy:**

**Key Insight**: Store raw API responses exactly as-is to ensure identical processing regardless of data source.

**Current Problem:**
- GitHub Actions: API → transform → custom JSON format
- Browser: custom JSON → different processing logic  
- Result: Two different data pipelines = complexity

**Proposed Solution:**
- GitHub Actions: API → store raw response as-is (no transformation)
- Browser: direct API OR backup file → **identical processing**
- Result: Single data pipeline = elegance

**Implementation:**
```javascript
// IDENTICAL processing whether from direct API or backup
async function getFigrData() {
  try {
    // Direct API call
    const response = await fetch('https://api.twelvedata.com/quote?symbol=FIGR&apikey=...');
    return await response.json();
  } catch (error) {
    // Backup file - SAME FORMAT as direct API
    const response = await fetch('https://backup-cdn.com/figr-quote.json');
    return await response.json();
  }
}

// Single processing function works for both sources
function processData(data) {
  return {
    price: parseFloat(data.price),
    change: parseFloat(data.percent_change),
    volume: parseInt(data.volume)
  };
}
```

**Benefits:**
- ✅ **Zero code duplication**: Same parsing logic for all sources
- ✅ **Transparent fallback**: Backup becomes invisible cache
- ✅ **Simplified testing**: One data pipeline to validate  
- ✅ **Consistent behavior**: Identical error handling and validation

### **Oracle-Validated Live-Data-First Architecture:**

**Root Cause of Current Issues:**
- Data source mixing: current price (GitHub Actions) + high/low (chart data) = logical errors
- FIGR shows low ($42.04) higher than current ($40.69) due to different data sources
- Solution: Consistent data source per asset

**Live-First Provider Tiering:**
```
Tier-1: Direct APIs (real-time) → PRIMARY
├── Figure Markets: HASH/BTC/ETH crypto data  
└── Twelve Data: FIGR stock quotes + charts
Tier-2: GitHub Actions JSON → FALLBACK ONLY
```

**Data Flow Architecture:**
```
┌───────────┐   raw JSON   ┌───────────────────┐  canonical map  ┌──────────────┐
│ Provider N │────────────►│  Transformer N    │────────────────►│  App state   │
└───────────┘              └───────────────────┘                 └──────────────┘
```

**Migration Strategy:**

**Phase 1: Live-Data-First Implementation (Current)**
1. Create canonical data format and transformer functions
2. Implement orchestrated fetch (parallel API calls)  
3. Add graceful per-provider fallback logic
4. Playwright testing at each step

**Phase 2: Production Rollout**
1. Feature flag rollout (percentage-based)
2. Dual-fetch validation (live vs backup comparison)
3. Monitor error rates and data consistency
4. Full rollout with GitHub Actions as backup only

**Phase 2: Backup Modernization (Optional)**
1. Set up Supabase database for backup data
2. Migrate GitHub Actions to write to Supabase
3. Add Supabase fallback logic to V5
4. Eliminate git-based data storage entirely

**Phase 3: Production Optimization**  
1. Monitor API reliability and rate limits
2. Implement smart caching strategies
3. Add user API key configuration for scaling
4. Fine-tune backup vs primary switching logic

## ✅ **Recent Completed Features (v5.1.1)**

**Period Persistence:**
- Selected chart periods (24H/1W/1M) now survive page refreshes
- localStorage integration with proper initialization sequence
- Global period changes affect all assets consistently

**Portfolio Persistence Fixed:**
- V5 portfolio quantities now persist across page refreshes  
- Root cause: Duplicate event handlers between V3/V4/V5 systems
- Solution: Disabled conflicting V4 handler, consolidated to V5 system
- Data flow: CLJS map → localStorage → proper restoration on startup

**Architecture Lessons:**
- Version separation is critical for maintainability
- Mixing V3/V4/V5 systems created subtle "Frankenstein" bugs
- Silent event handler overwrites caused hard-to-debug failures
- Need better version boundaries and migration strategies

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

### 🚀 **V5 Prototype (NEW)**
- **Multi-Asset Grid**: Professional responsive grid supporting all cryptocurrencies
- **Square Chart Layout**: Terminal-style cards with square aspect ratio charts
- **Global Period Selection**: Universal 24H/1W/1M chart periods affecting all cards
- **Complete Portfolio System**: Individual card portfolios + total portfolio value summary
- **Currency Conversion**: Full currency conversion throughout V5 UI (prices, volume, portfolio)
- **Enhanced UX**: Optimized portfolio display (PV: $123.45 USD (quantity token) ✏️)
- **Correct Asset Descriptions**: "Provenance Blockchain HASH Token" and other accurate labels
- **Feature Flag System**: `?ui=v5` enables complete multi-asset experience alongside V4

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
- ✅ **FIGR Historical Data**: Alpha Vantage API (CORS-enabled, IPO data available)

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

#### 2. V5 Portfolio Integration
**Goal**: Add portfolio management to V5 cards with professional terminal styling  
**Why**: Complete V5 card functionality with portfolio tracking per asset  
**Visual Specification**:
```
┌─────────────────────────────────────────┐
│ HASH             [⚙️]        €0.052 │ ← Chart with overlays
│ $0.049 [EUR]  📈📉 CHART  ▲2.34% │
│[1W]                        €0.046 │  
└─────────────────────────────────────────┘
Figure Markets Hash Token                  ← Asset description
Volume: $1.2M    Trades: 45    [FM]        ← Stats row
────────────────────────────────────────── ← Separator
PV: 123.45 HASH  =  €1,234.00 EUR    [✏️] ← Portfolio section
```

**Two States**:
- **No holdings**: `[ + Add to portfolio ]` (dashed button, full width)
- **With holdings**: `PV: 123.45 HASH = €1,234.00 EUR [✏️]` (quantity + value + edit)

**Oracle Implementation Plan**:
1. **Data Layer**: Thin re-frame wrappers over existing portfolio atoms
   - `:portfolio/qty` subscription per asset
   - `:portfolio/value` subscription with currency conversion
   - `:portfolio/set-qty` and `:portfolio/remove` events
2. **UI Component**: `portfolio-section` component with terminal styling
   - Full-width bottom section with border separator
   - Dashed add button for empty state
   - Professional "tape" layout for holdings display
   - Currency-aware value conversion using existing system
3. **Modal Integration**: Reuse existing V4 portfolio panel system
   - Same `show-portfolio-panel` atom for edit functionality
   - Unified portfolio management across V4/V5
4. **Styling Guidelines**: overlay-tier3 palette, 44px tap targets, no animations

#### 3. Portfolio Performance Card (Future)
**Goal**: Dedicated portfolio card showing aggregated performance like individual assets  
**Why**: Users need subliminal awareness of overall portfolio health and trends  
**Dependencies**: Complete after V5 portfolio integration
**Implementation**: 
- Create `:portfolio-historical` subscription aggregating asset histories
- Calculate portfolio metrics (sum quantity × price at each time point)
- Reuse existing background-chart component with `:portfolio` pseudo-ID
- Portfolio card component with familiar asset card UX

#### 4. Auto-feed Selection (Future)
**Goal**: Automatically choose best data source per asset based on liquidity  
**Why**: Figure Markets has low liquidity for major cryptos vs established exchanges  
**Implementation**: 
- Auto-rank feeds by liquidity thresholds and freshness
- Fallback logic when Figure Markets volume < threshold → CoinGecko
- Show feed source as small indicator pill ("FM" / "CG")
- No manual selection required - smart defaults

#### 5. Extend V5 to All Assets
**Goal**: Apply proven V5 template to BTC, ETH, SOL, XRP, and other assets
**Why**: Scale the working HASH prototype to complete asset coverage
**Implementation**: 
- Modify v5-prototype-section to show multiple assets instead of just HASH
- Test period selection and currency conversion across all assets
- Ensure performance with multiple square charts and API calls
- Maintain feature flag system for safe rollout

### 📊 **MEDIUM PRIORITY** (Enhanced Subliminal Features)

#### 4. Chart Color Sentiment  
**Goal**: Green charts for positive trends, red for negative  
**Why**: Instant visual mood assessment across all assets  
**Implementation**: 
- Calculate price direction (current vs 24h ago)
- Dynamic chart colors based on positive/negative change
- Subtle but clear visual indicators

#### 5. FIGR Historical Charts - TWELVE DATA BREAKTHROUGH 🚀
**Goal**: Add FIGR stock historical data for V5 charts  
**Status**: MAJOR BREAKTHROUGH - Rich intraday data discovered!

**🎯 Research Evolution:**
1. **Alpha Vantage**: ❌ Daily-only data = boring straight lines (2-8 points)
2. **Stooq**: ❌ CORS blocked for browser requests  
3. **FinancialModelingPrep**: ❌ Requires paid API key
4. **Twelve Data**: ✅ BREAKTHROUGH - Rich intraday data available!

**🚀 Twelve Data Discovery:**
- **Symbol Reuse**: FIGR previously owned by First National Bank of Groton  
- **Current Data**: Figure Technologies data available since IPO (2025-09-11)
- **Intraday Available**: 5min, 15min intervals work perfectly for new IPO!
- **CORS Enabled**: `access-control-allow-origin: *` confirmed via curl
- **Browser Compatible**: Direct fetch() requests work flawlessly

**📊 Rich FIGR Data Available:**
- **5min Intervals**: 30+ points per trading day with real volatility
- **15min Intervals**: Perfect granularity for weekly charts  
- **Daily Data**: Complete IPO history (8 trading days)
- **Price Action**: $45.81 → $43.08 with natural intraday movement
- **Volume**: 132k-798k per 5min interval (active trading)

**🔥 Data Quality Comparison:**
```
Alpha Vantage: 44.96 ————————————— 43.09 (2 boring points)
Twelve Data:   45.81 ╱╲╱╲╱╲╱╲╱╲╱╲╱ 43.08 (30+ rich points)
```

**🎯 Implementation Strategy (REVISED):**
- **Intraday Intervals**: 
  - 24H → 5min intervals (up to 288 points)
  - 1W → 15min intervals (up to 672 points)  
  - 1M → 1day intervals (8 IPO trading days)
- **API Advantages**: 800 calls/day vs Alpha Vantage's 25
- **Data Filtering**: Remove 2022 symbol artifact data (pre-IPO)
- **Rich Charts**: Real financial volatility instead of straight lines

**🔧 Technical Implementation:**
- **Twelve Data API**: `https://api.twelvedata.com/time_series`
- **Dynamic Intervals**: Period-based interval selection
- **IPO Date Filtering**: Filter out pre-2025-09-11 junk data
- **Transform Function**: `twelve-data->chart-data` for V5 format
- **Standard Caching**: localStorage with version keys
- **Error Handling**: Handle API errors and rate limits

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
