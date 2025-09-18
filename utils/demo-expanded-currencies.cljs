#!/usr/bin/env nbb

(ns demo-expanded-currencies
  "Demo of expanded currency support including Asian and emerging market currencies"
  (:require [clojure.pprint :as pprint]))

(defn demo-current-currencies []
  "Show the currencies we now support"
  (println "🌍 **Expanded Currency Support**")
  (println "===============================\n")

  (println "📊 **Now Supporting 13 Major Currencies:**")
  (println "==========================================")
  (println "🇺🇸 USD - US Dollar (default)")
  (println "🇪🇺 EUR - Euro")
  (println "🇯🇵 JPY - Japanese Yen")
  (println "🇬🇧 GBP - British Pound")
  (println "🇨🇦 CAD - Canadian Dollar")
  (println "🇦🇺 AUD - Australian Dollar")
  (println "🇨🇭 CHF - Swiss Franc")
  (println "🇸🇬 SGD - Singapore Dollar")
  (println "🇭🇰 HKD - Hong Kong Dollar")
  (println "🇰🇷 KRW - Korean Won")
  (println "🇨🇳 CNY - Chinese Yuan")
  (println "🇮🇳 INR - Indian Rupee")
  (println "🇧🇷 BRL - Brazilian Real")
  (println))

(defn demo-additional-currencies []
  "Suggest additional major currencies to consider"
  (println "💡 **Additional Major Currencies to Consider:**")
  (println "==============================================")
  (println "🇲🇽 MXN - Mexican Peso (Latin America's 2nd largest economy)")
  (println "🇷🇺 RUB - Russian Ruble (major commodity currency)")
  (println "🇿🇦 ZAR - South African Rand (African financial hub)")
  (println "🇹🇷 TRY - Turkish Lira (emerging market)")
  (println "🇸🇦 SAR - Saudi Riyal (oil economy)")
  (println "🇦🇪 AED - UAE Dirham (Middle East financial hub)")
  (println "🇳🇴 NOK - Norwegian Krone (oil economy)")
  (println "🇸🇪 SEK - Swedish Krona (Nordic region)")
  (println "🇩🇰 DKK - Danish Krone (Nordic region)")
  (println "🇳🇿 NZD - New Zealand Dollar (Oceania)")
  (println "🇮🇱 ILS - Israeli Shekel (tech economy)")
  (println "🇹🇭 THB - Thai Baht (Southeast Asia)")
  (println "🇲🇾 MYR - Malaysian Ringgit (Southeast Asia)")
  (println "🇵🇭 PHP - Philippine Peso (Southeast Asia)")
  (println "🇮🇩 IDR - Indonesian Rupiah (largest Southeast Asian economy)")
  (println "🇻🇳 VND - Vietnamese Dong (growing economy)")
  (println "🇪🇬 EGP - Egyptian Pound (Middle East/Africa)")
  (println "🇳🇬 NGN - Nigerian Naira (largest African economy)")
  (println))

(defn demo_regional_priority []
  "Show currency priorities by region"
  (println "🗺️  **Regional Currency Priorities:**")
  (println "===================================")
  (println)
  (println "🏆 **Tier 1 (Already Added):**")
  (println "Major global reserve currencies + largest Asian economies")
  (println "USD, EUR, JPY, GBP, CNY, INR, CAD, AUD, CHF")
  (println)
  (println "🥈 **Tier 2 (High Priority):**")
  (println "Regional financial hubs + crypto-active economies")
  (println "SGD, HKD, KRW, BRL ✅ (already added)")
  (println "MXN, AED, NOK, SEK, NZD (suggest adding)")
  (println)
  (println "🥉 **Tier 3 (Medium Priority):**")
  (println "Emerging markets + regional powers")
  (println "RUB, ZAR, TRY, SAR, THB, MYR")
  (println)
  (println "📈 **Crypto-Active Regions:**")
  (println "Countries with high crypto adoption:")
  (println "🇻🇳 VND - Vietnam (high crypto adoption)")
  (println "🇹🇷 TRY - Turkey (crypto as inflation hedge)")
  (println "🇦🇷 ARS - Argentine Peso (crypto adoption due to inflation)")
  (println "🇳🇬 NGN - Nigeria (largest African crypto market)")
  (println))

