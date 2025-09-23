const { chromium } = require('playwright');

async function testStep1() {
  console.log('🧪 STEP 1: Testing feature flag detection...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  let featureFlagLogs = [];
  
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('🔍 PHASE 1: Feature flag check')) {
      featureFlagLogs.push(text);
      console.log(`✅ Feature Flag Log: ${text}`);
    } else if (text.includes('❌') || text.includes('Error')) {
      console.log(`❌ Error: ${text}`);
    }
  });
  
  try {
    // Test 1: Without direct=true flag
    console.log('📱 Test 1: Loading without direct=true flag...');
    await page.goto('http://localhost:8000/?ui=v5', { 
      waitUntil: 'networkidle',
      timeout: 15000 
    });
    
    await page.waitForTimeout(2000);
    
    // Test 2: With direct=true flag  
    console.log('📱 Test 2: Loading with direct=true flag...');
    await page.goto('http://localhost:8000/?ui=v5&direct=true', {
      waitUntil: 'networkidle', 
      timeout: 15000
    });
    
    await page.waitForTimeout(2000);
    
    // Test 3: Set localStorage and test without URL param
    console.log('📱 Test 3: Testing localStorage persistence...');
    await page.evaluate(() => {
      localStorage.setItem('enable-direct-api', 'true');
    });
    
    await page.goto('http://localhost:8000/?ui=v5', {
      waitUntil: 'networkidle',
      timeout: 15000  
    });
    
    await page.waitForTimeout(2000);
    
    console.log('🎯 Step 1 Results:');
    console.log(`📊 Total feature flag logs: ${featureFlagLogs.length}`);
    
    if (featureFlagLogs.length >= 3) {
      console.log('✅ STEP 1 SUCCESS: Feature flag detection working!');
      return true;
    } else {
      console.log('❌ STEP 1 FAILED: Missing feature flag logs');
      return false;
    }
    
  } catch (error) {
    console.error('❌ STEP 1 ERROR:', error.message);
    return false;
  } finally {
    await browser.close();
  }
}

testStep1().then(success => {
  if (success) {
    console.log('🎉 STEP 1 COMPLETE - Ready for Step 2');
  } else {
    console.log('🚨 STEP 1 FAILED - Fix before proceeding');
  }
  process.exit(success ? 0 : 1);
}).catch(console.error);
