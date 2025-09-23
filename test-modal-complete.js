const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('🧪 Testing complete modal functionality...');
    
    await page.goto('http://localhost:8000/?ui=v5&direct=true');
    await page.waitForTimeout(3000);
    
    // Test 1: Modal opens
    console.log('\n✅ Test 1: Modal opens');
    const addButton = await page.locator('button:has-text("Add to Portfolio")').first();
    await addButton.click();
    
    // Wait for modal to appear
    await page.waitForTimeout(1000);
    await page.waitForSelector('#quantity-input', { timeout: 5000 });
    
    const modalVisible = await page.locator('#quantity-input').isVisible();
    console.log('Modal visible:', modalVisible ? '✅ YES' : '❌ NO');
    
    if (!modalVisible) {
      await browser.close();
      return;
    }
    
    // Test 2: Can input quantity
    console.log('\n✅ Test 2: Input quantity');
    await page.fill('#quantity-input', '100.5');
    const inputValue = await page.inputValue('#quantity-input');
    console.log('Input value:', inputValue === '100.5' ? '✅ CORRECT' : `❌ WRONG: ${inputValue}`);
    
    // Test 3: Save button works
    console.log('\n✅ Test 3: Save quantity');
    await page.click('text=Save');
    await page.waitForTimeout(1000);
    
    // Modal should close
    const modalClosed = await page.locator('#quantity-input').count();
    console.log('Modal closed after save:', modalClosed === 0 ? '✅ YES' : '❌ NO');
    
    // Should now show edit button instead of add button
    const editButtonExists = await page.locator('button:has-text("✏️")').count();
    console.log('Edit button appeared:', editButtonExists > 0 ? '✅ YES' : '❌ NO');
    
    // Test 4: Edit existing quantity
    if (editButtonExists > 0) {
      console.log('\n✅ Test 4: Edit existing quantity');
      await page.click('button:has-text("✏️")');
      await page.waitForTimeout(500);
      
      const editModalVisible = await page.locator('#quantity-input').isVisible();
      console.log('Edit modal visible:', editModalVisible ? '✅ YES' : '❌ NO');
      
      if (editModalVisible) {
        const currentValue = await page.inputValue('#quantity-input');
        console.log('Current value loaded:', currentValue === '100.5' ? '✅ CORRECT' : `❌ WRONG: ${currentValue}`);
        
        // Test 5: Cancel button
        console.log('\n✅ Test 5: Cancel button');
        await page.click('text=Cancel');
        await page.waitForTimeout(500);
        
        const canceledModalClosed = await page.locator('#quantity-input').count();
        console.log('Modal closed after cancel:', canceledModalClosed === 0 ? '✅ YES' : '❌ NO');
      }
    }
    
    console.log('\n🎉 Portfolio modal fix complete!');
    
  } catch (error) {
    console.log('❌ Error:', error.message);
  } finally {
    await browser.close();
  }
})();
