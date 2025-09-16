# Crypto Price Tracker - Architecture & Implementation Guide

## Overview

This is a real-time cryptocurrency price tracker built with ClojureScript/Scittle that displays prices from Figure Markets and Yahoo Finance. The application emphasizes smooth user experience with selective updates, visual feedback, and glass morphism design.

## Key Design Principles

1. **No Jarring Refreshes** - Uses fine-grained state management to update only changed data
2. **Visual Feedback** - Clear indicators when data fetching occurs
3. **Serverless Architecture** - GitHub Actions + GitHub Pages for zero-cost hosting
4. **Browser-Only ClojureScript** - Uses Scittle for client-side Clojure compilation
5. **Responsive Design** - Modern glass morphism UI with Tailwind CSS

## Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   GitHub        │    │   GitHub Pages   │    │   Browser       │
│   Actions       │───▶│   (Static Host)  │───▶│   (Scittle)     │
│   (Data Fetch)  │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                                               │
         ▼                                               ▼
┌─────────────────┐                            ┌─────────────────┐
│ Figure Markets  │                            │ Reagent Components│
│ + Yahoo Finance │                            │ + State Atoms   │
│ APIs            │                            │                 │
└─────────────────┘                            └─────────────────┘
```

## Technology Stack

### Frontend
- **ClojureScript** - Functional programming language compiling to JavaScript
- **Scittle** - Browser-based ClojureScript interpreter (no build step)
- **Reagent** - ClojureScript wrapper for React
- **Tailwind CSS** - Utility-first CSS framework with JIT compilation

### Backend/Data Pipeline
- **GitHub Actions** - Automated data fetching every 10 minutes
- **Figure Markets API** - Cryptocurrency price data
- **Yahoo Finance API** - FIGR stock data
- **jq** - JSON processing in GitHub Actions

### Hosting
- **GitHub Pages** - Static site hosting
- **No server required** - Purely client-side application

## File Structure

```
├── index.html                    # Main HTML with Tailwind config
├── src/crypto_app.cljs          # Main ClojureScript application
├── data/crypto-prices.json     # Auto-generated price data
├── .github/workflows/
│   └── fetch-crypto-data.yml   # GitHub Action for data fetching
└── ARCHITECTURE.md             # This documentation
```

## Core Implementation Details

### 1. Fine-Grained State Management

The application uses separate Reagent atoms for different data aspects to enable selective updates:

```clojure
;; Separate atoms for fine-grained updates
(def prices-atom (r/atom {}))           ; Individual coin prices
(def price-keys-atom (r/atom []))       ; Available cryptocurrencies
(def last-update-atom (r/atom nil))     ; Fetch timestamp
(def loading-atom (r/atom true))        ; Loading state
(def error-atom (r/atom nil))           ; Error state
(def update-flash-atom (r/atom false))  ; Visual feedback
```

### 2. Selective Update Algorithm

The app compares primitive values to detect actual changes:

```clojure
(doseq [[coin-id new-data] prices]
  (let [old-data (get old-prices coin-id)
        old-price (get old-data "usd")
        new-price (get new-data "usd")
        old-change (get old-data "usd_24h_change")
        new-change (get new-data "usd_24h_change")]
    (when (or (not= old-price new-price) 
              (not= old-change new-change))
      (swap! prices-atom assoc coin-id new-data))))
```

**Why This Works:**
- Reagent cursors like `@(r/cursor prices-atom [crypto-id])` only re-render when that specific coin's data changes
- Comparing primitive values avoids false positives from `js->clj` object recreation
- Only updates atoms when data actually changes

### 3. Data Pipeline (GitHub Actions)

The `fetch-crypto-data.yml` workflow:

1. **Fetches Data** from two sources:
   - Figure Markets API: `https://www.figuremarkets.com/service-hft-exchange/api/v1/markets`
   - Yahoo Finance API: `https://query1.finance.yahoo.com/v8/finance/chart/FIGR`

2. **Processes with jq** to create unified JSON:
   ```bash
   jq --null-input \
      --argjson crypto "$CRYPTO_RESPONSE" \
      --argjson stock "$STOCK_RESPONSE" \
      '...' > data/crypto-prices.json
   ```

3. **Commits & Pushes** updated data to GitHub Pages

### 4. Visual Feedback System

#### Top Scan Line
- Fixed position indicator at top of page
- Animated sweep during data fetching
- CSS keyframe animation with linear timing

```css
.fetch-indicator.active {
    opacity: 1;
    animation: fetch-scan 2s linear;
}
```

#### Bottom Flash Indicator
- Green flash when data updates
- Shows "Last updated" timestamp
- Uses `animate-ping` for visual emphasis

### 5. Cache Busting

Prevents browser caching of JSON data:

```clojure
(js/fetch (str "./data/crypto-prices.json?t=" (js/Date.now)))
```

## Component Architecture

### Main Components

1. **`crypto-card`** - Individual cryptocurrency display
   - Uses Reagent cursor for selective updates
   - Glass morphism styling with hover effects
   - Displays price, change, volume, trades, bid/ask

2. **`app-component`** - Main application container
   - Handles loading, error, and success states
   - Sorts cryptocurrencies (HASH first, FIGR second)
   - Shows update indicator with timestamp

