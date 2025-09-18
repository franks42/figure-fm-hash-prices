# API Field Mapping & Standardization

*Generated from api-field-mapping.json - DO NOT EDIT MANUALLY*

## Field Mapping Table

| **Standardized Field** | **Type** | **Figure Markets API** | **Yahoo Finance API** | **CoinGecko API** | **Alpha Vantage API** | **Current Internal** | **Description** |
|------------------------|----------|------------------------|----------------------|-------------------|----------------------|---------------------|-----------------|
| **Trading Features** |
| `proTradeAvailable` | boolean | `proTradeAvailable` | - | - | - | - | Pro trading availability |
| `baseVolume24h` | string | `baseVolume24h` | - | - | - | - | 24h volume in base currency |
| **Timestamps** |
| `lastTradeTime` | number | - | `regularMarketTime` | `last_updated` | `07. latest trading day` | - | Last trade timestamp |
| `timestamp` | number | - | - | `last_updated` | - | - | Data fetch timestamp |
| `lastUpdate` | string | - | - | `last_updated` | - | - | Human-readable update time |
| **Extended Price Data** |
| `openPrice` | number | - | `regularMarketOpen` | - | `02. open` | - | Opening price |
| `previousClose` | number | - | `previousClose` | - | `08. previous close` | - | Previous closing price |
| `priceChange` | number | - | (calculated) | `price_change_24h` | `09. change` | - | Absolute price change |
| `fiftyTwoWeekHigh` | number | - | `fiftyTwoWeekHigh` | - | - | - | 52-week high |
| `fiftyTwoWeekLow` | number | - | `fiftyTwoWeekLow` | - | - | - | 52-week low |
| **Trading Specifications** |
| `denomExponent` | number | `denomExponent` | - | - | - | - | Base currency decimal places |
| `quoteExponent` | number | `quoteExponent` | - | - | - | - | Quote currency decimal places |
| `pricePrecision` | number | `pricePrecision` | - | - | - | - | Price decimal precision |
| `quantityPrecision` | number | `quantityPrecision` | - | - | - | - | Quantity decimal precision |
| `priceIncrement` | string | `priceIncrement` | - | - | - | - | Minimum price increment |
| `sizeIncrement` | string | `sizeIncrement` | - | - | - | - | Minimum quantity increment |
| `minTradeQuantity` | string | `minTradeQuantity` | - | - | - | - | Minimum trade size |
| **Market Information** |
| `marketLocation` | string | `marketLocation` | - | - | - | - | Market jurisdiction |
| `marketLocations` | array | `marketLocations` | - | - | - | - | Available market locations |
| `exchange` | string | - | `fullExchangeName` | - | - | - | Exchange name |
| `timezone` | string | - | `timezone` | - | - | - | Market timezone |
| **Basic Identification** |
| `assetId` | string | `id` | - | `id` | `01. symbol` | - | Unique asset identifier |
| `symbol` | string | `symbol` | `symbol` | `symbol` | `01. symbol` | `symbol` | Trading pair symbol (e.g., "BTC-USD") |
| `displayName` | string | `displayName` | `longName` | `name` | - | - | Human-readable asset name |
| `baseCurrency` | string | `denom` | - | - | - | - | Base currency (e.g., "BTC") |
| `quoteCurrency` | string | `quoteDenom` | `currency` | - | `09. currency` | - | Quote currency (e.g., "USD") |
| `assetType` | string | `marketType` | - | - | - | `type` | Asset type ("crypto", "stock", etc.) |
| **Company/Token Info** |
| `companyName` | string | - | `longName` | `name` | - | - | Full company/token name |
| `companyShortName` | string | - | `shortName` | - | - | - | Short company name |
| **Data Source** |
| `dataSource` | string | - | - | - | - | - | Data provider information |
| `baseFiatCurrency` | string | - | - | - | - | - | User's preferred base fiat currency for price display |
| **Pricing Data** |
| `currentPrice` | currencyAmount | `midMarketPrice` + `quoteDenom` | `regularMarketPrice` + `currency` | `current_price` + `USD` | `05. price` + `09. currency` | `usd` + `USD` | Current/mid market price with currency specification |
| `lastTradePrice` | number | `lastTradedPrice` | `regularMarketPrice` | `current_price` | `05. price` | `last_price` | Last executed trade price in user's default fiat currency |
| `bidPrice` | number | `bestBid` | `bid` | - | - | `bid` | Best bid price in user's default fiat currency |
| `askPrice` | number | `bestAsk` | `ask` | - | - | `ask` | Best ask price in user's default fiat currency |
| `indexPrice` | number | `indexPrice` | - | - | - | - | Reference/index price in user's default fiat currency |
| **24 Hour Statistics** |
| `priceChange24h` | number | `priceChange24h` | (calculated) | `price_change_24h` | `06. change` | `usd_24h_change` | Absolute price change |
| `priceChangePercent24h` | number | `percentageChange24h` | (calculated) | `price_change_percentage_24h` | `10. change percent` | - | Percentage price change |
| `volume24h` | currencyAmount | `volume24h` + `quoteDenom` | `regularMarketVolume` + `currency` | `total_volume` + `USD` | - | `usd_24h_vol` | 24-hour trading volume with currency specification |
| `baseVolume24h` | tokenAmount | `baseVolume24h` + `denom` | - | - | `06. volume` + `01. symbol` | - | 24-hour trading volume in base currency/token quantity |
| `high24h` | number | `high24h` | `regularMarketDayHigh` | `high_24h` | `03. high` | - | 24-hour high price |
| `low24h` | number | `low24h` | `regularMarketDayLow` | `low_24h` | `04. low` | - | 24-hour low price |
| `tradeCount24h` | number | `tradeCount24h` | - | - | - | `trades_24h` | Number of trades in 24h |
| **Blockchain/Contract Info** |
| `contractAddress` | array | `contractAddress` | - | `platforms` | - | - | Smart contract addresses |
| `contractAddressUrl` | array | `contractAddressUrl` | - | - | - | - | Blockchain explorer URLs |
| `unifiedCryptoassetId` | string | `unifiedCryptoassetId` | - | - | - | - | Universal crypto asset ID |
| **Market Cap & Valuation** |
| `marketCap` | currencyAmount | - | `marketCap` + `currency` | `market_cap` + `USD` | - | `usd_market_cap` | Market capitalization with currency specification |


