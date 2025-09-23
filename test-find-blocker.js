const { chromium } = require('playwright');

async function findBlocker() {
  console.log('🧪 Finding the blocking overlay element...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('📱 Loading app...');
    await page.goto('http://localhost:8000/?direct=true', { 
      waitUntil: 'networkidle',
      timeout: 20000 
    });
    
    await page.waitForTimeout(3000);
    
    // Oracle's one-liner to find blocking elements
    const blockers = await page.evaluate(() => {
      return [...document.querySelectorAll('*')].filter(e => {
        const s = getComputedStyle(e);
        const r = e.getBoundingClientRect();
        return (s.position === 'fixed' || s.position === 'absolute') &&
               parseFloat(s.opacity || 1) > 0 &&
               s.pointerEvents !== 'none' &&
               r.width * r.height > 0 &&
               r.top <= 0 && r.left <= 0 &&
               r.bottom >= window.innerHeight && r.right >= window.innerWidth;
      }).map(e => ({
        tag: e.tagName,
        classes: e.className,
        html: e.outerHTML.slice(0, 200),
        text: e.textContent?.slice(0, 100)
      }));
    });
    
    console.log(`🔍 Found ${blockers.length} blocking elements:`);
    blockers.forEach((blocker, i) => {
      console.log(`\n🚨 BLOCKER ${i+1}:`);
      console.log(`  Tag: ${blocker.tag}`);
      console.log(`  Classes: ${blocker.classes}`);
      console.log(`  HTML: ${blocker.html}`);
      console.log(`  Text: ${blocker.text}`);
    });
    
    // Also check for common modal classes
    const modalElements = await page.evaluate(() => {
      const selectors = [
        '.fixed.inset-0',
        '[data-backdrop]', 
        '.modal-backdrop',
        '.modal-overlay',
        '.backdrop',
        '.overlay'
      ];
      
      return selectors.flatMap(sel => 
        [...document.querySelectorAll(sel)].map(e => ({
          selector: sel,
          visible: e.offsetParent !== null,
          classes: e.className,
          text: e.textContent?.slice(0, 50)
        }))
      );
    });
    
    console.log(`\n🔍 Modal elements by common selectors:`);
    modalElements.forEach(modal => {
      console.log(`  ${modal.selector}: visible=${modal.visible}, classes="${modal.classes}", text="${modal.text}"`);
    });
    
    return blockers.length > 0;
    
  } catch (error) {
    console.error('❌ Debug failed:', error.message);
    return false;
  } finally {
    await browser.close();
  }
}

findBlocker().catch(console.error);
