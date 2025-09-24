# Figure Markets Hash Prices Tracker

A **V5-only** cryptocurrency and stock price tracking system with professional terminal-style interface. Features working portfolio modals, multi-asset grid layout, and real-time data integration.

## Current Version

- **V6.3.2 (UI Polish)**: Clean interface - updated title, removed prototype text, added version/settings
- **URL**: `http://localhost:8000/` (V5 interface) or `http://localhost:8000/simple.html` (demo)

## Live-Data-First Architecture

Real-time cryptocurrency and stock tracker with **direct API integration** and **Oracle-validated architecture**.

### ✅ **Core Features**
- **Subliminal Gradient Charts**: Chart fill colors based on percentage change magnitude (0.01% subtle → 10%+ intense)
- **V5 Terminal Interface**: Professional multi-asset grid layout with clean white lines/text
- **Working Modals**: Portfolio and currency selector modals (CSS clipping fixed)
- **localStorage Persistence**: Portfolio quantities and currency selection persist
- **Multi-Currency Support**: 6 currencies with working modal selector
- **Portfolio Management**: Add/edit/remove portfolio with modal interface
- **Real-time Data**: Direct API integration with Figure Markets and Twelve Data

### 🚀 **Live Data Sources (ONLY)**
- **Figure Markets API**: HASH, BTC, ETH, SOL, UNI, XRP, LINK, FIGR_HELOC (real-time)
- **Twelve Data API**: FIGR stock quotes + historical charts (real-time)
- **No backup data**: GitHub Actions completely disabled - live APIs only

### 🏗️ **Technical Stack**
- **Frontend**: ClojureScript + Scittle + Re-frame + Reagent + uPlot
- **APIs**: Direct browser calls with CORS support
- **Testing**: Playwright automated browser testing
- **Architecture**: Live-first with graceful fallback to backup data

## Interface Features

### 💱 **Real-Time Multi-Currency**
- **6 Major Currencies**: USD, EUR, GBP, JPY, CAD, AUD
- **Live Conversion**: All prices converted in real-time
- **Persistent Selection**: Currency choice saved across sessions
- **Interactive Selector**: Modal overlay for currency switching

### 📊 **Live Portfolio Management** 
- **Individual Holdings**: Per-asset portfolio quantities
- **Real-time Valuation**: Portfolio value in selected currency
- **Persistent Storage**: Holdings survive page refresh (V5 re-frame system)
- **Interactive Editing**: Modal overlay for quantity management
- **Total Value Display**: Aggregated portfolio value across all assets

### 📈 **Live Market Data**
- **Crypto Assets**: HASH, BTC, ETH, SOL, UNI, XRP, LINK, FIGR_HELOC (Figure Markets API)
- **Stock Data**: FIGR (Twelve Data API with real-time quotes)
- **Consistent Sources**: Current price + high/low from same API (no mixing)
- **Rich Charts**: 
  - Crypto: Figure Markets historical data
  - FIGR: Twelve Data intraday (5min/15min/daily intervals)
- **Live Updates**: 30-second polling with real-time API calls

### User Experience
- **Glass-morphism Design**: Modern UI with transparency effects
- **Responsive Layout**: Optimized for desktop and mobile viewing
- **Visual Feedback**:
  - Scanning animation during data fetch
  - Flash indicators for price updates
  - Warning system for mock data usage
- **Error Handling**: Graceful fallbacks with clear error messaging

## Live-Data-First Architecture

### 🎯 **Data Flow (Oracle-Validated)**
```
Tier-1: Direct APIs (Primary) → Real-time data
├── Figure Markets API: Crypto current prices + historical charts
└── Twelve Data API: FIGR stock quotes + historical charts

Tier-2: GitHub Actions (Backup) → 10-minute batch updates
└── Fallback when live APIs fail
```

### 🔄 **Canonical Data Pipeline**
```
┌─────────────┐   raw JSON   ┌─────────────────┐  V5 format   ┌─────────────┐
│ Provider API │─────────────►│ Transformer     │─────────────►│ App State   │
└─────────────┘               └─────────────────┘              └─────────────┘
```

### 🧪 **Testing Infrastructure**
- **Playwright**: Automated browser testing for all functionality
- **Test Scripts**: `test-current-state.js`, `test-buttons.js`, `test-live-data.js`
- **Mandatory Testing**: No functionality claimed working without Playwright verification

### Data Schema
```clojure
{:symbol "BTC-USD"
 :currentPrice {:amount 116662.10 :currency "USD"}
 :volume24h {:amount 237253.72 :currency "USD"}
 :priceChange24h 0.0292
 :bidPrice 116621.34
 :askPrice 116702.85
 :assetType "crypto"
 :dataSource "Figure Markets"
 :timestamp 1705234567890}
```

## Development & Testing

### 🧪 **Playwright Testing (Mandatory)**
```bash
# Install testing infrastructure
npm install playwright
npx playwright install chromium

# Test before claiming anything works
node test-current-state.js    # Basic functionality
node test-buttons.js          # UI interactions
node test-live-data.js        # API integration
```

### 🚀 **Local Development**
```bash
# Start development server
python3 -m http.server 8000

# V5 Interface URLs
http://localhost:8000/                # V5 interface (full features)
http://localhost:8000/simple.html     # V5 demo (fast loading)
```

### 🔧 **Code Quality**
```bash
# Always run after changes
clj-kondo --lint src/crypto_app_v3/
cljfmt fix src/crypto_app_v3/
```

### Key Files

**Current Implementation:**
- `index.html`: Application entry point
- `src/crypto_app_v3/`: Re-frame architecture modules
  - `core.cljs`: Application initialization and mounting
  - `events.cljs`: Event handlers and business logic
  - `subs.cljs`: Subscriptions for reactive data access
  - `views.cljs`: UI components and rendering
  - `effects.cljs`: HTTP requests and side effects
  - `portfolio.cljs`: Portfolio management
  - `portfolio_atoms.cljs`: Portfolio state management