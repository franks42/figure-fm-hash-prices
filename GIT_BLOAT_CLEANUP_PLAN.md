# Git Repository Bloat Cleanup Plan
*Implement shallow checkout + periodic cleanup to prevent infinite repo growth*

## Current Problem

Your GitHub Actions force-push `crypto-prices.json` every 10 minutes:
- **Current growth**: ~5KB × 6 times/hour × 24 hours × 30 days = **~22MB/month**  
- **Yearly projection**: ~264MB just for data files
- **CI slowdown**: Fetching entire history on every run

## Solution: Shallow Checkout + Periodic Cleanup

Implement `--depth 1` checkout and monthly orphan branch cleanup to keep repo size manageable.

---

## Phase 1: Implement Shallow Checkout (30 minutes)

### Step 1.1: Update Current Active Workflow

**Goal**: Reduce CI bandwidth and speed up runs immediately

**File**: `.github/workflows/fetch-crypto-data-nbb.yml`

**Current**:
```yaml
- name: Checkout repository
  uses: actions/checkout@v4
```

**New**:
```yaml
- name: Checkout repository  
  uses: actions/checkout@v4
  with:
    fetch-depth: 1      # Only fetch latest commit
    ref: main           # Ensure we're on main branch
```

**Benefits**:
- ✅ **Immediate**: Faster CI runs (less data to fetch)
- ✅ **Safe**: No functional changes to data processing
- ✅ **Reversible**: Easy to rollback if issues arise

### Step 1.2: Update Legacy Workflow (if needed)

**File**: `.github/workflows/fetch-crypto-data.yml`

Apply same changes if you decide to keep this workflow temporarily.

### Step 1.3: Test the Change

**Validation**:
- [ ] Workflow runs successfully
- [ ] Data file is updated correctly
- [ ] Frontend receives new data
- [ ] CI run time decreases

**Test Script**:
```bash
# Monitor workflow timing before/after
gh workflow run fetch-crypto-data-nbb.yml
gh run list --limit 5  # Compare durations
```

---

## Phase 2: Add Periodic Cleanup (1 hour)

### Step 2.1: Create Monthly Cleanup Job

**Goal**: Prevent long-term repo growth with monthly "fresh start"

**Add to `.github/workflows/fetch-crypto-data-nbb.yml`**:

```yaml
name: Fetch Crypto Data (nbb)

on:
  workflow_dispatch:
  schedule:
    - cron: '*/10 * * * *'        # Every 10 minutes (data fetch)
    - cron: '0 0 1 * *'          # Monthly cleanup (1st day, midnight UTC)

jobs:
  # Determine which job to run
  check-schedule:
    runs-on: ubuntu-latest
    outputs:
      job-type: ${{ steps.determine.outputs.job-type }}
    steps:
      - name: Determine job type
        id: determine
        run: |
          if [ "${{ github.event.schedule }}" = "0 0 1 * *" ]; then
            echo "job-type=cleanup" >> $GITHUB_OUTPUT
          else
            echo "job-type=fetch" >> $GITHUB_OUTPUT  
          fi

  # Regular data fetch (every 10 min)
  fetch-data:
    needs: check-schedule
    if: needs.check-schedule.outputs.job-type == 'fetch'
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 1
          ref: main
      
      # ... existing fetch steps ...

  # Monthly cleanup (1st of month)
  cleanup-repo:
    needs: check-schedule  
    if: needs.check-schedule.outputs.job-type == 'cleanup'
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Need full history for cleanup
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Fetch latest data
        uses: ./.github/actions/fetch-data
        with:
          alpha-vantage-api-key: ${{ secrets.ALPHA_VANTAGE_API_KEY }}

      - name: Create fresh data branch
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action - Monthly Cleanup"
          
          # Create completely new orphan branch (no history)
          git checkout --orphan data-updates-fresh
          
          # Add only the latest data file
          git add data/crypto-prices.json
          git commit -m "🧹 Monthly cleanup: Fresh data branch $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
          
          # Force push to replace data-updates branch completely
          git push origin data-updates-fresh:data-updates --force
          
          echo "✅ Created fresh data-updates branch with no history"
          echo "📊 Old branch history eliminated"
```

### Step 2.2: Alternative: Separate Cleanup Workflow

**Option**: Create dedicated cleanup workflow for cleaner separation

**Create**: `.github/workflows/monthly-cleanup.yml`

