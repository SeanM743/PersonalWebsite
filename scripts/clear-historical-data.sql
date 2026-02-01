-- Clear Historical Portfolio Balance Data
-- This script safely removes calculated historical balances while preserving all source data
-- 
-- SAFE TO RUN: Only affects account_balance_history table
-- NOT AFFECTED: stock_transactions, accounts, users, and all other tables
--
-- The system will automatically reconstruct this data from transactions + Finnhub API

BEGIN;

-- Show what will be deleted
SELECT 
    COUNT(*) as total_records,
    MIN(date) as earliest_date,
    MAX(date) as latest_date,
    COUNT(DISTINCT account_id) as affected_accounts
FROM account_balance_history;

-- Uncomment the line below to actually delete the data
-- DELETE FROM account_balance_history;

-- If you uncommented the DELETE, commit the transaction
-- COMMIT;

-- Otherwise, rollback (no changes made)
ROLLBACK;
