# Crypto Price Tracker - Card Layout Design Brief

## Project Overview
We need a new card layout design for a real-time cryptocurrency price tracker built with ClojureScript + Reagent + Tailwind CSS. The app uses selective updates (individual cards update without full page refresh) with glass morphism styling.

## Technical Constraints & Requirements

### Framework & Styling
- **CSS Framework**: Tailwind CSS (utility-first) - all styling MUST use Tailwind classes
- **Current Card Size**: Responsive flexbox layout, approximately 350-400px width
- **Hover Effects**: Must maintain smooth scale and lift transforms
- **Browser Support**: Modern browsers, mobile-first responsive design

### Architecture Notes
- Cards use **selective rendering** - individual data points update independently
- **Glass morphism** design with dark background and subtle blur effects
- **Real-time updates** with visual feedback (flash effects on data changes)
- Cards must maintain consistent height across different cryptocurrencies

## Data Points to Display (Required)

Each card must accommodate these exact data fields:

### Primary Data
1. **Crypto Name** - Full name (e.g., "BITCOIN", "ETHEREUM")
2. **Symbol** - Trading symbol (e.g., "BTC", "ETH", "HASH")  
3. **Icon** - Single character emoji/symbol: ₿, Ξ, ◎, 🔗, 🦄, 💰, 📈, 🏠
4. **Current Price** - Main price display (e.g., $115,352.73, $0.029, $37.33)

### Secondary Data
5. **24h Change %** - Percentage change with +/- and color coding
6. **24h Volume** - Trading volume (formatted: $427K, $1.2M, $11.8M)
7. **24h Trades** - Number of trades (integer: 37, 71, 0)

### Optional Data (Conditional Display)
8. **Bid Price** - Buy price (not all assets have this)
9. **Ask Price** - Sell price (not all assets have this)
10. **Day High/Low** - For stock data only

## Required Color Palette

### Neon Colors (Must Use Exact Values)
```css
--neon-green: #00ff88    /* Positive changes, gains */
--neon-red: #ff4d5a      /* Negative changes, losses */
--neon-cyan: #00ccff     /* Accent highlights */
--neon-pink: #ff00ff     /* Accent highlights */
```

### Background & Text
```css
--bg-card: rgba(255, 255, 255, 0.03)     /* Glass morphism card background */
--bg-hover: rgba(255, 255, 255, 0.06)    /* Card hover state */
--border: rgba(255, 255, 255, 0.1)       /* Card borders */
--border-hover: rgba(255, 255, 255, 0.2) /* Hover borders */
--text-primary: #ffffff                   /* Main text */
--text-secondary: #6b7280                 /* Gray-500 - labels, secondary info */
```

## Current Visual Style Reference

### Glass Morphism Cards
- **Background**: `bg-white/[0.03]` - Very subtle white overlay
- **Border**: `border-white/10` - Subtle white border
- **Backdrop blur**: `backdrop-blur-lg` - Frosted glass effect
- **Corner radius**: `rounded-3xl` - 24px radius for premium feel

### Animation & Interactions
- **Hover transform**: Scale 102% + lift 4px (`hover:scale-[1.02] hover:-translate-y-1`)
- **Transition**: `transition-all duration-300 ease-out`
- **Shadow on hover**: `hover:shadow-2xl hover:shadow-purple-500/10`

### Typography
- **Price display**: Large, bold, tabular numbers
- **Labels**: Small, uppercase, letter-spaced, gray
- **Change indicators**: Medium weight with arrow symbols (▲ ▼)

## Layout Requirements

### Responsive Design
- **Mobile**: Single column, full width (max 375px)
- **Tablet**: 2 columns 
- **Desktop**: 3+ columns in CSS Grid
- **Container**: Cards must maintain consistent height

### Visual Hierarchy (Priority Order)
1. **Current Price** - Most prominent element
2. **Change %** - Secondary prominence with color coding
3. **Crypto Name & Symbol** - Clear identification
4. **Trading metrics** - Volume, trades (less prominent)
5. **Bid/Ask** - Smallest, only when available