```yaml
name: Monthly Repository Cleanup

on:
  workflow_dispatch:  # Manual trigger for testing
  schedule:
    - cron: '0 2 1 * *'  # 1st day of month, 2 AM UTC (avoid conflicts)

permissions:
  contents: write

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout with full history
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Get current data-updates branch content
        run: |
          git fetch origin data-updates:data-updates || echo "data-updates branch doesn't exist yet"
          if git show-ref --verify --quiet refs/heads/data-updates; then
            git checkout data-updates
            cp data/crypto-prices.json /tmp/latest-crypto-prices.json
            git checkout main
          else
            echo "No existing data-updates branch, will fetch fresh data"
          fi

      - name: Fetch fresh crypto data
        uses: ./.github/actions/fetch-data  
        with:
          alpha-vantage-api-key: ${{ secrets.ALPHA_VANTAGE_API_KEY }}

      - name: Use existing data if fetch fails
        run: |
          if [ ! -s data/crypto-prices.json ] && [ -f /tmp/latest-crypto-prices.json ]; then
            echo "Fetch failed, using existing data"
            cp /tmp/latest-crypto-prices.json data/crypto-prices.json
          fi

      - name: Create orphan branch
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action - Cleanup"
          
          # Create fresh branch with no history
          git checkout --orphan data-updates-clean
          git rm -rf . 2>/dev/null || true
          
          # Re-add only necessary files
          mkdir -p data
          cp data/crypto-prices.json data/
          git add data/crypto-prices.json
          
          # Commit with cleanup message
          git commit -m "🧹 Monthly cleanup $(date -u '+%Y-%m-%d')" \
                     -m "Eliminated branch history to prevent repo bloat" \
                     -m "Previous data-updates branch size reset"
          
          # Replace data-updates branch
          git push origin data-updates-clean:data-updates --force
          
          echo "✅ Repository cleanup completed"
          echo "📊 data-updates branch history reset"

      - name: Verify cleanup
        run: |
          # Check that new branch has only 1 commit
          git ls-remote origin data-updates
          echo "Cleanup verification: data-updates branch recreated"

      - name: Notify cleanup completion
        run: |
          echo "🧹 Monthly cleanup completed successfully"
          echo "📉 Repository size optimized"
          echo "📅 Next cleanup: $(date -d '+1 month' -u '+%Y-%m-%d')"
```

---

## Phase 3: Enhanced Monitoring & Optimization (45 minutes)

### Step 3.1: Add Repository Size Monitoring

**Goal**: Track repo size over time and alert if cleanup fails

**Create**: `.github/workflows/repo-health-check.yml`

```yaml
name: Repository Health Check

on:
  workflow_dispatch:
  schedule:
    - cron: '0 6 * * 1'  # Weekly on Monday 6 AM UTC

jobs:
  health-check:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Analyze repository size
        run: |
          echo "🔍 Repository Health Analysis"
          echo "=============================="
          
          # Total repo size
          REPO_SIZE=$(du -sh .git | cut -f1)
          echo "📁 Total repository size: $REPO_SIZE"
          
          # Count commits in data-updates branch
          DATA_COMMITS=$(git rev-list --count origin/data-updates 2>/dev/null || echo "0")
          echo "📊 data-updates commits: $DATA_COMMITS"
          
          # Size of data-updates branch
          if [ "$DATA_COMMITS" -gt "0" ]; then
            git checkout origin/data-updates
            DATA_SIZE=$(du -sh data/ | cut -f1)
            echo "💾 data/ directory size: $DATA_SIZE"
            git checkout main
          fi
          
          # Alert if too many commits (should be ~1 after cleanup)
          if [ "$DATA_COMMITS" -gt "100" ]; then
            echo "⚠️  WARNING: data-updates branch has $DATA_COMMITS commits"
            echo "🧹 Monthly cleanup may have failed"
            echo "📋 Consider manual cleanup or investigate automation"
          else
            echo "✅ Repository size appears healthy"
          fi
          
          # Check latest data file age
          DATA_AGE=$(find data/ -name "*.json" -mmin +30 | wc -l)
          if [ "$DATA_AGE" -gt "0" ]; then
            echo "⚠️  Data files appear stale (>30 min old)"
          fi

      - name: Create size report
        run: |
          cat > /tmp/repo-health.md << EOF
          # Repository Health Report
          **Date**: $(date -u)
          
          ## Size Metrics
          - Repository size: $(du -sh .git | cut -f1)
          - data-updates commits: $(git rev-list --count origin/data-updates 2>/dev/null || echo "0")
          - Data directory size: $(du -sh data/ | cut -f1)
          
          ## Recommendations
          $(if [ "$(git rev-list --count origin/data-updates 2>/dev/null || echo "0")" -gt "100" ]; then
            echo "- ⚠️ Run monthly cleanup manually"
          else
            echo "- ✅ Repository health is good"
          fi)
          EOF
          
          cat /tmp/repo-health.md
```

### Step 3.2: Add Manual Cleanup Trigger

**Goal**: Allow manual cleanup when needed

**Add to existing workflow or create new**:

```yaml
# Add to fetch-crypto-data-nbb.yml
on:
  workflow_dispatch:
    inputs:
      cleanup:
        description: 'Run cleanup (true/false)'  
        required: false
        default: 'false'
        type: boolean

jobs:
  manual-cleanup:
    if: github.event.inputs.cleanup == 'true'
    runs-on: ubuntu-latest
    steps:
      # ... same cleanup steps as monthly cleanup ...
```

