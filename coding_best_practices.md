# Coding Best Practices - AI Assistant Guidelines

## Code Quality Requirements

### ⚠️ MANDATORY: Linting & Formatting After EVERY Change
```bash
# ALWAYS run these commands after ANY code modification:
clj-kondo --lint src/crypto_app_v3/
cljfmt fix src/crypto_app_v3/
```

**CRITICAL RULE**: NEVER ask the user to test code that has not passed `clj-kondo` without errors.

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

## Development Workflow

### Standard Process
1. **Plan the change** (use todo_write if complex)
2. **Make code modifications**
3. **Run clj-kondo** - fix ALL errors before proceeding
4. **Run cljfmt** - ensure consistent formatting
5. **Test functionality** (only after linting passes)
6. **Commit changes** if tests pass

### Error Handling Philosophy
- **Fail fast** - catch issues immediately with linting
- **Be transparent** - communicate problems clearly to user
- **Iterate rapidly** - small changes, frequent validation
- **Learn from failures** - each error teaches better practices

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

---

**Remember**: Quality code that works correctly is more valuable than quick code that fails. Take time to do it right.
