# Figure Markets Hash Prices Tracker - V2 Interface

A real-time cryptocurrency and digital asset price tracking system with multi-currency support and portfolio management. The V2 interface provides a modern, reactive UI for tracking Figure Markets assets (HASH, FIGR) alongside major cryptocurrencies.

## V2 Interface Features

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
- **Automated Fetching**: GitHub Actions retrieve data every 5-15 minutes
- **Standardized Format**: All data normalized to consistent schema
- **Exchange Rates**: Updated from exchangerate-api.com
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

### Live V2 Interface
- **GitHub Pages**: https://franks42.github.io/figure-fm-hash-prices/index-v2.html
- **Local Development**: `python3 -m http.server 8000`
- **Data Updates**: Automatic via GitHub Actions

### Key Files
- `index-v2.html`: V2 interface entry point
- `src/crypto_app_v2/`: ClojureScript modules
  - `state.cljs`: Application state and persistence
  - `views.cljs`: UI components and rendering
  - `effects.cljs`: Data fetching and side effects