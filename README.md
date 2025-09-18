# Figure Markets Hash Prices Tracker

A real-time cryptocurrency and digital asset price tracking system that combines automated data fetching with a modern web interface. This project specifically tracks Figure Markets assets (HASH, FIGR) alongside major cryptocurrencies (BTC, ETH) with portfolio management capabilities.

## Architecture Overview

This project consists of two independent components working together:

### 1. Automated Data Pipeline (GitHub Actions)
- **Cron-based fetching**: Automatically retrieves market data every 5-15 minutes
- **Multi-source aggregation**: Fetches from Figure Markets API and external sources
- **Standardized data format**: Converts all API responses into a unified schema
- **Intelligent scheduling**: More frequent updates during market hours (9 AM - 6 PM EST, Mon-Fri)
- **Robust error handling**: Logs unmapped fields for continuous schema improvement

### 2. Web Interface (GitHub Pages + Scittle)
- **ClojureScript frontend**: Modern reactive UI built with Reagent and Scittle
- **Real-time updates**: Displays live market data without server infrastructure
- **Portfolio tracking**: Persistent portfolio management with localStorage
- **Responsive design**: Beautiful glass-morphism UI with Tailwind CSS
- **Modular architecture**: Separate ClojureScript modules for maintainability

## Features

### Market Data
- **Figure Markets Assets**: HASH token and FIGR stock with specialized formatting
- **Major Cryptocurrencies**: Bitcoin (BTC), Ethereum (ETH)
- **Comprehensive metrics**: Current prices, 24h volume, bid/ask spreads, price changes
- **Real-time updates**: Auto-refreshing data with visual indicators

### Portfolio Management
- **Holdings tracking**: Track quantities for each asset
- **Value calculations**: Real-time portfolio valuation
- **Persistent storage**: Holdings saved locally across sessions
- **Individual asset views**: Detailed breakdown per holding

### Data Sources
- **Figure Markets API**: Primary source for HASH and FIGR data
- **External APIs**: Backup sources for major cryptocurrencies
- **Fallback mechanisms**: Embedded test data for offline functionality

## Technical Implementation

### Standardized Data Dictionary
All market data is normalized into a consistent schema:

```json
{
  "symbol": "BTC-USD",
  "currentPrice": {"amount": 45000.50, "currency": "USD"},
  "volume24h": {"amount": 1500000000, "currency": "USD"},
  "priceChange24h": 1250.30,
  "bidPrice": 44999.00,
  "askPrice": 45001.00,
  "assetType": "crypto",
  "dataSource": "Figure Markets",
  "timestamp": 1705234567890
}
```

### Deployment
- **Live interface**: https://franks42.github.io/figure-fm-hash-prices/
- **V2 standardized format**: https://franks42.github.io/figure-fm-hash-prices/index-v2.html
- **Local development**: `python3 -m http.server 8000`

### GitHub Actions Workflows
- `fetch-standardized-data.yml`: Main data pipeline with intelligent scheduling
- `fetch-crypto-data-nbb.yml`: nbb-based alternative implementation
- Automated commits with timestamped updates