# ClojureScript Crypto App with GitHub Actions Data Pipeline

## Overview

This guide shows how to build a live cryptocurrency data display app using ClojureScript/Scittle hosted on GitHub Pages, with GitHub Actions automatically fetching and updating crypto data to bypass CORS limitations.

## The Problem

Building a client-side crypto app faces a common challenge: **CORS restrictions** prevent direct API calls from the browser to most cryptocurrency exchanges. Traditional solutions require backend servers or proxy services.

## The Solution

Use GitHub Actions as a scheduled data fetcher that stores results in your repository, which your ClojureScript app can then fetch without CORS issues.

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   GitHub Pages  │    │ GitHub Actions   │    │ Crypto APIs     │
│                 │    │                  │    │                 │
│ ClojureScript   │◄───┤ Scheduled Fetch  │◄───┤ CoinGecko       │
│ App fetches     │    │ Every 5-10 min   │    │ Binance         │
│ /data/prices.json│   │ Saves to repo    │    │ Coinbase        │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Implementation

### 1. Repository Structure

```
your-crypto-app/
├── .github/workflows/
│   └── fetch-crypto-data.yml
├── data/
│   └── crypto-prices.json        # Auto-generated
├── js/
│   └── app.cljs                  # Your ClojureScript app
├── index.html                    # Main page
└── README.md
```

### 2. GitHub Action Workflow

Create `.github/workflows/fetch-crypto-data.yml`:

```yaml
name: Fetch Crypto Data

on:
  schedule:
    # Run every 10 minutes (GitHub's minimum is 5 min)
    - cron: '*/10 * * * *'
  workflow_dispatch: # Allow manual trigger

permissions:
  contents: write

jobs:
  fetch-data:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout repository
      uses: actions/checkout@v4
      
    - name: Fetch crypto data
      run: |
        mkdir -p data
        
        # Option 1: Use CoinGecko (CORS-enabled, free)
        curl -s "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,cardano,polkadot,chainlink&vs_currencies=usd&include_24hr_change=true&include_24hr_vol=true" | \
        jq '. + {"timestamp": now, "source": "coingecko", "last_update": (now | strftime("%Y-%m-%d %H:%M:%S UTC"))}' > data/crypto-prices.json
        
        # Option 2: Multiple sources (commented out)
        # curl -s "https://api.binance.com/api/v3/ticker/24hr" > temp_binance.json
        # curl -s "https://api.coinbase.com/v2/exchange-rates" > temp_coinbase.json
        
    - name: Commit and push if changed
      run: |
        git config --local user.email "action@github.com"
        git config --local user.name "GitHub Action"
        git add data/crypto-prices.json
        
        # Only commit if there are changes
        if ! git diff --staged --quiet; then
          git commit -m "Update crypto data $(date -u +%Y%m%d-%H%M%S)"
          git push
        fi
```

### 3. Alternative: Using Existing Actions

You can also use community-maintained actions:

```yaml
name: Fetch Crypto Data (Using Action)

on:
  schedule:
    - cron: '*/10 * * * *'
  workflow_dispatch:

permissions:
  contents: write

jobs:
  fetch-data:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout
      uses: actions/checkout@v4
      
    - name: Fetch API Data
      uses: JamesIves/fetch-api-data-action@v2
      with:
        endpoint: https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,cardano&vs_currencies=usd&include_24hr_change=true
        
    - name: Deploy Data
      uses: JamesIves/github-pages-deploy-action@v4
      with:
        branch: main
        folder: fetch-api-data-action
        target-folder: data
```

### 4. ClojureScript/Scittle Application

Create `index.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Crypto Price Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/scittle@0.6.17/dist/scittle.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .crypto-card { 
            border: 1px solid #ddd; 
            border-radius: 8px; 
            padding: 15px; 
            margin: 10px 0; 
            background: #f9f9f9; 
        }
        .price { font-size: 1.5em; font-weight: bold; }
        .change-positive { color: #4CAF50; }
        .change-negative { color: #f44336; }
        .loading { color: #666; font-style: italic; }
    </style>
</head>
<body>
    <h1>🚀 Crypto Price Dashboard</h1>
    <div id="app">
        <div class="loading">Loading crypto data...</div>
    </div>

    <script type="application/x-scittle">
        (ns crypto-app
          (:require [clojure.string :as str]))

        (def app-state (atom {:prices {} :last-update nil :loading true}))

        (defn format-price [price]
          (str "$" (.toLocaleString price "en-US" #js{:minimumFractionDigits 2 :maximumFractionDigits 2})))

        (defn format-change [change]
          (let [formatted (str (.toFixed (js/Math.abs change) 2) "%")
                class (if (>= change 0) "change-positive" "change-negative")
                symbol (if (>= change 0) "+" "")]
            [:span {:class class} (str symbol formatted)]))

        (defn crypto-card [crypto-id data]
          (let [name (str/capitalize (name crypto-id))
                price (get data "usd")
                change (get data "usd_24h_change")]
            [:div {:class "crypto-card"}
             [:h3 name]
             [:div {:class "price"} (format-price price)]
             [:div "24h change: " (format-change change)]]))

        (defn app-component []
          (let [{:keys [prices last-update loading]} @app-state]
            [:div
             (if loading
               [:div {:class "loading"} "Loading crypto data..."]
               [:div
                (for [[crypto-id data] prices]
                  ^{:key crypto-id} [crypto-card crypto-id data])
                (when last-update
                  [:div {:style {:margin-top "20px" :font-size "0.9em" :color "#666"}}
                   "Last updated: " last-update])])]))

        (defn fetch-crypto-data []
          (-> (js/fetch (str js/window.location.origin "/data/crypto-prices.json"))
              (.then #(.json %))
              (.then (fn [data]
                       (let [js-data (js->clj data :keywordize-keys true)
                             prices (dissoc js-data :timestamp :source :last_update)
                             last-update (:last_update js-data)]
                         (swap! app-state assoc 
                                :prices prices 
                                :last-update last-update 
                                :loading false))))
              (.catch #(do 
                        (js/console.error "Failed to fetch crypto data:" %)
                        (swap! app-state assoc :loading false)))))

        (defn render-app []
          (let [app-element (js/document.getElementById "app")]
            (set! (.-innerHTML app-element) "")
            (.appendChild app-element 
                         (js/document.createTextNode 
                          (str (app-component))))))

        ;; Simple virtual DOM rendering (you might want to use Reagent for production)
        (defn render-to-string [component]
          (cond
            (vector? component)
            (let [[tag attrs & children] component
                  attrs-str (if (map? attrs)
                             (str/join " " (map (fn [[k v]] (str (name k) "=\"" v "\"")) attrs))
                             "")
                  children (if (map? attrs) children (cons attrs children))]
              (str "<" (name tag) (when (seq attrs-str) (str " " attrs-str)) ">"
                   (str/join "" (map render-to-string children))
                   "</" (name tag) ">"))
            
            (string? component) component
            (number? component) (str component)
            :else (str component)))

        (defn update-dom []
          (let [app-element (js/document.getElementById "app")]
            (set! (.-innerHTML app-element) (render-to-string (app-component)))))

        ;; Initialize app
        (add-watch app-state :dom-update (fn [_ _ _ _] (update-dom)))

        ;; Fetch data immediately and then every 30 seconds
        (fetch-crypto-data)
        (js/setInterval fetch-crypto-data 30000)
    </script>
</body>
</html>
```

