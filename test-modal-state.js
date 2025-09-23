const { chromium } = require('playwright');

async function debugModalState() {
  console.log('🧪 Debugging modal state and overlays...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('📱 Loading app...');
    await page.goto('http://localhost:8000/?direct=true', { 
      waitUntil: 'networkidle',
      timeout: 20000 
    });
    
    await page.waitForTimeout(3000);
    
    // Check for any modal overlays
    const overlays = await page.locator('[class*="fixed"][class*="inset-0"]').all();
    console.log(`🔍 Found ${overlays.length} fixed overlay elements`);
    
    for (let i = 0; i < overlays.length; i++) {
      const overlay = overlays[i];
      const isVisible = await overlay.isVisible();
      const classes = await overlay.getAttribute('class');
      console.log(`📋 Overlay ${i+1}: visible=${isVisible}, classes="${classes}"`);
      
      if (isVisible) {
        const text = await overlay.textContent();
        console.log(`📄 Overlay ${i+1} content: "${text.substring(0, 100)}..."`);
      }
    }
    
    // Check specific modal states in the DOM
    const currencyModal = await page.locator('text=Select Currency').count();
    const portfolioModal = await page.locator('text=Edit').count();
    console.log(`💱 Currency modal count: ${currencyModal}`);
    console.log(`📊 Portfolio modal count: ${portfolioModal}`);
    
    return overlays.length;
    
  } catch (error) {
    console.error('❌ Debug failed:', error.message);
    return -1;
  } finally {
    await browser.close();
  }
}

debugModalState().then(overlayCount => {
  console.log(`🎯 Found ${overlayCount} overlay elements blocking interactions`);
}).catch(console.error);
