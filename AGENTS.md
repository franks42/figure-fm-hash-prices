# AGENTS.md - Developer Guide

## Build/Lint/Test Commands
- **Format code**: `cljfmt fix src/crypto_app_v3/`
- **Lint code**: `clj-kondo --lint src/crypto_app_v3/`
- **Local development**: `python3 -m http.server 8000`
- **Deploy**: Push to GitHub (GitHub Pages auto-deploys, Actions fetch data every 10 minutes)

## Workflows
- **Primary**: `fetch-crypto-data-nbb.yml` - Active data fetching with multi-source fallback

## Architecture & Structure
- **Frontend**: ClojureScript + Scittle (browser-based, no build step) + Reagent + Tailwind CSS
- **Data Pipeline**: GitHub Actions → Figure Markets API + Yahoo Finance → JSON files  
- **Hosting**: GitHub Pages (serverless, static site)
- **Core Files**: 
  - Current: `index.html`, `src/crypto_app_v3/` (re-frame architecture)
- **State Management**:
  - Single re-frame app-db with event-driven architecture
  - Hybrid portfolio state using plain atoms for persistence

## Code Style & Conventions
- **Language**: ClojureScript with Reagent components
- **Naming**: kebab-case for functions, snake_case for crypto IDs, SCREAMING_SNAKE_CASE for constants
- **State management**: Fine-grained atoms, never merge back to single app-state
- **Selective updates**: Compare primitive values with `not=`, never compare full `js->clj` objects
- **Visual feedback**: Always show loading indicators and update flashes to users
- **Formatting**: Always run `cljfmt` and `clj-kondo` after changes
- **Cache busting**: Use timestamps in API calls to prevent stale data

## Critical Implementation Notes
- **Selective rendering is core**: Only update components when data actually changes
- **Primitive comparison**: Use `(not= old-price new-price)` not `(not= old-data new-data)`
- **Visual indicators**: Scan line during fetch, green flash on updates, timestamps
- **No screen flashing**: Loading states only on initial load, not background fetches
