const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Listen for console logs
  page.on('console', msg => {
    if (msg.text().includes('📊 PF:') || msg.text().includes('CHART-GRADIENT')) {
      console.log('BROWSER:', msg.text());
    }
  });
  
  try {
    console.log('🧪 Debugging PF period changes...');
    
    await page.goto('http://localhost:8000/');
    await page.waitForTimeout(8000);
    
    // Add portfolio first
    console.log('\n✅ Adding portfolio holdings...');
    const addButton = await page.locator('button:has-text("Add to Portfolio")').first();
    if (await addButton.count() > 0) {
      await addButton.click();
      await page.waitForSelector('#quantity-input', { timeout: 5000 });
      await page.fill('#quantity-input', '100');
      await page.click('text=Save');
      await page.waitForTimeout(3000); // Wait for PF card to appear
    }
    
    // Check initial PF state
    const pfExists = await page.locator('text=PF').count();
    console.log('PF card exists:', pfExists > 0 ? 'YES' : 'NO');
    
    if (pfExists === 0) {
      console.log('❌ PF card not showing - stopping test');
      return;
    }
    
    // Test period changes
    const periods = ['1W', '1M', '24H'];
    
    for (const period of periods) {
      console.log(`\n✅ Testing period: ${period}`);
      
      // Click period button to change
      const periodButton = await page.locator('button').filter({ hasText: new RegExp('24H|1W|1M') }).first();
      if (await periodButton.count() > 0) {
        const currentPeriod = await periodButton.textContent();
        console.log(`Current period: ${currentPeriod}, clicking to change...`);
        
        await periodButton.click();
        await page.waitForTimeout(2000);
        
        const newPeriod = await periodButton.textContent();
        console.log(`New period: ${newPeriod}`);
        
        // Wait for data to load
        await page.waitForTimeout(3000);
        
        // Check PF card state after period change
        const pfAfterChange = await page.evaluate(() => {
          // Look for PF card
          const pfElements = Array.from(document.querySelectorAll('*')).filter(el => 
            el.textContent && el.textContent.includes('Total Portfolio')
          );
          
          if (pfElements.length === 0) return { found: false };
          
          const pfCard = pfElements[0].closest('[class*="bg-white/"]');
          if (!pfCard) return { found: false };
          
          // Check if chart area exists and has content
          const chartArea = pfCard.querySelector('[style*="aspect-ratio"]');
          
          return {
            found: true,
            hasChart: !!chartArea,
            chartHasContent: chartArea ? chartArea.innerHTML.length > 50 : false,
            cardText: pfCard.textContent.substring(0, 200)
          };
        });
        
        console.log(`PF card after ${period}:`, pfAfterChange);
        
        if (!pfAfterChange.found) {
          console.log(`❌ PF card DISAPPEARED after changing to ${period}!`);
          break;
        } else if (!pfAfterChange.hasChart || !pfAfterChange.chartHasContent) {
          console.log(`❌ PF chart missing for ${period}!`);
        } else {
          console.log(`✅ PF card working for ${period}`);
        }
      }
    }
    
    console.log('\n⏸️ Keeping browser open for manual inspection...');
    
  } catch (error) {
    console.log('❌ Error:', error.message);
  } finally {
    // Keep open for inspection
    // await browser.close();
  }
})();