## Currency Exchange Rate APIs

| **Standardized Field** | **Type** | **FreeCurrencyAPI** | **ExchangeRate.host** | **Fixer.io** | **Description** |
|------------------------|----------|--------------------|-----------------------|---------------|-----------------|
| **Base Currency Info** |
| `baseCurrency` | string | `base_currency` | `base` | `base` | Base currency code (e.g., "USD") |
| `date` | string | `date` | `date` | `date` | Rate date (YYYY-MM-DD) |
| `timestamp` | number | - | `timestamp` | `timestamp` | Unix timestamp |
| **Exchange Rates** |
| `chf` | number | `data.CHF` | `rates.CHF` | `rates.CHF` | CHF exchange rate |
| `inr` | number | `data.INR` | `rates.INR` | `rates.INR` | INR (Indian Rupee) exchange rate |
| `sgd` | number | `data.SGD` | `rates.SGD` | `rates.SGD` | SGD (Singapore Dollar) exchange rate |
| `jpy` | number | `data.JPY` | `rates.JPY` | `rates.JPY` | JPY exchange rate |
| `aud` | number | `data.AUD` | `rates.AUD` | `rates.AUD` | AUD exchange rate |
| `mxn` | number | `data.MXN` | `rates.MXN` | `rates.MXN` | MXN (Mexican Peso) exchange rate |
| `cad` | number | `data.CAD` | `rates.CAD` | `rates.CAD` | CAD exchange rate |
| `sek` | number | `data.SEK` | `rates.SEK` | `rates.SEK` | SEK (Swedish Krona) exchange rate |
| `vnd` | number | `data.VND` | `rates.VND` | `rates.VND` | VND (Vietnamese Dong) exchange rate |
| `thb` | number | `data.THB` | `rates.THB` | `rates.THB` | THB (Thai Baht) exchange rate |
| `hkd` | number | `data.HKD` | `rates.HKD` | `rates.HKD` | HKD (Hong Kong Dollar) exchange rate |
| `eur` | number | `data.EUR` | `rates.EUR` | `rates.EUR` | EUR exchange rate |
| `cny` | number | `data.CNY` | `rates.CNY` | `rates.CNY` | CNY (Chinese Yuan) exchange rate |
| `rates` | object | `data` | `rates` | `rates` | Currency rates object |
| `krw` | number | `data.KRW` | `rates.KRW` | `rates.KRW` | KRW (Korean Won) exchange rate |
| `nzd` | number | `data.NZD` | `rates.NZD` | `rates.NZD` | NZD (New Zealand Dollar) exchange rate |
| `brl` | number | `data.BRL` | `rates.BRL` | `rates.BRL` | BRL (Brazilian Real) exchange rate |
| `myr` | number | `data.MYR` | `rates.MYR` | `rates.MYR` | MYR (Malaysian Ringgit) exchange rate |
| `gbp` | number | `data.GBP` | `rates.GBP` | `rates.GBP` | GBP exchange rate |
| `nok` | number | `data.NOK` | `rates.NOK` | `rates.NOK` | NOK (Norwegian Krone) exchange rate |
| `aed` | number | `data.AED` | `rates.AED` | `rates.AED` | AED (UAE Dirham) exchange rate |
| **API Metadata** |
| `success` | boolean | - | `success` | `success` | API request success status |
| `source` | string | - | `source` | `source` | Data source information |