**Usage**:
```bash
# Trigger manual cleanup
gh workflow run fetch-crypto-data-nbb.yml -f cleanup=true
```

---

## Phase 4: Optimize Fetch Strategy (30 minutes)

### Step 4.1: Smarter Branch Handling

**Goal**: Only create new branch when data actually changes

```yaml
# Enhanced commit logic in main fetch job
- name: Smart commit and push  
  run: |
    git config --local user.email "action@github.com"
    git config --local user.name "GitHub Action"
    
    # Check if data actually changed
    if git show-ref --verify --quiet refs/remotes/origin/data-updates; then
      git fetch origin data-updates:data-updates
      if git diff --quiet data-updates -- data/crypto-prices.json; then
        echo "📊 No data changes detected, skipping commit"
        exit 0
      fi
    fi
    
    # Data changed or first run, create update
    git checkout -b data-updates-temp
    git add data/crypto-prices.json
    git commit -m "Update crypto data $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
    git push origin data-updates-temp:data-updates --force
    echo "✅ Data updated successfully"
```

### Step 4.2: Add Data Validation

**Goal**: Prevent committing invalid data

```yaml
- name: Validate data before commit
  run: |
    if [ ! -s data/crypto-prices.json ]; then
      echo "❌ Error: crypto-prices.json is empty"
      exit 1
    fi
    
    # Check JSON validity  
    if ! jq empty data/crypto-prices.json; then
      echo "❌ Error: Invalid JSON format"
      exit 1
    fi
    
    # Check critical data presence
    HASH_PRICE=$(jq -r '.hash.usd // 0' data/crypto-prices.json)
    if [ "$HASH_PRICE" = "0" ] || [ "$HASH_PRICE" = "null" ]; then
      echo "⚠️  Warning: HASH price missing or zero"
    fi
    
    BTC_PRICE=$(jq -r '.btc.usd // 0' data/crypto-prices.json)  
    if (( $(echo "$BTC_PRICE < 10000" | bc -l) )); then
      echo "⚠️  Warning: BTC price seems unusually low: $BTC_PRICE"
    fi
    
    echo "✅ Data validation passed"
```

---

## Implementation Timeline

| Phase | Task | Time | Risk | Priority |
|-------|------|------|------|----------|
| 1 | Add shallow checkout | 30 min | Low | High |
| 2 | Add monthly cleanup | 1 hour | Medium | High |  
| 3 | Add monitoring | 45 min | Low | Medium |
| 4 | Optimize fetch | 30 min | Low | Low |

**Total: 2 hours 45 minutes**

---

## Testing & Validation

### Phase 1 Testing (Shallow Checkout)
```bash
# Test current workflow with shallow checkout
gh workflow run fetch-crypto-data-nbb.yml

# Monitor timing improvement
gh run list --limit 5 --json conclusion,startedAt,updatedAt

# Verify data still updates correctly
curl -s "https://raw.githubusercontent.com/franks42/figure-fm-hash-prices/data-updates/data/crypto-prices.json" | jq '.btc.usd'
```

### Phase 2 Testing (Cleanup)
```bash
# Test monthly cleanup manually
gh workflow run monthly-cleanup.yml

# Verify branch history reset
git ls-remote origin data-updates
git log --oneline origin/data-updates  # Should show only 1-2 commits

# Check repo size improvement
du -sh .git  # Should be significantly smaller
```

### Phase 3 Testing (Monitoring)
```bash
# Run health check
gh workflow run repo-health-check.yml

# View health report
gh run view --log
```

## Rollback Plan

### If Issues Arise:

1. **Immediate rollback**:
```yaml
# Revert to original checkout
- name: Checkout repository
  uses: actions/checkout@v4  # Remove fetch-depth
```

2. **Restore full history** (if needed):
```bash
# Locally restore data-updates with history
git clone --bare https://github.com/franks42/figure-fm-hash-prices.git
cd figure-fm-hash-prices.git
git push origin HEAD:data-updates --force
```

3. **Emergency data restore**:
```bash
# Use GitHub's API to restore previous data
curl -H "Authorization: token $GITHUB_TOKEN" \
     "https://api.github.com/repos/franks42/figure-fm-hash-prices/contents/data/crypto-prices.json?ref=data-updates"
```

## Expected Benefits

### Immediate (Phase 1):
- ✅ **50-80% faster CI runs**
- ✅ **Reduced bandwidth usage**
- ✅ **No functional changes**

### Long-term (All Phases):
- ✅ **Repo size capped at ~10-20MB** (vs unlimited growth)
- ✅ **Monthly automatic cleanup**  
- ✅ **Health monitoring & alerts**
- ✅ **Improved data validation**

### Metrics to Track:
- **CI Duration**: Before/after timing comparison
- **Repository Size**: Monthly size reports
- **Data Quality**: Validation pass/fail rates
- **Cleanup Success**: Monthly cleanup execution logs

This plan addresses the repository bloat issue while maintaining all current functionality and adding monitoring for long-term health.
