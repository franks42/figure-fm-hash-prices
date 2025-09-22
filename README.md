# Figure Markets Hash Prices Tracker

A real-time cryptocurrency and digital asset price tracking system with multi-currency support and portfolio management. Features modern, reactive UI for tracking Figure Markets assets (HASH, FIGR) alongside major cryptocurrencies.

## Current Version

- **V4.0.0 (Production)**: Re-frame architecture with background price charts - `index.html`

## Architecture Overview

Modern reactive crypto tracker with real-time background price charts and multi-currency portfolio management.

### Core Features  
- ✅ **Background Price Charts**: 24h historical data from Figure Markets API (browser-direct, CORS enabled)
- ✅ **Multi-Currency Support**: 10 global currencies with live exchange rate conversion
- ✅ **Portfolio Management**: Persistent holdings with real-time value calculations
- ✅ **Robust Data Pipeline**: Fault-tolerant GitHub Actions with fallback handling
- ✅ **Visual Feedback**: Stale data warnings, loading indicators, update animations
- ✅ **iOS Widget Ready**: Widget-optimized layouts for screenshot automation

### Technical Stack
- **Frontend**: ClojureScript + Scittle (no-build) + Re-frame + uPlot charts
- **Data**: Figure Markets API + Yahoo Finance with 10-minute updates  
- **Charts**: Real-time background charts with 24h historical price trends
- **Hosting**: GitHub Pages with automated data pipeline

## Interface Features

### Multi-Currency Support
- **10 Global Currencies**: USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, KRW, SEK
- **Real-time Exchange Rates**: Live currency conversion with exchange rate indicators
- **Persistent Preferences**: Selected currency saved across sessions
- **Visual Indicators**: Shows when using mock data vs. live exchange rates

### Enhanced Portfolio Management
- **Multi-currency Valuation**: View portfolio value in any supported currency
- **Persistent Storage**: Holdings saved locally with automatic restoration
- **Per-asset Management**: Individual portfolio panels for each asset
- **Live Calculations**: Real-time portfolio value updates with currency conversion

### Market Data Display
- **Figure Markets Assets**: HASH token and FIGR stock with specialized formatting
- **Major Cryptocurrencies**: BTC, ETH, LINK, SOL, UNI, XRP
- **Comprehensive Metrics**:
  - Current prices with currency conversion
  - 24h volume in selected currency
  - Bid/Ask spreads
  - Price change percentages
- **Real-time Updates**: 30-second auto-refresh with visual indicators

### User Experience
- **Glass-morphism Design**: Modern UI with transparency effects
- **Responsive Layout**: Optimized for desktop and mobile viewing
- **Visual Feedback**:
  - Scanning animation during data fetch
  - Flash indicators for price updates
  - Warning system for mock data usage
- **Error Handling**: Graceful fallbacks with clear error messaging

## Technical Architecture

### Data Pipeline
- **Active Pipeline**: `.github/workflows/fetch-crypto-data-nbb.yml`
  - Uses ClojureScript (nbb) for data processing
  - Runs every 10 minutes
  - Multi-source fallback: Yahoo Finance → Alpha Vantage → Hardcoded fallback
- **Automated Fetching**: GitHub Actions retrieve data every 10 minutes
- **Standardized Format**: All data normalized to consistent schema
- **Branch Strategy**: Data stored in `data-updates` branch, code in `main`

### Frontend Stack
- **ClojureScript + Scittle**: No-build reactive UI with Reagent
- **Tailwind CSS**: Utility-first styling with custom components
- **Modular Architecture**: Separate namespaces for state, effects, and views

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

## Deployment

### Live Interface
- **Production**: https://franks42.github.io/figure-fm-hash-prices/
- **Local Development**: `python3 -m http.server 8000`
- **Data Updates**: Automatic via GitHub Actions every 10 minutes

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