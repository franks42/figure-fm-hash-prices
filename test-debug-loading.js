const { chromium } = require('playwright');

async function debugLoading() {
  console.log('🧪 DEBUG: Why is app stuck loading...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  page.on('console', msg => {
    const text = msg.text();
    console.log(`📊 ${text}`);
  });
  
  page.on('response', response => {
    console.log(`🌐 ${response.status()} ${response.url()}`);
  });
  
  try {
    console.log('📱 Loading with direct=true...');
    await page.goto('http://localhost:8000/?direct=true', { 
      waitUntil: 'domcontentloaded',
      timeout: 20000 
    });
    
    console.log('⏳ Waiting and watching for 10 seconds...');
    await page.waitForTimeout(10000);
    
    // Check if still loading
    const loadingElements = await page.locator('text=Loading').count();
    console.log(`🔍 Loading elements found: ${loadingElements}`);
    
    // Check if cards appeared
    const cardElements = await page.locator('.bg-white\\/\\[0\\.03\\]').count();
    console.log(`🔍 Card elements found: ${cardElements}`);
    
  } catch (error) {
    console.error('❌ Debug test failed:', error.message);
  } finally {
    await browser.close();
  }
}

debugLoading().catch(console.error);