3. **`fetch-crypto-data`** - Data fetching function
   - Manages visual indicators
   - Handles differential updates
   - Updates timestamp on every fetch

## State Flow

```
Initial Load:
├── Show loading spinner
├── Fetch data from JSON
├── Parse and populate all atoms
├── Hide loading, show cards
└── Start 5-second interval

Subsequent Updates:
├── Show top scan line
├── Fetch fresh JSON data
├── Compare with existing data
├── Update only changed coins
├── Update timestamp (always)
├── Flash green indicator
└── Hide scan line after 2s
```

## Performance Optimizations

### 1. Selective Rendering
- Only components with changed data re-render
- Reagent cursors provide automatic optimization
- Primitive value comparison prevents false updates

### 2. Loading State Management
- Loading states only shown on initial load
- Background fetches are silent
- Prevents screen flashing every 5 seconds

### 3. Animation Efficiency
- CSS animations over JavaScript
- Hardware-accelerated transforms
- Minimal DOM manipulation

## Data Format

The `crypto-prices.json` follows this structure:

```json
{
  "btc": {
    "usd": 115352.73,
    "usd_24h_change": 0.3272,
    "usd_24h_vol": 427974.095095,
    "symbol": "BTC-USD",
    "bid": 115287.90,
    "ask": 115417.56,
    "last_price": 115177.99,
    "trades_24h": 37,
    "type": "crypto"
  },
  "figr": {
    "usd": 37.33,
    "usd_24h_change": 14.8,
    "usd_24h_vol": 1500000,
    "symbol": "FIGR",
    "type": "stock",
    "day_high": 40.39,
    "day_low": 33.47
  },
  "timestamp": 1757985823,
  "source": "figuremarkets+yahoo",
  "last_update": "2025-09-16 01:23:43 UTC"
}
```

## Styling Approach

### Tailwind Configuration
- Custom colors for neon effects
- Custom animations for glass morphism
- JIT compilation for dynamic classes

### Key Visual Elements
- **Glass morphism cards** - `bg-white/[0.03] backdrop-blur-lg`
- **Neon gradients** - `from-neon-green via-neon-cyan to-neon-pink`
- **Floating background** - Animated radial gradients
- **Scan line effects** - Linear gradient animations

## Error Handling

### Network Failures
- Error state in UI with retry message
- Automatic retry every 30 seconds
- Console logging for debugging

### Data Validation
- Null checks for all numeric values
- Fallback to zero for missing data
- Type checking in data processing

## CORS Solution

Since browsers block direct API calls to external services, the app uses GitHub Actions as a proxy:

1. **GitHub Actions** run server-side (no CORS restrictions)
2. **Fetch APIs** and process data with jq
3. **Commit results** to repository
4. **GitHub Pages** serves static JSON
5. **Browser** fetches from same domain (no CORS issues)

## Development Workflow

### Local Development
```bash
# Start local server
python3 -m http.server 8000

# Format code
cljfmt fix src/crypto_app.cljs

# Lint code
clj-kondo --lint src/crypto_app.cljs
```

### Deployment
1. Push changes to GitHub
2. GitHub Pages automatically deploys
3. GitHub Actions update data every 10 minutes

## Testing Selective Updates

To verify selective updates work:

1. **Manually edit** `data/crypto-prices.json`
2. **Change HASH price** to different value
3. **Wait 5 seconds** for next fetch
4. **Observe** only HASH card updates

## Key Challenges Solved

### 1. Screen Refresh Elimination
**Problem:** Full page re-renders every 30 seconds
**Solution:** Fine-grained state atoms + primitive value comparison

### 2. CORS Restrictions
**Problem:** Cannot fetch APIs directly from browser
**Solution:** GitHub Actions as server-side data pipeline

### 3. Animation Conflicts
**Problem:** Tailwind CSS resets conflicted with custom animations
**Solution:** Moved keyframes to Tailwind config with proper z-index

### 4. Cache Issues
**Problem:** Browser cached old JSON data
**Solution:** Timestamp-based cache busting

## Future Enhancements

### Potential Features
- Portfolio tracking (localStorage-based)
- Price alerts
- Historical charts
- More cryptocurrencies
- Mobile app (React Native + ClojureScript)

### Architecture Improvements
- WebSocket for real-time updates
- Service worker for offline support
- Progressive Web App features
- Error boundary components

## For Other AI Assistants

### Key Context for Modifications
1. **Always use fine-grained atoms** - Never merge back to single app-state
2. **Primitive value comparison** - Don't compare full objects from `js->clj`
3. **Visual feedback is crucial** - Users expect to see when updates occur
4. **Code quality matters** - Always run `cljfmt` and `clj-kondo`
5. **Selective updates are the core feature** - Any changes that break this defeat the purpose

### Common Pitfalls
- Using `=` on `js->clj` objects (always returns false)
- Updating loading states on background fetches (causes screen flash)
- Removing cache busting (causes stale data)
- Merging atoms back into single state (breaks selective rendering)

### Testing Guidelines
- Always test selective updates with manual JSON edits
- Verify no screen flashing during updates
- Check visual indicators work properly
- Ensure timestamps update correctly

This architecture provides a smooth, responsive user experience while maintaining clean, functional code patterns typical of ClojureScript applications.