### Spacing & Layout
- **Card padding**: `p-6` (24px all sides)
- **Internal spacing**: Consistent vertical rhythm
- **Grid gaps**: Adequate breathing room between sections
- **Responsive margins**: Appropriate for mobile/desktop

## Required Figma Deliverables

### 1. Card Variants (Desktop - 350-400px width)
- **High-value crypto**: BTC at $115,352.73 (+0.32%)
- **Mid-value crypto**: ETH at $4,535.83 (-1.11%)
- **Low-value crypto**: HASH at $0.029 (-23.68%)
- **Stock**: FIGR at $37.33 (+14.8%)

### 2. Mobile Cards (Full width, <375px)
- Same variants as desktop
- Optimized spacing for touch interfaces
- Maintained visual hierarchy

### 3. State Variations
- **Default state**: Standard card appearance
- **Hover state**: Scale + lift + glow effects
- **Update flash**: Brief highlight when data changes
- **Loading state**: Skeleton or subtle pulse animation

### 4. Data Scenarios
- **With bid/ask prices**: Full data display
- **Without bid/ask prices**: Cleaner layout
- **Zero/null values**: Graceful handling of missing data
- **Extreme values**: Very high ($100K+) and very low ($0.001) prices

### 5. Component Annotations
Each design element must include:
- **Exact Tailwind CSS classes**
- **Spacing measurements** (padding, margins, gaps)
- **Typography specs** (font-size, font-weight, line-height)
- **Color values** (referencing the neon palette)
- **Responsive breakpoint differences**

## Animation Considerations

### Selective Updates
- Individual data points update without full card re-render
- **Flash effects** on price/change updates (brief neon glow)
- **Smooth transitions** for all animated properties
- **Performance**: Minimal reflow/repaint

### Micro-interactions
- **Hover states**: Subtle but premium feeling
- **Loading indicators**: Non-intrusive, maintains layout
- **Update feedback**: Clear but not distracting

## Technical Integration Notes

### Tailwind Implementation
The design will be implemented directly as ClojureScript/Reagent components using Tailwind utility classes. Example structure:

```clojure
[:div {:class "relative bg-white/[0.03] border border-white/10 rounded-3xl p-6 
               backdrop-blur-lg transition-all duration-300 ease-out
               hover:scale-[1.02] hover:-translate-y-1 hover:bg-white/[0.06]"}
  ;; Card content with exact classes from your design
]
```

### Color Usage
- **Positive changes**: Always use neon-green (#00ff88)
- **Negative changes**: Always use neon-red (#ff4d5a)
- **Interactive elements**: neon-cyan or neon-pink accents
- **Backgrounds**: Glass morphism with white opacity overlays

## Asset Requirements

### Icons/Symbols
Current cryptocurrency icons (single characters):
- Bitcoin: ₿
- Ethereum: Ξ  
- Solana: ◎
- HASH: 🔗
- UNI: 🦄
- XRP: 💰
- FIGR: 📈
- FIGR_HELOC: 🏠

### Typography
- **Font**: System fonts (default Tailwind stack)
- **Number display**: Must use `tabular-nums` class for alignment
- **Labels**: Uppercase with letter-spacing for premium feel

## Success Criteria

1. **Visual Impact**: Premium, modern feel that stands out
2. **Readability**: Clear data hierarchy, easy to scan
3. **Responsiveness**: Perfect on mobile and desktop
4. **Implementation Ready**: Copy-paste Tailwind classes
5. **Performance**: Optimized for selective updates
6. **Consistency**: Works across all data scenarios

## Inspiration & Style Direction

Think: **Futuristic financial dashboard** meets **glass morphism UI** with **neon cyberpunk accents**. The design should feel like a premium trading interface that crypto enthusiasts would want to keep open 24/7.

---

**Deliverable Format**: Figma file with annotated components showing exact Tailwind CSS classes for seamless developer handoff.
