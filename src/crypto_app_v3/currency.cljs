(ns crypto-app-v3.currency)

;; Currency utilities - shared between V4 and V5 components
;; Extracted to avoid circular dependencies

(defn format-number
  "Format number with specified decimal places"
  [n decimals]
  (if (number? n)
    (.toLocaleString n "en-US"
                     #js{:minimumFractionDigits decimals
                         :maximumFractionDigits decimals})
    "0"))

(defn convert-currency
  "Convert USD amount to target currency using current exchange rates"
  [usd-amount target-currency exchange-rates]
  (if (= target-currency "USD")
    usd-amount
    (let [currency-key (keyword target-currency)
          rate (get exchange-rates currency-key)]
      (if rate
        (* usd-amount rate)
        usd-amount))))  ; Fallback to USD if rate not available

(defn get-currency-symbol
  "Get currency symbol for display"
  [currency-code]
  (case currency-code
    "USD" "$"
    "EUR" "€"
    "GBP" "£"
    "JPY" "¥"
    "CAD" "C$"
    "AUD" "A$"
    "CHF" "CHF"
    "CNY" "¥"
    "KRW" "₩"
    "SEK" "kr"
    currency-code))  ; Fallback to code itself

(def supported-currencies
  "List of supported currencies with symbols and names"
  [{:code "USD" :symbol "$" :name "US Dollar"}
   {:code "EUR" :symbol "€" :name "Euro"}
   {:code "GBP" :symbol "£" :name "British Pound"}
   {:code "JPY" :symbol "¥" :name "Japanese Yen"}
   {:code "CAD" :symbol "C$" :name "Canadian Dollar"}
   {:code "AUD" :symbol "A$" :name "Australian Dollar"}
   {:code "CHF" :symbol "CHF" :name "Swiss Franc"}
   {:code "CNY" :symbol "¥" :name "Chinese Yuan"}
   {:code "KRW" :symbol "₩" :name "Korean Won"}
   {:code "SEK" :symbol "kr" :name "Swedish Krona"}])
