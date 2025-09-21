# V3 Completion Plan - Oracle-Guided Fixes

## Executive Summary
The V2→V3 migration was reported as complete but analysis revealed missing features, architectural inconsistencies, and bugs. This plan addresses the identified gaps to achieve true feature parity and proper re-frame architecture.

## Issues Identified by Oracle Review

### 🚨 High Priority - Missing Core Features
1. **24h Trades Count Missing** ✅ COMPLETED
   - Present in V2 cards, omitted in V3
   - User loses liquidity indication
   - **Action**: Add trades-24h row back to crypto cards

2. **Memory Usage Investigation** 🆘 URGENT
   - Safari reports "significant memory" usage and reloads page
   - **Action**: Profile memory usage, identify leaks/bloat sources, implement fixes
   - Potential causes: setInterval leaks, DOM accumulation, re-frame subscription leaks, large data retention

3. **Portfolio Storage Inconsistency**
   - Data stored in BOTH re-frame app-db AND reagent atoms
   - Re-frame code exists but unused (dead code)
   - Atoms currently handle all UI interactions
   - **Root Cause**: Previous instance fell back to atoms due to "localStorage issues"
   - **Oracle Finding**: localStorage issues were self-inflicted bugs, not technical limitations
   - **Action**: Fix re-frame localStorage bugs and complete migration

### 🔧 Medium Priority - UI/UX Regressions  
3. **Compact Header Layout**
   - Current: Multi-line header takes too much vertical space
   - **Action**: 
     - Version - crypto tracker - timestamp on one line (smaller font for tracker)
     - Remove "Real-time cryptocurrency prices - Re-frame Architecture" subtitle
     - TOTAL PORTFOLIO VALUE and value on one line with smaller font

4. **Portfolio Button/Value UI Behavior**
   - V2: Smart component - button when empty, clickable value when holdings exist
   - V3: Always shows both button AND value (redundant UI)
   - **Action**: Implement V2's conditional UI logic

5. **Flash Dispatch Bug**
   - First update flash never fires due to dispatch overwrite in `:smart-price-update`
   - **Action**: Fix cond-> logic to avoid :dispatch key collision

### ✅ Acceptable Changes
- Bid/Ask section removal (user confirmed not valuable due to non-live feeds)
- Last-update badge moved from bottom to top-right (visual preference)

## Technical Root Causes Analysis

### Portfolio localStorage "Issues" (Oracle Findings)
The previous instance claimed re-frame localStorage was problematic. Oracle analysis revealed:

1. **Duplicate Event Registration**
   - `events.cljs` and `portfolio.cljs` both register same event IDs
   - Last-loaded file silently overwrites first → random behavior
   
2. **JS/CLJS Data Type Mixing**
   - JSON parsed objects stored directly in app-db (JS objects)
   - Subsequent operations create CLJS maps
   - Inconsistent data shapes between save/restore

3. **Competing Persistence Strategies**
   - Two different fx handlers for same functionality
   - Different localStorage keys used

**Conclusion**: Fallback to atoms was unnecessary - issues are fixable.

## Step-by-Step Implementation Plan

### Phase 1: Snapshot & Setup
- [ ] Document current state in this plan
- [ ] Commit/push/tag as `v3-pre-completion-fixes`
- [ ] Oracle pre-analysis for each fix

### Phase 2: Memory Usage Investigation
- [ ] Oracle: Analyze V3 codebase for memory leak sources
- [ ] Profile browser memory usage during normal operation
- [ ] Check for setInterval cleanup issues
- [ ] Investigate re-frame subscription lifecycle
- [ ] Measure DOM node accumulation
- [ ] Test with different browsers to isolate Safari-specific issues

### Phase 3: Portfolio Storage Consolidation
- [ ] Oracle: Analyze current portfolio code paths
- [ ] Fix duplicate event registrations (remove portfolio.cljs or rename IDs)
- [ ] Fix JS/CLJS data mixing (consistent keywordized maps)
- [ ] Migrate UI components from atoms to re-frame subscriptions/dispatch
- [ ] Remove portfolio-atoms.cljs and related dead code
- [ ] Test portfolio persistence thoroughly

### Phase 4: Missing Features
- [ ] Oracle: Review V2 trades-24h implementation  
- [ ] Add trades-24h field to crypto cards in V3
- [ ] Oracle: Analyze V2 portfolio UI behavior
- [ ] Implement smart portfolio button/value component

### Phase 5: Bug Fixes
- [ ] Oracle: Review flash dispatch logic
- [ ] Fix cond-> dispatch overwrite in `:smart-price-update`
- [ ] Clear setInterval IDs to prevent memory leaks (optional)

### Phase 6: Validation
- [ ] Oracle: Final architecture review
- [ ] Test all features against V2 parity checklist
- [ ] Performance comparison
- [ ] Commit final state with comprehensive tag

## Success Criteria
- [ ] All V2 features present in V3 (minus bid/ask)
- [ ] Single source of truth for all data (re-frame app-db only)
- [ ] No dead code or unused atoms
- [ ] localStorage persistence works reliably
- [ ] UI behavior matches V2 expectations
- [ ] No architectural inconsistencies

## Risk Mitigation
- Oracle consultation before each major change
- Incremental commits after each fix
- Rollback plan using snapshot tag
- Test after each phase completion

---
*Plan created: 2025-09-20*  
*Oracle analysis confirms technical feasibility of all fixes*
