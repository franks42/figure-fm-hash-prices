# Figure Market Tracker

## Overview
Browser-based crypto/stock price tracker. ClojureScript via Scittle (interpreted, not compiled) with Reagent + Re-frame. Deployed on GitHub Pages from main branch.

## Critical Rules
- **Cache busters**: ALWAYS bump `?v=X.X.X` in `index.html` when changing ANY `.cljs` file. Browser will serve stale code otherwise.
- **Script load order**: Matters in `index.html` — effects before events, subs before views.
- **Test before push**: Start `python3 -m http.server 8000`, verify with Chrome DevTools screenshots.
- **CORS**: Yahoo Finance v8 is CORS-blocked from browser. Use Twelve Data for FIGR in browser. Yahoo v8 only in GitHub Actions (`action.cljs`).

## Architecture
- `src/crypto_app_v3/` — all application code
- `effects.cljs` — re-frame effects (fetch, localStorage, market data orchestration)
- `events.cljs` — event handlers
- `subs.cljs` — subscriptions
- `card_v5.cljs` — card UI components (3-row layout below chart)
- `chart_v5.cljs` — uPlot chart with HTML overlays
- `direct_api.cljs` — API fetch functions
- `transform.cljs` — data transformers (`:figure-markets`, `:twelve`, `:yahoo`)
- `.github/actions/fetch-data/action.cljs` — GitHub Actions data fetcher (nbb, not Scittle)

## Data Sources
- Figure Markets API — crypto (HASH, FGRD, BTC, ETH, SOL, LINK, UNI, XRP)
- Twelve Data API — FIGR stock (key in direct_api.cljs)
- Yahoo Finance v8 — GitHub Actions server-side only
- Portfolio in localStorage key `"crypto-portfolio-v3-simple"`

## Debugging
- Access re-frame DB from browser console: `scittle.core.eval_string('(clj->js @re-frame.db/app-db)')`
- Lint: `clj-kondo --lint src/crypto_app_v3/filename.cljs`
- Use Chrome DevTools MCP (not Playwright MCP when Chrome is already running)
