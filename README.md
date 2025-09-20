# Figure Markets Hash Prices Tracker

A real-time cryptocurrency and digital asset price tracking system with multi-currency support and portfolio management. Features modern, reactive UI for tracking Figure Markets assets (HASH, FIGR) alongside major cryptocurrencies.

## Current Versions

- **V2 (Stable)**: Direct Reagent atoms - `index-v2.html` 
- **V3 (Re-frame)**: Event-driven architecture - `index-v3.html` - **IN DEVELOPMENT**

## V3 Re-frame Migration (In Progress)

V3 is a complete rewrite using re-frame architecture for better state management and scalability.

### Architecture Changes
- **Event-driven State**: Single app-db with immutable updates via events
- **Reactive Subscriptions**: Declarative data flow with automatic UI updates  
- **Effect Handlers**: Isolated side effects for HTTP requests and localStorage
- **Hybrid Portfolio**: Plain Reagent atoms for portfolio data (avoids persistence issues)

### Current Status
- ✅ **Core Architecture**: Complete re-frame setup with events/subs/effects
- ✅ **Currency System**: 10-currency support with exchange rate conversion
- ✅ **Portfolio Management**: Persistent holdings with localStorage (hybrid approach)
- ✅ **Market Feed Indicators**: Data source display, scan line animations
- ✅ **UI Components**: All V2 components migrated to re-frame patterns
- 🔄 **Feature Parity**: Working towards 100% V2 compatibility
- 🔄 **Testing & Polish**: Debugging display issues and data field mappings

### Known Issues (V3)
- Deployment delays on GitHub Pages affecting testing
- Some UI components may have incorrect data field mappings
- Volume/High-Low displays being debugged

## Interface Features (V2 & V3)

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

### Live Interfaces
- **V2 (Stable)**: https://franks42.github.io/figure-fm-hash-prices/index-v2.html
- **V3 (Development)**: https://franks42.github.io/figure-fm-hash-prices/index-v3.html
- **Local Development**: `python3 -m http.server 8000`
- **Data Updates**: Automatic via GitHub Actions every 10 minutes

### Key Files

**V2 Implementation (Stable):**
- `index-v2.html`: V2 interface entry point
- `src/crypto_app_v2/`: ClojureScript modules
  - `state.cljs`: Application state and persistence
  - `views.cljs`: UI components and rendering
  - `effects.cljs`: Data fetching and side effects

**V3 Implementation (In Development):**
- `index-v3.html`: V3 re-frame interface entry point
- `src/crypto_app_v3/`: Re-frame architecture modules
  - `core.cljs`: Application initialization and mounting
  - `events.cljs`: Event handlers and business logic
  - `subs.cljs`: Subscriptions for reactive data access
  - `views.cljs`: UI components and rendering
  - `effects.cljs`: HTTP requests and side effects
  - `portfolio_atoms.cljs`: Hybrid portfolio state (plain atoms)