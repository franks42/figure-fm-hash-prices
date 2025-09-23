const { chromium } = require('playwright');

async function testRootUrl() {
  console.log('🧪 Testing root URL (http://localhost:8000/)...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  let v5Enabled = false;
  
  page.on('console', msg => {
    const text = msg.text();
    
    if (text.includes('🎨 V5 is the only layout - always enabled')) {
      console.log(`✅ V5 Layout: ${text}`);
      v5Enabled = true;
    } else if (text.includes('❌') || text.includes('Error')) {
      console.log(`❌ Error: ${text}`);
    }
  });
  
  try {
    console.log('📱 Loading root URL...');
    await page.goto('http://localhost:8000/', { 
      waitUntil: 'networkidle',
      timeout: 15000 
    });
    
    console.log('⏳ Waiting for V5 cards...');
    await page.waitForSelector('.bg-white\\/\\[0\\.03\\]', { timeout: 10000 });
    
    console.log('✅ V5 cards found on root URL!');
    return true;
    
  } catch (error) {
    console.error('❌ Root URL test failed:', error.message);
    return false;
  } finally {
    await browser.close();
  }
}

testRootUrl().then(success => {
  if (success) {
    console.log('🎉 ROOT URL SUCCESS - V5 cards load without ?ui=v5');
  } else {
    console.log('🚨 ROOT URL FAILED - Still need ?ui=v5 parameter');
  }
  process.exit(success ? 0 : 1);
}).catch(console.error);
