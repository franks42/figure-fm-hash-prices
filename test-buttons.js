const { chromium } = require('playwright');

async function testButtons() {
  console.log('🧪 Testing portfolio and currency button functionality...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  let modalTests = {
    currencyClick: false,
    currencyModal: false,
    portfolioClick: false,
    portfolioModal: false
  };
  
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('currency/show-selector')) {
      console.log(`💱 Currency selector triggered: ${text}`);
      modalTests.currencyClick = true;
    } else if (text.includes('portfolio/show-panel')) {
      console.log(`📊 Portfolio panel triggered: ${text}`);
      modalTests.portfolioClick = true;
    }
  });
  
  try {
    console.log('📱 Loading with direct API...');
    await page.goto('http://localhost:8000/?direct=true', { 
      waitUntil: 'networkidle',
      timeout: 20000 
    });
    
    console.log('⏳ Waiting for cards to load...');
    await page.waitForSelector('.bg-white\\/\\[0\\.03\\]', { timeout: 10000 });
    
    // Test currency button click
    console.log('💱 Testing currency button...');
    try {
      await page.click('text=USD', { timeout: 5000 });
      modalTests.currencyClick = true;
      console.log('✅ Currency button clickable');
      
      // Check if currency modal appeared
      const currencyModal = await page.locator('text=Select Currency').count();
      modalTests.currencyModal = currencyModal > 0;
      console.log(`🔍 Currency modal visible: ${modalTests.currencyModal}`);
      
      if (modalTests.currencyModal) {
        await page.click('text=EUR', { timeout: 2000 });
        console.log('✅ Currency selection works');
      }
    } catch (e) {
      console.log(`❌ Currency button test failed: ${e.message}`);
    }
    
    await page.waitForTimeout(1000);
    
    // Test portfolio button click  
    console.log('📊 Testing portfolio button...');
    try {
      await page.click('text=Add to Portfolio', { timeout: 5000 });
      modalTests.portfolioClick = true;
      console.log('✅ Portfolio button clickable');
      
      // Check if portfolio modal appeared
      const portfolioModal = await page.locator('text=Edit').count();
      modalTests.portfolioModal = portfolioModal > 0;
      console.log(`🔍 Portfolio modal visible: ${modalTests.portfolioModal}`);
      
    } catch (e) {
      console.log(`❌ Portfolio button test failed: ${e.message}`);
    }
    
    console.log('🎯 Button Test Results:');
    console.log(`💱 Currency click: ${modalTests.currencyClick ? 'SUCCESS' : 'FAILED'}`);
    console.log(`💱 Currency modal: ${modalTests.currencyModal ? 'SUCCESS' : 'FAILED'}`);
    console.log(`📊 Portfolio click: ${modalTests.portfolioClick ? 'SUCCESS' : 'FAILED'}`);
    console.log(`📊 Portfolio modal: ${modalTests.portfolioModal ? 'SUCCESS' : 'FAILED'}`);
    
    const allWorking = modalTests.currencyClick && modalTests.currencyModal && 
                      modalTests.portfolioClick && modalTests.portfolioModal;
    return allWorking;
    
  } catch (error) {
    console.error('❌ Button test failed:', error.message);
    return false;
  } finally {
    await browser.close();
  }
}

testButtons().then(success => {
  if (success) {
    console.log('🎉 ALL BUTTONS WORKING - Portfolio and currency functionality restored!');
  } else {
    console.log('🚨 BUTTONS STILL BROKEN - Need to debug modal mounting');
  }
  process.exit(success ? 0 : 1);
}).catch(console.error);