### Exchange Rate TypeScript Interface

```typescript
interface StandardizedExchangeRates {
  baseCurrency: string;
  date: string;
  timestamp?: number;
  rates: {
    [currencyCode: string]: number;
  };
  success?: boolean;
  source?: string;
}

// Common currency rates
interface CommonCurrencyRates {
  eur: number;   // Euro
  jpy: number;   // Japanese Yen
  gbp: number;   // British Pound
  cad: number;   // Canadian Dollar
  aud: number;   // Australian Dollar
  chf: number;   // Swiss Franc
  sgd: number;   // Singapore Dollar
  hkd: number;   // Hong Kong Dollar
  krw: number;   // Korean Won
  cny: number;   // Chinese Yuan
  inr: number;   // Indian Rupee
  brl: number;   // Brazilian Real
  mxn: number;   // Mexican Peso
  aed: number;   // UAE Dirham
  nok: number;   // Norwegian Krone
  sek: number;   // Swedish Krona
  nzd: number;   // New Zealand Dollar
  thb: number;   // Thai Baht
  vnd: number;   // Vietnamese Dong
  myr: number;   // Malaysian Ringgit
}
```

## Type Definitions

```typescript
// Currency amount types
interface CurrencyAmount {
  amount: number;
  currency: string; // USD, EUR, JPY, etc.
}

interface TokenAmount {
  amount: number;
  currency: string; // BTC, ETH, AAPL, etc.
}

interface StandardizedAsset {
  // Pro trading availability
  proTradeAvailable?: boolean;
  // 24h volume in base currency
  baseVolume24h?: string;
  // Last trade timestamp
  lastTradeTime?: number;
  // Data fetch timestamp
  timestamp?: number;
  // Human-readable update time
  lastUpdate?: string;
  // Opening price
  openPrice?: number;
  // Previous closing price
  previousClose?: number;
  // Absolute price change
  priceChange?: number;
  // 52-week high
  fiftyTwoWeekHigh?: number;
  // 52-week low
  fiftyTwoWeekLow?: number;
  // Base currency decimal places
  denomExponent?: number;
  // Quote currency decimal places
  quoteExponent?: number;
  // Price decimal precision
  pricePrecision?: number;
  // Quantity decimal precision
  quantityPrecision?: number;
  // Minimum price increment
  priceIncrement?: string;
  // Minimum quantity increment
  sizeIncrement?: string;
  // Minimum trade size
  minTradeQuantity?: string;
  // Market jurisdiction
  marketLocation?: string;
  // Available market locations
  marketLocations?: string[];
  // Exchange name
  exchange?: string;
  // Market timezone
  timezone?: string;
  // Unique asset identifier
  assetId?: string;
  // Trading pair symbol (e.g., "BTC-USD")
  symbol: string;
  // Human-readable asset name
  displayName?: string;
  // Base currency (e.g., "BTC")
  baseCurrency?: string;
  // Quote currency (e.g., "USD")
  quoteCurrency?: string;
  // Asset type ("crypto", "stock", etc.)
  assetType: string;
  // Full company/token name
  companyName?: string;
  // Short company name
  companyShortName?: string;
  // Data provider information
  dataSource?: string;
  // User's preferred base fiat currency for price display
  baseFiatCurrency?: string;
  // Current/mid market price with currency specification
  currentPrice: CurrencyAmount;
  // Last executed trade price in user's default fiat currency
  lastTradePrice?: number;
  // Best bid price in user's default fiat currency
  bidPrice?: number;
  // Best ask price in user's default fiat currency
  askPrice?: number;
  // Reference/index price in user's default fiat currency
  indexPrice?: number;
  // Absolute price change
  priceChange24h?: number;
  // Percentage price change
  priceChangePercent24h?: number;
  // 24-hour trading volume with currency specification
  volume24h?: CurrencyAmount;
  // 24-hour trading volume in base currency/token quantity
  baseVolume24h?: TokenAmount;
  // 24-hour high price
  high24h?: number;
  // 24-hour low price
  low24h?: number;
  // Number of trades in 24h
  tradeCount24h?: number;
  // Smart contract addresses
  contractAddress?: string[];
  // Blockchain explorer URLs
  contractAddressUrl?: string[];
  // Universal crypto asset ID
  unifiedCryptoassetId?: string;
  // Market capitalization with currency specification
  marketCap?: CurrencyAmount;
}
```