(defn demo_api_availability []
  "Check which currencies are available in our target APIs"
  (println "🔌 **API Currency Availability:**")
  (println "================================")
  (println)
  (println "✅ **FreeCurrencyAPI.com (Free tier: 5,000 req/month)**")
  (println "Supports: 32 major currencies including all our current ones")
  (println "Available: USD, EUR, JPY, GBP, CAD, AUD, CHF, SGD, HKD, KRW, CNY, INR, BRL")
  (println "+ MXN, AED, NOK, SEK, DKK, NZD, ZAR, THB, MYR, PHP, IDR, VND")
  (println)
  (println "✅ **ExchangeRate.host (Free: 100 req/month, Paid: $14.99/month)**")
  (println "Supports: 168+ currencies (most comprehensive)")
  (println "Available: All major currencies + most emerging market currencies")
  (println)
  (println "✅ **Fixer.io (Professional grade)**")
  (println "Supports: 170+ currencies")
  (println "Available: All major + emerging market currencies")
  (println))

(defn demo_currency_examples []
  "Show examples with the new currencies"
  (println "💰 **Multi-Currency Price Display Examples:**")
  (println "============================================")
  (println)

  (let [btc_usd_price 116484.2
        exchange_rates {"EUR" 0.85
                        "JPY" 110.25
                        "GBP" 0.73
                        "SGD" 1.35
                        "HKD" 7.8
                        "KRW" 1320.0
                        "CNY" 7.2
                        "INR" 83.5
                        "BRL" 5.1}]

    (println "📊 **BTC Price in Different Currencies:**")
    (doseq [[currency rate] exchange_rates]
      (let [converted_price (* btc_usd_price rate)]
        (println (str currency ": " (.toFixed converted_price 2)))))
    (println)

    (println "🏦 **Regional Financial Hubs:**")
    (println (str "Singapore (SGD): " (.toFixed (* btc_usd_price 1.35) 2)))
    (println (str "Hong Kong (HKD): " (.toFixed (* btc_usd_price 7.8) 2)))
    (println (str "Seoul (KRW): " (.toFixed (* btc_usd_price 1320.0) 0)))
    (println)

    (println "🌏 **Largest Asian Economies:**")
    (println (str "China (CNY): " (.toFixed (* btc_usd_price 7.2) 2)))
    (println (str "India (INR): " (.toFixed (* btc_usd_price 83.5) 2)))
    (println (str "Japan (JPY): " (.toFixed (* btc_usd_price 110.25) 0)))
    (println)))

(defn suggest_next_additions []
  "Suggest the next currencies to add"
  (println "🚀 **Recommended Next Additions:**")
  (println "==================================")
  (println)
  (println "🎯 **Top 5 Priority (Financial Hubs + Major Economies):**")
  (println "1. 🇲🇽 MXN - Mexican Peso (2nd largest Latin American economy)")
  (println "2. 🇦🇪 AED - UAE Dirham (Middle East financial hub)")
  (println "3. 🇳🇴 NOK - Norwegian Krone (oil economy, strong currency)")
  (println "4. 🇸🇪 SEK - Swedish Krona (Nordic financial center)")
  (println "5. 🇳🇿 NZD - New Zealand Dollar (completes major Anglo currencies)")
  (println)
  (println "💡 **High Crypto Adoption (Next 3):**")
  (println "6. 🇹🇭 THB - Thai Baht (crypto-friendly regulations)")
  (println "7. 🇻🇳 VND - Vietnamese Dong (high retail crypto adoption)")
  (println "8. 🇲🇾 MYR - Malaysian Ringgit (growing crypto market)")
  (println)
  (println "📊 **This would give us 21 total currencies covering:**")
  (println "- All G7 currencies ✅")
  (println "- Top 10 global economies ✅")
  (println "- Major financial hubs ✅")
  (println "- High crypto adoption regions ✅")
  (println "- Emerging market leaders ✅"))

(defn main []
  "Run the expanded currency demo"
  (demo-current-currencies)
  (demo-additional-currencies)
  (demo_regional_priority)
  (demo_api_availability)
  (demo_currency_examples)
  (suggest_next_additions)

  (println)
  (println "✅ **Summary:**")
  (println "===============")
  (println "🎉 Added 7 new currencies: SGD, HKD, KRW, CNY, INR, BRL")
  (println "🌍 Now supporting 13 major global currencies")
  (println "🚀 Ready to add 8 more high-priority currencies")
  (println "📈 Covers all major crypto markets and financial hubs"))

;; Run the demo
(main)