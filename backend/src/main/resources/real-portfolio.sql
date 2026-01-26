-- Real Portfolio Data for Personal Dashboard
-- Based on actual portfolio holdings as of January 2025

-- Clear existing data for admin user (user_id = 1)
DELETE FROM stock_tickers WHERE user_id = 1;

-- Insert actual stock positions for admin user (user_id = 1)
-- Note: Only including publicly traded stocks that can be tracked via Finnhub API
INSERT INTO stock_tickers (symbol, purchase_price, quantity, user_id, notes, created_at, updated_at) 
VALUES 
    -- Major Holdings
    ('AMZN', 232.38, 3462, 1, 'Amazon.com Inc - E-commerce and cloud computing giant', NOW(), NOW()),
    ('SOFI', 27.48, 3500, 1, 'SoFi Technologies Inc - Digital financial services platform', NOW(), NOW()),
    ('ANET', 130.77, 600, 1, 'Arista Networks Inc - Cloud networking solutions', NOW(), NOW()),
    ('QQQ', 623.93, 109.903, 1, 'Invesco QQQ Trust ETF - Nasdaq 100 tracking ETF', NOW(), NOW()),
    ('CRWV', 78.87, 634.128, 1, 'CoreWeave Inc Class A - AI cloud infrastructure', NOW(), NOW()),
    ('ACHR', 8.13, 5000, 1, 'Archer Aviation Inc - Electric vertical takeoff aircraft', NOW(), NOW()),
    ('INTC', 36.16, 906.672, 1, 'Intel Corporation - Semiconductor manufacturer', NOW(), NOW()),
    ('SOUN', 10.90, 2500, 1, 'SoundHound AI Inc Class A - Voice AI technology', NOW(), NOW())
ON CONFLICT (user_id, symbol) DO NOTHING;

-- Note: The following holdings are not included as they are not publicly traded stocks:
-- - FCASH (Cash position): $394,117.62
-- - S Fund (TSP/401k): $193,020.35  
-- - O24H (Vanguard Target 2045): $118,260.78
-- - NH2027959 (NH Portfolio 2027): $48,011.94
-- - NH0000909 (NH College Portfolio): $23,159.58
-- - G Fund (TSP/401k): $4,707.43
-- - FDRXX (Fidelity Cash Reserves): $3,552.12

-- Verify the data was inserted
SELECT 
    symbol,
    purchase_price,
    quantity,
    (purchase_price * quantity) as total_investment,
    notes
FROM stock_tickers 
WHERE user_id = 1 
ORDER BY (purchase_price * quantity) DESC;

-- Portfolio Summary
SELECT 
    COUNT(*) as total_positions,
    SUM(purchase_price * quantity) as total_stock_investment
FROM stock_tickers 
WHERE user_id = 1;