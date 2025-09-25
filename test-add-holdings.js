const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    console.log('🧪 Adding portfolio holdings to test PF card...');
    
    await page.goto('http://localhost:8000/');
    await page.waitForTimeout(8000); // Wait for data to load
    
    // Step 1: Verify no PF card initially
    console.log('\n✅ Step 1: Initial state (no portfolio)');
    const initialPfCards = await page.locator('text=PF').count();
    console.log('PF cards initially:', initialPfCards);
    
    // Step 2: Find any "Add to Portfolio" button
    console.log('\n✅ Step 2: Looking for portfolio buttons...');
    
    const addButtons = await page.locator('button:has-text("Add to Portfolio")').count();
    const editButtons = await page.locator('button:has-text("✏️")').count();
    
    console.log('Add buttons found:', addButtons);
    console.log('Edit buttons found:', editButtons);
    
    if (addButtons === 0) {
      console.log('❌ No "Add to Portfolio" buttons found!');
      console.log('🔍 Looking for any portfolio-related buttons...');
      
      const allButtons = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('button'));
        return buttons.map(b => b.textContent.trim()).filter(t => t.length > 0);
      });
      
      console.log('All button text found:', allButtons);
      return;
    }
    
    // Step 3: Add holdings to trigger PF card
    console.log('\n✅ Step 3: Adding portfolio holdings...');
    
    // Add HASH holdings
    console.log('Adding HASH holdings...');
    const hashAddButton = await page.locator('button:has-text("Add to Portfolio")').first();
    await hashAddButton.click();
    await page.waitForSelector('#quantity-input', { timeout: 5000 });
    await page.fill('#quantity-input', '1000');
    await page.click('text=Save');
    await page.waitForTimeout(2000);
    
    // Add BTC holdings if possible
    const btcAddButton = await page.locator('button:has-text("Add to Portfolio")').first();
    const btcExists = await btcAddButton.count();
    
    if (btcExists > 0) {
      console.log('Adding BTC holdings...');
      await btcAddButton.click();
      await page.waitForTimeout(500);
      await page.fill('#quantity-input', '0.5');
      await page.click('text=Save');
      await page.waitForTimeout(2000);
    }
    
    // Step 4: Check if PF card appears
    console.log('\n✅ Step 4: Checking for PF card after adding holdings...');
    const pfCardsAfter = await page.locator('text=PF').count();
    console.log('PF cards after adding holdings:', pfCardsAfter);
    
    if (pfCardsAfter > 0) {
      console.log('🎉 PF CARD APPEARED!');
      
      // Step 5: Analyze PF card content
      console.log('\n✅ Step 5: Analyzing PF card content...');
      
      const pfCardAnalysis = await page.evaluate(() => {
        // Find elements containing "PF"
        const pfElements = Array.from(document.querySelectorAll('*')).filter(el => 
          el.textContent && el.textContent.includes('PF') && el.textContent.trim() !== 'PF'
        );
        
        if (pfElements.length === 0) return { found: false };
        
        // Find the card container
        let pfCard = null;
        for (const el of pfElements) {
          const card = el.closest('[class*="bg-white/"][class*="border"]');
          if (card) {
            pfCard = card;
            break;
          }
        }
        
        if (!pfCard) return { found: false };
        
        // Extract card information
        const text = pfCard.textContent;
        const priceMatch = text.match(/\$[\d,]+\.?\d*/);
        const changeMatch = text.match(/[▲▼][\d.]+%/);
        
        return {
          found: true,
          symbol: text.includes('PF') ? 'PF' : 'not found',
          price: priceMatch ? priceMatch[0] : 'not found',
          change: changeMatch ? changeMatch[0] : 'not found',
          description: text.includes('Total Portfolio') ? 'Total Portfolio' : 'not found',
          feedIndicator: text.includes('[PF]') || text.includes('PF') ? 'PF' : 'not found',
          hasPortfolioSection: text.includes('Add to Portfolio') || text.includes('✏️'),
          fullText: text.substring(0, 300)
        };
      });
      
      console.log('PF card analysis:', pfCardAnalysis);
      
      if (pfCardAnalysis.found) {
        console.log('🎯 PF Card Verification:');
        console.log('  Symbol "PF":', pfCardAnalysis.symbol === 'PF' ? '✅' : '❌');
        console.log('  Price display:', pfCardAnalysis.price !== 'not found' ? '✅' : '❌');
        console.log('  Change percentage:', pfCardAnalysis.change !== 'not found' ? '✅' : '❌');
        console.log('  Description:', pfCardAnalysis.description === 'Total Portfolio' ? '✅' : '❌');
        console.log('  Feed indicator:', pfCardAnalysis.feedIndicator === 'PF' ? '✅' : '❌');
        console.log('  No portfolio section:', !pfCardAnalysis.hasPortfolioSection ? '✅' : '❌');
      }
    } else {
      console.log('❌ PF card did not appear after adding holdings');
      console.log('🔍 Debugging: Checking card count and portfolio state...');
      
      const totalCards = await page.locator('[class*="bg-white/"][class*="border"]').count();
      const portfolioState = await page.evaluate(() => {
        if (typeof window.re_frame !== 'undefined') {
          try {
            const holdings = window.re_frame.core.deref(
              window.re_frame.core.subscribe([':portfolio/holdings'])
            );
            const hasHoldings = window.re_frame.core.deref(
              window.re_frame.core.subscribe([':portfolio/has-holdings?'])
            );
            return { holdings, hasHoldings };
          } catch (e) {
            return { error: e.message };
          }
        }
        return { error: 're-frame not available' };
      });
      
      console.log('Total cards found:', totalCards);
      console.log('Portfolio state:', portfolioState);
    }
    
    console.log('\n⏸️ Keeping browser open for manual inspection...');
    
  } catch (error) {
    console.log('❌ Error:', error.message);
  } finally {
    // Keep browser open for inspection
    // await browser.close();
  }
})();
