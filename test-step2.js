const { chromium } = require('playwright');

async function testStep2() {
  console.log('🧪 STEP 2: Testing Figure Markets direct API call...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  let success = false;
  
  page.on('console', msg => {
    const text = msg.text();
    
    if (text.includes('✅ PHASE 1: Direct API test success')) {
      console.log(`🎉 API SUCCESS: ${text}`);
      success = true;
    } else if (text.includes('🚀 PHASE 1') || text.includes('📡 PHASE 1') || text.includes('✅ PHASE 1')) {
      console.log(`📊 API Log: ${text}`);
    } else if (text.includes('❌ PHASE 1')) {
      console.log(`❌ API Error: ${text}`);
    }
  });
  
  // Listen for network errors
  page.on('response', response => {
    if (response.url().includes('figuremarkets.com')) {
      console.log(`🌐 Figure Markets API: ${response.status()} ${response.url()}`);
    }
  });
  
  try {
    console.log('📱 Loading V5 with direct API enabled...');
    await page.goto('http://localhost:8000/?ui=v5&direct=true', { 
      waitUntil: 'networkidle',
      timeout: 20000 
    });
    
    console.log('⏳ Waiting for API calls...');
    await page.waitForTimeout(5000);
    
    if (success) {
      console.log('✅ STEP 2 SUCCESS: Figure Markets direct API working!');
      return true;
    } else {
      console.log('❌ STEP 2 FAILED: No success confirmation');
      return false;
    }
    
  } catch (error) {
    console.error('❌ STEP 2 ERROR:', error.message);
    return false;
  } finally {
    await browser.close();
  }
}

testStep2().then(success => {
  if (success) {
    console.log('🎉 STEP 2 COMPLETE - Ready for Step 3');
  } else {
    console.log('🚨 STEP 2 FAILED - Fix before proceeding');
  }
  process.exit(success ? 0 : 1);
}).catch(console.error);
