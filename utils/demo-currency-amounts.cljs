#!/usr/bin/env nbb

(ns demo-currency-amounts
  "Demo script showing the new currencyAmount and tokenAmount structures"
  (:require ["fs" :as fs]
            [clojure.pprint :as pprint]))

(defn demo-currency-amounts []
  "Demonstrate the currencyAmount and tokenAmount approach"
  (println "💰 Currency Amount Structure Demo")
  (println "=================================\n")

  (println "🎯 **The Problem We Solved:**")
  (println "Instead of assuming all prices are in 'user default currency',")
  (println "we now explicitly specify the currency for each amount.\n")

  (println "📊 **Old Approach** (ambiguous):")
  (pprint/pprint {"currentPrice" 45000.50
                  "volume24h" 25000000000})
  (println "   ❌ What currency is this in? USD? EUR? User's preference?\n")

  (println "✅ **New Approach** (explicit):")
  (pprint/pprint {"currentPrice" {"amount" 45000.50
                                  "currency" "USD"}
                  "volume24h" {"amount" 25000000000
                               "currency" "USD"}
                  "baseVolume24h" {"amount" 1500000
                                   "currency" "BTC"}})
  (println "   ✅ Crystal clear: price is $45,000.50 USD, volume is $25B USD, base volume is 1.5M BTC\n"))

(defn demo-api-mappings []
  "Show how different APIs map to the new structure"
  (println "🔄 **API Mapping Examples:**")
  (println "===========================\n")

  (println "🏢 **Figure Markets API Response:**")
  (pprint/pprint {"midMarketPrice" "45000.50"
                  "quoteDenom" "USD"
                  "volume24h" "25000000000"
                  "baseVolume24h" "1500000"
                  "denom" "BTC"})
  (println "   ⬇️ **Maps to:**")
  (pprint/pprint {"currentPrice" {"amount" 45000.50
                                  "currency" "USD"}
                  "volume24h" {"amount" 25000000000
                               "currency" "USD"}
                  "baseVolume24h" {"amount" 1500000
                                   "currency" "BTC"}})
  (println)

  (println "🥇 **CoinGecko API Response:**")
  (pprint/pprint {"current_price" 45000.50
                  "total_volume" 25000000000})
  (println "   ⬇️ **Maps to:**")
  (pprint/pprint {"currentPrice" {"amount" 45000.50
                                  "currency" "USD"}  ; CoinGecko defaults to USD
                  "volume24h" {"amount" 25000000000
                               "currency" "USD"}})
  (println)

  (println "📈 **Yahoo Finance API Response:**")
  (pprint/pprint {"regularMarketPrice" 150.25
                  "currency" "USD"
                  "regularMarketVolume" 50000000})
  (println "   ⬇️ **Maps to:**")
  (pprint/pprint {"currentPrice" {"amount" 150.25
                                  "currency" "USD"}
                  "volume24h" {"amount" 50000000
                               "currency" "USD"}})
  (println))

(defn demo-currency-conversion []
  "Show how currency conversion would work"
  (println "🌍 **Multi-Currency Support:**")
  (println "=============================\n")

  (println "📝 **Example: User wants EUR display**")
  (println "1. API gives us: {amount: 45000.50, currency: 'USD'}")
  (println "2. Exchange rate: 1 USD = 0.85 EUR")
  (println "3. Convert: 45000.50 × 0.85 = 38250.43")
  (println "4. Display: {amount: 38250.43, currency: 'EUR'}\n")

  (println "🔄 **Portfolio Value Calculation:**")
  (println "User holds:")
  (println "- 0.5 BTC @ $45,000.50 USD = $22,500.25 USD")
  (println "- 10 ETH @ $3,200.00 USD = $32,000.00 USD")
  (println "- Total: $54,500.25 USD")
  (println)
  (println "If user wants EUR:")
  (println "- $54,500.25 × 0.85 = €46,325.21 EUR\n"))

(defn demo-typescript-usage []
  "Show TypeScript usage examples"
  (println "⌨️  **TypeScript Usage:**")
  (println "========================\n")

  (println "```typescript")
  (println "interface CurrencyAmount {")
  (println "  amount: number;")
  (println "  currency: string; // USD, EUR, JPY, etc.")
  (println "}")
  (println)
  (println "interface TokenAmount {")
  (println "  amount: number;")
  (println "  currency: string; // BTC, ETH, AAPL, etc.")
  (println "}")
  (println)
  (println "// Example usage:")
  (println "const btcPrice: CurrencyAmount = {")
  (println "  amount: 45000.50,")
  (println "  currency: 'USD'")
  (println "};")
  (println)
  (println "const tradingVolume: TokenAmount = {")
  (println "  amount: 1500000,")
  (println "  currency: 'BTC'")
  (println "};")
  (println)
  (println "// Convert to user's preferred currency")
  (println "function convertCurrency(")
  (println "  amount: CurrencyAmount, ")
  (println "  targetCurrency: string,")
  (println "  exchangeRates: Record<string, number>")
  (println "): CurrencyAmount {")
  (println "  const rate = exchangeRates[targetCurrency];")
  (println "  return {")
  (println "    amount: amount.amount * rate,")
  (println "    currency: targetCurrency")
  (println "  };")
  (println "}")
  (println "```\n"))

(defn main []
  "Run all demos"
  (demo-currency-amounts)
  (demo-api-mappings)
  (demo-currency-conversion)
  (demo-typescript-usage)
  (println "✅ **Benefits of currencyAmount approach:**")
  (println "- 🎯 **Explicit**: Always know what currency each amount is in")
  (println "- 🌍 **Multi-currency**: Easy conversion between currencies")
  (println "- 🔒 **Type-safe**: TypeScript interfaces prevent currency mix-ups")
  (println "- 🧮 **Portfolio math**: Accurate calculations across different assets")
  (println "- 📊 **Clear display**: Users see exactly what currency they're viewing"))

;; Run the demo
(main)