## Key Standardization Decisions

1. **Naming Convention**: camelCase for all fields (JavaScript/TypeScript friendly)
2. **Price Fields**: All prices as `number` type (no strings)
3. **Consistent Terminology**:
   - `Price` suffix for all price-related fields
   - `24h` suffix for 24-hour statistics
   - `Percent` for percentage values
4. **Core Fields**: Every asset must have `symbol`, `currentPrice`, `assetType`
5. **Optional Fields**: Most fields optional to accommodate different API capabilities
6. **Type Safety**: Strong typing with enums for `assetType`

## Implementation Notes

- **Field Mapping Function**: Create utility functions to map from each API format to standardized format
- **Validation**: Ensure required fields are present and types are correct
- **Fallbacks**: Handle missing fields gracefully with sensible defaults
- **Performance**: Consider caching mapped data to avoid repeated transformations

### Currency Conversion Implementation

```typescript
// Example mapping functions for each API
function mapFigureMarketsData(apiResponse: any): StandardizedAsset {
  return {
    assetId: apiResponse.id,
    symbol: apiResponse.symbol,
    displayName: apiResponse.displayName,
    baseCurrency: apiResponse.denom,
    quoteCurrency: apiResponse.quoteDenom,
    assetType: "crypto",
    currentPrice: Number(apiResponse.midMarketPrice),
    bidPrice: Number(apiResponse.bestBid),
    askPrice: Number(apiResponse.bestAsk),
    volume24h: Number(apiResponse.volume24h),
    // ... map other fields
  };
}

function mapYahooFinanceData(apiResponse: any): StandardizedAsset {
  return {
    symbol: apiResponse.symbol,
    displayName: apiResponse.longName,
    quoteCurrency: apiResponse.currency,
    assetType: "stock",
    currentPrice: apiResponse.regularMarketPrice,
    bidPrice: apiResponse.bid,
    askPrice: apiResponse.ask,
    volume24h: apiResponse.regularMarketVolume,
    // ... map other fields
  };
}

function mapCoinGeckoData(apiResponse: any): StandardizedAsset {
  return {
    assetId: apiResponse.id,
    symbol: apiResponse.symbol?.toUpperCase(),
    displayName: apiResponse.name,
    assetType: "crypto",
    currentPrice: apiResponse.current_price,
    marketCap: apiResponse.market_cap,
    volume24h: apiResponse.total_volume,
    priceChangePercent24h: apiResponse.price_change_percentage_24h,
    // ... map other fields
  };
}

function mapExchangeRateData(apiResponse: any, provider: string): StandardizedExchangeRates {
  switch (provider) {
    case 'freecurrencyapi':
      return {
        baseCurrency: apiResponse.base_currency || 'USD',
        date: apiResponse.date,
        rates: apiResponse.data,
        source: 'FreeCurrencyAPI'
      };
    case 'exchangerate.host':
      return {
        baseCurrency: apiResponse.base,
        date: apiResponse.date,
        timestamp: apiResponse.timestamp,
        rates: apiResponse.rates,
        success: apiResponse.success,
        source: 'ExchangeRate.host'
      };
    default:
      throw new Error(`Unknown exchange rate provider: ${provider}`);
  }
}
```

---

*Last updated: 2025-09-17*
*Generated from: api-field-mapping.json v1.1.0*