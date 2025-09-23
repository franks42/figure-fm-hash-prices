const { chromium } = require('playwright');

async function testLiveData() {
  console.log('🧪 Testing live-data-first orchestrated fetch...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  let liveData = {
    figure: false,
    twelve: false,
    fallback: false
  };
  
  page.on('console', msg => {
    const text = msg.text();
    
    if (text.includes('🚀 LIVE: Orchestrated fetch')) {
      console.log(`🚀 ORCHESTRATED: ${text}`);
    } else if (text.includes('✅ LIVE: Figure Markets success')) {
      console.log(`✅ FIGURE: ${text}`);
      liveData.figure = true;
    } else if (text.includes('✅ LIVE: Twelve Data success')) {
      console.log(`✅ TWELVE: ${text}`);
      liveData.twelve = true;
    } else if (text.includes('✅ LIVE: Provider success')) {
      console.log(`✅ PROVIDER: ${text}`);
    } else if (text.includes('✅ LIVE: All providers successful')) {
      console.log(`✅ SUCCESS: ${text}`);
      liveData.fallback = false;
    } else if (text.includes('🔄 LIVE: Some providers failed')) {
      console.log(`❌ FALLBACK: ${text}`);
      liveData.fallback = true;
    } else if (text.includes('❌ LIVE:')) {
      console.log(`❌ ERROR: ${text}`);
    }
  });
  
  page.on('response', response => {
    if (response.url().includes('figuremarkets.com')) {
      console.log(`🌐 Figure Markets: ${response.status()}`);
    } else if (response.url().includes('twelvedata.com')) {
      console.log(`🌐 Twelve Data: ${response.status()}`);
    }
  });
  
  try {
    console.log('📱 Loading with direct API enabled...');
    await page.goto('http://localhost:8000/?direct=true', { 
      waitUntil: 'networkidle',
      timeout: 20000 
    });
    
    console.log('⏳ Waiting for orchestrated fetch...');
    await page.waitForTimeout(5000);
    
    console.log('🎯 Live Data Results:');
    console.log(`📊 Figure Markets: ${liveData.figure ? 'SUCCESS' : 'FAILED'}`);
    console.log(`📊 Twelve Data: ${liveData.twelve ? 'SUCCESS' : 'FAILED'}`);
    console.log(`📊 Fallback triggered: ${liveData.fallback ? 'YES' : 'NO'}`);
    
    const success = liveData.figure && liveData.twelve && !liveData.fallback;
    return success;
    
  } catch (error) {
    console.error('❌ Live data test failed:', error.message);
    return false;
  } finally {
    await browser.close();
  }
}

testLiveData().then(success => {
  if (success) {
    console.log('🎉 LIVE DATA SUCCESS - Orchestrated fetch working!');
  } else {
    console.log('🚨 LIVE DATA FAILED - Check logs above');
  }
  process.exit(success ? 0 : 1);
}).catch(console.error);
