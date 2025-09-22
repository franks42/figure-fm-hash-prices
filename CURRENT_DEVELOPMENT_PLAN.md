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

#### 1. Portfolio Performance Card  
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

#### 7. USD Volume Estimation
**Goal**: Show meaningful volume comparisons across assets  
**Formula**: `Token Volume × (High + Low) / 2`  
**Why**: USD amounts more intuitive than token counts

#### 8. Volume Trend Charts  
**Goal**: Subliminal trading activity assessment over 1w/1m/3-6m  
**Why**: Gauge Figure Markets adoption vs established exchanges

#### 9. Osmosis DEX Integration
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
