-- Sample Portfolio Data for Personal Dashboard
-- Run this script if you want to manually add the stock positions

-- Insert stock positions for admin user (user_id = 1)
INSERT INTO stock_tickers (symbol, purchase_price, quantity, user_id, notes, created_at, updated_at) 
VALUES 
    ('AMZN', 150.00, 10, 1, 'Amazon - E-commerce and cloud computing giant', NOW(), NOW()),
    ('SOFI', 8.50, 100, 1, 'SoFi Technologies - Digital financial services', NOW(), NOW()),
    ('ANET', 320.00, 5, 1, 'Arista Networks - Cloud networking solutions', NOW(), NOW()),
    ('INTC', 25.00, 50, 1, 'Intel Corporation - Semiconductor manufacturer', NOW(), NOW()),
    ('CRWV', 15.00, 25, 1, 'Crown Electrokinetics - Smart glass technology', NOW(), NOW())
ON CONFLICT (user_id, symbol) DO NOTHING;

-- Verify the data was inserted
SELECT 
    symbol,
    purchase_price,
    quantity,
    (purchase_price * quantity) as total_investment,
    notes
FROM stock_tickers 
WHERE user_id = 1 
ORDER BY symbol;