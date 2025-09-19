# Crypto Tracker Enhancements Roadmap

## 📅 Timestamp & Display
- [ ] **Add local timezone timestamp display**: Show timestamps in user's local timezone instead of UTC

## 🏗️ Data Standardization
- [ ] **Create standardized data dictionary for all market data fields**: Single source of truth for all market data field definitions
- [ ] **Convert all API responses to use standardized field names**: Ensure consistent naming across Figure Markets, Yahoo Finance, etc.
- [ ] **Add market source indicator for each token**: Display which exchange each token's data comes from

## 💰 Portfolio & Currency
- [ ] **Display portfolio value with both token count and fiat value**: Show both quantities AND fiat values
- [ ] **Add multi-fiat currency support (EUR, YEN, others)**: Support multiple fiat currencies beyond USD
- [ ] **Implement currency conversion rate feed**: Real-time exchange rates for fiat conversions

## 📈 Historic Data & Charts
- [ ] **Get historic data for all assets (24h, week, month, 6 month, 1 year)**: Collect comprehensive price history
- [ ] **Add optional graph display for historic data**: Interactive charts for price history visualization

## 🔒 HASH-Specific Features
- [ ] **Obtain HASH holdings with vesting information**: Get detailed vesting schedule information
- [ ] **Display spendable vs vesting HASH holdings breakdown**: Show available HASH vs locked/vesting amounts

## 🎨 UI/UX Enhancements
- [ ] **Replace card components with professional Figma UI components**: Upgrade to polished, professional design system components
- [ ] **Fix portfolio input: remove initial '0' to prevent cursor confusion**: Use empty field or placeholder instead of pre-filled "0"

---

## Implementation Notes

### Data Sources
- **Current**: Figure Markets API, Yahoo Finance API
- **Future**: Historic data APIs, currency conversion APIs, Provenance blockchain for HASH vesting

### Currency Exchange Rate APIs (Research Complete)
**Recommended Implementation Strategy:**

1. **FreeCurrencyAPI.com** ⭐ *Best for Development*
   - **FREE**: 5,000 monthly requests
   - **Currencies**: 32 major currencies (USD, EUR, JPY, GBP, etc.)
   - **Updates**: Daily
   - **Endpoints**: Latest, historical, time ranges
   - **Usage**: ~165 requests/day (perfect for hourly updates)

2. **ExchangeRate.host** ⭐ *Best for Production*
   - **FREE**: 100 monthly requests
   - **Currencies**: 168 currencies
   - **Updates**: Real-time with 99.99% uptime
   - **Historical**: 19 years of data
   - **Paid**: $14.99/month for 10,000 requests

3. **Fixer.io** ⭐ *Most Professional*
   - **Features**: 170+ currencies, 60-second updates
   - **Reliability**: Industry standard, bank-sourced data

**Implementation Plan:**
- Start with FreeCurrencyAPI.com for development (5,000 free requests)
- Upgrade to ExchangeRate.host for production reliability
- Example endpoints:
  ```
  https://api.freecurrencyapi.com/v1/latest?apikey=YOUR_KEY&currencies=EUR,JPY,GBP
  https://api.exchangerate.host/latest?base=USD&symbols=EUR,JPY,GBP
  ```

### Technology Stack
- **Frontend**: ClojureScript with Scittle (browser-based)
- **Data Fetching**: nbb-based GitHub Action (deployed and working)
- **UI Framework**: Reagent + Tailwind CSS
- **Charts**: Consider Chart.js, D3.js, or similar for historic data visualization

### Priorities
1. **Data standardization** - Foundation for all other features
2. **Historic data** - Core functionality for portfolio tracking
3. **Multi-currency support** - International user experience
4. **HASH vesting** - Unique value proposition
5. **UI overhaul** - Professional appearance

---

*Last updated: 2025-09-17*
*Status: Planning phase - ready for implementation*
*Currency API research: Complete ✅*