### 5. GitHub Actions Quotas & Limitations

**Free Tier Limits:**
- **Public repos**: Unlimited minutes
- **Private repos**: 2,000 minutes/month
- **Minimum cron interval**: 5 minutes
- **Storage**: 500 MB for artifacts

**Recommended Settings:**
- Run every 10 minutes (288 runs/day)
- Use public repository for unlimited usage
- Each run takes ~30 seconds = ~144 minutes/day

**Rate Limiting:**
- GitHub: 1,000 API requests/hour per repo
- CoinGecko: 50 calls/minute (demo plan)
- Binance: 1,200 requests/minute

## Real-World Examples

This pattern is widely used in production:

1. **GitHub's Official Demo**: [flat-demo-bitcoin-price](https://github.com/githubocto/flat-demo-bitcoin-price)
2. **Crypto Data Pipelines**: Multiple projects using GitHub Actions for ETL
3. **Price Tracking Apps**: Automated daily crypto price trackers
4. **Popular Actions**:
   - `JamesIves/fetch-api-data-action` (2k+ stars)
   - `gautemo/fetch-api-data-action`
   - GitHub's official Flat Data Action

## Alternative Data Sources

### CORS-Enabled APIs (Direct Access)
```clojure
;; These can be called directly from ClojureScript
(def cors-enabled-apis
  {:coingecko "https://api.coingecko.com/api/v3/simple/price"
   :coincap   "https://api.coincap.io/v2/assets"
   :binance   "https://api.binance.com/api/v3/ticker/price"}) ; Some endpoints
```

### WebSocket Connections (No CORS)
```clojure
;; WebSockets bypass CORS entirely
(def websocket-feeds
  {:binance    "wss://stream.binance.com:9443/ws/btcusdt@ticker"
   :coinbase   "wss://ws-feed.pro.coinbase.com"
   :kraken     "wss://ws.kraken.com"})
```

## Advanced Features

### Multiple Data Sources
```yaml
- name: Fetch from multiple sources
  run: |
    # Fetch from different exchanges
    curl -s "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd" > temp_coingecko.json
    curl -s "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT" > temp_binance.json
    
    # Combine data
    node -e "
      const coingecko = JSON.parse(require('fs').readFileSync('temp_coingecko.json'));
      const binance = JSON.parse(require('fs').readFileSync('temp_binance.json'));
      
      const combined = {
        timestamp: Date.now(),
        sources: {
          coingecko: coingecko,
          binance: { btc_usdt: parseFloat(binance.price) }
        }
      };
      
      require('fs').writeFileSync('data/crypto-prices.json', JSON.stringify(combined, null, 2));
    "
```

### Error Handling & Notifications
```yaml
- name: Notify on failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: failure
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

## Security Considerations

1. **API Keys**: Store in GitHub Secrets if needed
2. **Rate Limiting**: Respect API limits to avoid bans
3. **Data Validation**: Verify JSON structure before committing
4. **Repository Visibility**: Use public repos for unlimited Actions

## Getting Started

1. Create a new GitHub repository
2. Add the workflow file to `.github/workflows/`
3. Create your ClojureScript app in `index.html`
4. Enable GitHub Pages in repository settings
5. Manually trigger the action to test
6. Access your app at `https://username.github.io/repo-name`

## Benefits

- ✅ **No CORS issues**: Data served from same origin
- ✅ **No backend required**: Pure static hosting
- ✅ **Automatic updates**: Set-and-forget data pipeline
- ✅ **Version controlled**: All data changes tracked in Git
- ✅ **Free hosting**: GitHub Pages + Actions free tier
- ✅ **Battle-tested**: Used by thousands of developers

## Limitations

- ⚠️ **Not real-time**: 5-10 minute update intervals
- ⚠️ **GitHub dependencies**: Relies on GitHub infrastructure
- ⚠️ **Rate limits**: API and Actions quotas apply
- ⚠️ **Public data**: Best for public repositories

This approach provides a robust, cost-effective solution for building crypto data applications without traditional backend infrastructure.