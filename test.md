# Testing Reminders - ALWAYS TEST BEFORE CLAIMING SOMETHING WORKS!

## 🚨 CRITICAL RULE: NEVER CLAIM SOMETHING WORKS WITHOUT TESTING IT YOURSELF

The user is tired of having to test for me when I can test myself. I must ALWAYS verify my work before claiming success.

## What I Can Test and How

### 1. Web Servers and HTTP Endpoints
```bash
# Start local server
python3 -m http.server 8000 &

# Test if server responds
curl -I http://localhost:8000/
curl -I http://localhost:8000/index-v2.html

# Test data files are accessible
curl -I http://localhost:8000/data/standardized-prices.json
curl -I http://localhost:8000/data/exchange-rates.json

# Test actual content
curl -s http://localhost:8000/data/exchange-rates.json | jq .
```

### 2. ClojureScript/nbb Scripts
```bash
# Test scripts locally before claiming they work
nbb .github/workflows/scripts/fetch-standardized-crypto-data.cljs
nbb .github/workflows/scripts/fetch-exchange-rates.cljs

# Check output files were created
ls -la data/
cat data/exchange-rates.json | jq .
```

### 3. GitHub Actions Workflows
```bash
# Check if workflow completed successfully
gh run list --workflow=fetch-standardized-data.yml --limit 1

# View failed logs if needed
gh run view [ID] --log-failed

# Test the actual API endpoints that workflows use
curl -s "https://api.exchangerate-api.com/v4/latest/USD" | jq .
```

### 4. Data Validation
```bash
# Verify data files have expected structure
cat data/exchange-rates.json | jq '.rates | keys'
cat data/standardized-prices.json | jq '.[0] | keys'

# Check file sizes (empty files are bad)
ls -la data/*.json

# Verify data from different branches
curl -s https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/exchange-rates.json | jq .
```

### 5. Browser Console Testing
When testing web apps, I can:
- Check browser console for errors using WebFetch
- Test specific API endpoints the app uses
- Verify JSON structure matches what the app expects

### 6. Currency Conversion Testing
```bash
# Test that exchange rates have proper values
cat data/exchange-rates.json | jq '.rates.EUR'  # Should be ~0.85
cat data/exchange-rates.json | jq '.rates.GBP'  # Should be ~0.73

# Verify rates are not null
cat data/exchange-rates.json | jq '.rates | select(. != null)'
```

### 7. Git and Repository State
```bash
# Check what branch data is in
git ls-remote origin | grep data-updates

# Verify files exist in correct branches
curl -I https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/exchange-rates.json
```

## Testing Workflow - FOLLOW THIS ALWAYS

1. **Before Making Changes**: Test current state to understand the problem
2. **After Making Changes**: Test that the fix actually works
3. **Before Claiming Success**: Run a full end-to-end test
4. **Document What You Tested**: Show the test commands and results

## Common Test Patterns

### Testing a Web App Fix:
```bash
# 1. Start server
python3 -m http.server 8000 &

# 2. Test server responds
curl -I http://localhost:8000/index-v2.html

# 3. Test data files
curl -s http://localhost:8000/data/exchange-rates.json | jq '.rates'

# 4. Test specific functionality (e.g., currency conversion)
# Only THEN can I claim it works
```

### Testing a GitHub Action Fix:
```bash
# 1. Test scripts locally first
nbb .github/workflows/scripts/fetch-exchange-rates.cljs

# 2. Check output
cat data/exchange-rates.json

# 3. Trigger workflow
gh workflow run fetch-standardized-data.yml

# 4. Verify success
gh run list --limit 1

# 5. Check data was pushed to correct branch
curl -s https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/exchange-rates.json | jq .
```

## 🚨 STOP AND TEST CHECKLIST

Before saying "it works" or "should work now", ask myself:

- [ ] Did I test the server starts?
- [ ] Did I test the endpoints respond?
- [ ] Did I test the data files have correct content?
- [ ] Did I test the actual functionality I was supposed to fix?
- [ ] Did I verify the problem is actually solved?

## Remember:
- "Should work" is not acceptable - TEST IT
- "Try this" is not acceptable - TEST IT FIRST
- Never assume something works because the code looks right
- The user shouldn't have to be my QA tester

## Examples of Proper Testing

### ✅ GOOD:
"I've tested the server locally and confirmed the exchange rate indicator now shows. Here's my test:
```bash
curl -s http://localhost:8000/data/exchange-rates.json | jq '.rates.EUR'
# Returns: 0.845
```
The currency conversion is working - BTC shows $117,741 in USD and €99,375 in EUR."

### ❌ BAD:
"The exchange rate indicator should now show properly."
"Try refreshing the page to see the currency conversion."
"The workflow should work now."

## Tools Available for Testing
- `curl` - Test HTTP endpoints
- `jq` - Parse and validate JSON
- `nbb` - Test ClojureScript scripts
- `gh` - Test GitHub workflows
- `python3 -m http.server` - Local web server
- Browser dev tools via WebFetch for frontend testing