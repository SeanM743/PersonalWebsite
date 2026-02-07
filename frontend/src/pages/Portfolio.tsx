import React, { useState, useEffect, useRef } from 'react';
import { apiService } from '../services/apiService';
import AccountSidebar from '../components/Portfolio/AccountSidebar';
import AccountDetails from '../components/Portfolio/AccountDetails';
import PortfolioOverview from '../components/Portfolio/PortfolioOverview';
import LoadingSpinner from '../components/UI/LoadingSpinner';

// Cache for prefetched stock data
interface PrefetchedData {
  transactions: any[];
  holdings: any[];
}

const Portfolio: React.FC = () => {
  const [accounts, setAccounts] = useState<any[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Prefetched stock data cache - available immediately when user clicks stock account
  const prefetchedDataRef = useRef<PrefetchedData | null>(null);

  // Fetch accounts and prefetch stock data sequentially to correct balance sync
  const fetchAccounts = async () => {
    setIsLoading(true);
    try {
      // 1. Fetch Transactions and Holdings first
      // getHoldings triggers the DB balance update for stock accounts
      const [txnsRes, holdingsRes] = await Promise.all([
        apiService.getTransactions(),
        apiService.getHoldings()
      ]);

      // 2. Fetch Accounts AFTER holdings have potentially updated the DB
      // This ensures we get the latest calculated balance
      const accountsRes = await apiService.getAccounts();

      if ((accountsRes as any).success) {
        setAccounts((accountsRes as any).data);
      }

      // Cache prefetched data for immediate use when stock account is selected
      prefetchedDataRef.current = {
        transactions: (txnsRes as any)?.success ? (txnsRes as any).data : [],
        holdings: (holdingsRes as any)?.success ? (holdingsRes as any).data : []
      };

    } catch (e) {
      console.error("Failed to fetch accounts", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const selectedAccount = accounts.find(a => a.id === selectedAccountId);

  return (
    <div className="flex h-[calc(100vh-64px)] bg-page overflow-hidden">
      {/* Sidebar */}
      <AccountSidebar
        accounts={accounts}
        selectedAccountId={selectedAccountId}
        onSelectAccount={setSelectedAccountId}
        onAccountsChanged={fetchAccounts}
        isLoading={isLoading}
      />

      {/* Main Content Area */}
      <div className="flex-1 overflow-y-auto">
        {selectedAccountId === null ? (
          <PortfolioOverview />
        ) : selectedAccount ? (
          <AccountDetails
            account={selectedAccount}
            onUpdate={fetchAccounts} // Refresh accounts if balance changes
          />
        ) : (
          <div className="flex items-center justify-center h-full text-muted">
            Account not found
          </div>
        )}
      </div>
    </div>
  );
};

export default Portfolio;