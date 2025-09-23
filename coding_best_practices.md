# Coding Best Practices - AI Assistant Guidelines

## Code Quality Requirements

### ⚠️ MANDATORY: Linting & Formatting After EVERY Change
```bash
# ALWAYS run these commands after ANY code modification:
clj-kondo --lint src/crypto_app_v3/
cljfmt fix src/crypto_app_v3/
```

**CRITICAL RULE**: NEVER ask the user to test code that has not passed `clj-kondo` without errors.

### 🧪 MANDATORY: Playwright Testing Before Claims
```bash
# ALWAYS test functionality with Playwright before claiming it works:
node test-current-state.js        # Basic functionality
node test-buttons.js              # UI interactions  
node test-live-data.js            # API integrations
node test-debug-loading.js        # Loading states
```

**ABSOLUTE RULE**: 
- **NEVER** claim "it works" or "should work" without Playwright verification
- **NEVER** say "try this" without testing it yourself first
- **ALWAYS** create and run Playwright tests for any new functionality
- **ALWAYS** test edge cases (button clicks, modal states, error conditions)

**Why this matters:**
- Prevents wasted user time debugging obviously broken code
- Catches browser-specific issues that don't show in server-side testing
- Validates actual user interactions, not just data flow
- Playwright shows real JavaScript errors and timing issues

### Testing Requirements
```bash
# Testing workflow - AFTER linting passes:
# 1. Run tests (method depends on project setup)
# 2. All tests must pass before commit
# 3. Write tests for every new public function/re-frame event
```

**TESTING RULE**: Failing tests block commits. New functionality requires corresponding tests.

### Honesty & Integrity
- **BE HONEST** - If code doesn't work, say so
- **NO CHEATING** - Don't claim something works when it doesn't  
- **NO LYING** - Admit failures immediately and work through them
- **Transparency builds trust** - Users prefer honest debugging over false confidence

## Problem-Solving Strategies

### When Stuck with Parentheses Matching
Two proven approaches:

#### Option 1: Modular Decomposition
- Break complex top-level forms into separate functions/definitions
- **Modularity is your friend** - smaller functions are easier to manage
- Compose smaller pieces into larger functionality
- Each function should have clear, single responsibility

#### Option 2: Temporary File Editing
- Copy problematic top-level form to temporary file
- Edit in isolation without surrounding complexity  
- Once editing is complete, copy back in-place
- **This makes editing significantly easier** in many cases

## Oracle Consultation Guidelines

### When to Consult Oracle
Always ask the Oracle for help with:

#### 1. **Coding Issues**
- Complex debugging scenarios
- Performance optimization questions
- Code structure and organization

#### 2. **Architectural Choices** 
- System design decisions
- Component interaction patterns
- State management approaches

#### 3. **Library/Module/Application Selection**
- Choosing between competing libraries
- Evaluating tool compatibility
- Understanding framework trade-offs

### Oracle Usage Pattern
```
Problem identified → Consult Oracle → Implement solution → Test & verify
```

## re-frame Architecture Requirements

### Event Handlers - Keep Pure
- Use `:fx` or `:db` returns only - NO side effects in event handlers
- Side effects belong in **effect handlers** only
- Event handlers must be pure functions for predictability and testing

### State Management
- **Single source of truth**: All app state lives in `db`
- Create immutable snapshots only - never mutate `db` directly
- Use namespaced keywords: `::namespace/event-name`
- Keep all events in central `events.cljs`

### Subscriptions  
- Subscriptions are **derivations only** - never perform side effects
- Components must deref subscriptions **inside render fn only**
- Never deref subscriptions in event handlers

### Interceptors
- Use interceptors for validation/logging rather than manual code in each handler
- Keep cross-cutting concerns (auth, logging) in interceptors

## Scittle & GitHub Pages Specifics

### SCI/Scittle Limitations (CRITICAL KNOWLEDGE)
**Scittle uses SCI (Small Clojure Interpreter) - NOT full ClojureScript!**

**❌ DOES NOT WORK in Scittle:**
- `defprotocol` and `defrecord` - Limited/broken implementation
- `deftype` and `definterface` - Not supported  
- `this-as` in JavaScript hosts - Not supported
- Complex JavaScript interop - May be limited
- Advanced macro features - May be restricted
- `future` and `pmap` - Disabled by default for safety

**✅ WORKS in Scittle:**
- Basic data structures (vectors, maps, sets, lists)
- Functions, basic macros (`def`, `let`, `fn`)
- Core library functions (`map`, `filter`, `reduce`)
- Basic DOM manipulation and JavaScript interop
- re-frame (specifically supported)
- Simple multimethods (simpler than protocols)

