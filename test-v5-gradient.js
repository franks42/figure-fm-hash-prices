const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('🧪 Testing V5 saturation gradient implementation...');
    
    await page.goto('http://localhost:8000/simple.html');
    await page.waitForTimeout(3000);
    
    // Check if the simple demo loads
    const title = await page.title();
    console.log('✅ Simple demo loaded:', title);
    
    console.log('\n🎨 Testing gradient test page...');
    await page.goto('http://localhost:8000/test-saturation-gradient.html');
    await page.waitForTimeout(2000);
    
    // Verify gradient test page elements
    const testResults = await page.locator('#test-results > div').count();
    const gradientSpectrum = await page.locator('#gradient-spectrum > div').count();
    
    console.log('✅ Test cases rendered:', testResults);
    console.log('✅ Gradient spectrum elements:', gradientSpectrum);
    
    // Test a few specific percentage values
    const testValues = [
      { pct: -10, expectedType: 'strong negative' },
      { pct: -0.01, expectedType: 'neutral' },
      { pct: 0.01, expectedType: 'neutral' },
      { pct: 5, expectedType: 'strong positive' }
    ];
    
    console.log('\n🔍 Gradient intensity analysis:');
    for (const test of testValues) {
      const element = await page.locator(`text="${test.pct > 0 ? '+' : ''}${test.pct.toFixed(2)}%"`).first();
      
      if (await element.count() > 0) {
        const styles = await element.evaluate(el => {
          const computed = getComputedStyle(el);
          return {
            color: computed.color,
            backgroundColor: getComputedStyle(el.closest('div')).backgroundColor
          };
        });
        
        console.log(`  ${test.pct}%: Color=${styles.color}, Background=${styles.backgroundColor}`);
      }
    }
    
    console.log('\n🎯 Recommendations:');
    console.log('1. Very small changes (±0.01%) should be barely visible (neutral gray)');
    console.log('2. Medium changes (±2%) should show clear but not intense colors');  
    console.log('3. Large changes (±10%) should be highly saturated/intense');
    console.log('4. Gradient should be seamless with no sudden jumps');
    
    console.log('\n⏸️ Browser staying open for visual inspection...');
    console.log('Compare different percentage intensities manually.');
    
  } catch (error) {
    console.log('❌ Error:', error.message);
  } finally {
    // Don't auto-close for manual inspection
    // await browser.close();
  }
})();
