const { chromium } = require('playwright');

async function testCurrentState() {
  console.log('🧪 Testing current V5 state...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Listen for console messages
  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    
    if (type === 'error') {
      console.log(`❌ Console Error: ${text}`);
    } else if (text.includes('📈 Generated URL:')) {
      console.log(`🔍 URL: ${text}`);
    } else if (text.includes('Failed to load resource')) {
      console.log(`❌ Resource Error: ${text}`);
    } else if (text.includes('🚀') || text.includes('✅') || text.includes('❌')) {
      console.log(`📊 App Log: ${text}`);
    }
  });
  
  // Listen for network errors
  page.on('response', response => {
    if (response.status() >= 400) {
      console.log(`❌ HTTP ${response.status()}: ${response.url()}`);
    }
  });
  
  try {
    console.log('📱 Loading V5 page...');
    await page.goto('http://localhost:8000/?ui=v5', { 
      waitUntil: 'networkidle',
      timeout: 30000 
    });
    
    console.log('⏳ Waiting for V5 cards to load...');
    await page.waitForSelector('.bg-white\\/\\[0\\.03\\]', { timeout: 10000 });
    
    console.log('✅ V5 cards found!');
    
    // Check if FIGR card exists
    const figrCard = await page.locator('text=FIGR').first();
    const figrExists = await figrCard.count() > 0;
    console.log(`📊 FIGR card exists: ${figrExists}`);
    
    // Wait a bit more for any async operations
    await page.waitForTimeout(5000);
    
    console.log('🎯 Test complete - check logs above for issues');
    
  } catch (error) {
    console.error('❌ Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

testCurrentState().catch(console.error);