**🔧 Scittle-Safe Patterns:**
- Use **multimethods** instead of protocols
- Use **regular functions** instead of records  
- Use **plain maps** instead of deftype
- Test ALL advanced features before assuming they work
- Stick to core ClojureScript features

### Bundle Size Considerations
- Scittle compiles ClojureScript in browser at runtime
- Keep bundle size small - avoid unnecessary dependencies
- Be careful with `goog.require` side effects

### Development Environment
```bash
# Local development server:
python3 -m http.server 8000
# OR
npx serve

# Playwright testing setup:
npm install playwright
npx playwright install chromium
```

### Playwright Testing Workflow
```bash
# 1. Make code changes
# 2. Run linting (clj-kondo + cljfmt)
# 3. Create/update Playwright test
# 4. Test functionality yourself BEFORE claiming it works
# 5. Only then present results to user

# Example test creation pattern:
node test-new-feature.js    # Verify your changes
node test-buttons.js        # Check UI interactions still work  
node test-current-state.js  # Ensure no regressions
```

**Testing-First Development:**
- Write Playwright test BEFORE implementing complex features
- Test incrementally after each small change
- Never batch multiple changes without intermediate testing
- Use headless: false during development to see what's happening

### Build Targets
- **Dev**: Unoptimized, source-maps enabled
- **Prod**: Advanced compilation, hashed bundles
- GitHub Pages auto-deploys from main branch

## Development Workflow

### Standard Process
1. **Plan the change** (use todo_write if complex)
2. **Make code modifications**
3. **Run clj-kondo** - fix ALL errors before proceeding
4. **Run cljfmt** - ensure consistent formatting
5. **Run tests** - all must pass before proceeding
6. **Test functionality** manually if needed
7. **Commit changes** only if all checks pass

### Error Handling Philosophy
- **Fail fast** - catch issues immediately with linting
- **Be transparent** - communicate problems clearly to user
- **Iterate rapidly** - small changes, frequent validation
- **Learn from failures** - each error teaches better practices

## Performance & Security

### Performance Patterns
- **Avoid N+1 subscription patterns** - group related data in single subscription
- **Component optimization** - use React.memo equivalent patterns where needed
- **Bundle analysis** - monitor bundle size impact of new dependencies

### Security Considerations
- **Never eval remote code** - especially important in browser-based compilation
- **Sanitize user input** before inserting into DOM
- **Content Security Policy** - follow OWASP CSP guidelines
- **Input validation** - use spec or Malli for event payload validation

## Repository Hygiene

### Branch Strategy
```bash
# Branch naming convention:
feature/add-chart-colors
fix/portfolio-calculation-bug
refactor/consolidate-portfolio-state
```

### Commit Messages
```bash
# Format: <type>: <subject>
feat: add chart color sentiment based on price direction
fix: correct FIGR data source indicator
refactor: consolidate portfolio value display components
```

### Documentation Requirements
- **Docstrings required** for every public function/var
- **Update docstrings** when changing function behavior
- **README updates** for new features or setup changes

### Pre-commit Checklist
```bash
# Recommended pre-commit hook commands:
clj-kondo --lint src/crypto_app_v3/
cljfmt fix src/crypto_app_v3/
# Run tests (project-specific command)
# Check for TODO/FIXME comments in new code
```

## AI Assistant Behavior Standards

### Communication
- Report linting results honestly
- Explain what failed and why
- Suggest concrete next steps
- Never hide or minimize issues

### Code Submission
- Only present code that passes all quality checks
- Include linting command outputs when relevant
- Acknowledge when something needs more work
- Ask for guidance when uncertain

## Quick Reference

### Common re-frame Patterns
```clojure
;; Event handler (pure)
(rf/reg-event-fx ::event-name
  (fn [{:keys [db]} [_ payload]]
    {:db (assoc db :key value)
     :fx [[:dispatch [::other-event]]]}))

;; Effect handler (side effects)
(rf/reg-fx :http-get
  (fn [config]
    (http/get config)))

;; Subscription (derivation)
(rf/reg-sub ::data
  (fn [db _]
    (get db :key)))

;; Component (deref in render only)
(defn my-component []
  (let [data @(rf/subscribe [::data])]
    [:div data]))
```

### clj-kondo Configuration Tips
```bash
# Common re-frame ignore patterns for clj-kondo:
^{:clj-kondo/ignore [:unused-private-var]}
^{:clj-kondo/ignore [:unresolved-symbol]}
```

### Debugging Tools
- Use `re-frame-10x` or `day8/re-frame-trace` for dev builds
- Enable in development, disable in production
- Console.log sparingly - prefer re-frame tracing

---

**Remember**: Quality code that works correctly is more valuable than quick code that fails. Take time to do it right.

**Oracle Escalation**: For scenarios >30 min unresolved, complex architectural decisions, or unfamiliar library integration - summarize attempted fixes when consulting Oracle.
