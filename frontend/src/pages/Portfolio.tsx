import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import AccountSidebar from '../components/Portfolio/AccountSidebar';
import AccountDetails from '../components/Portfolio/AccountDetails';
import PortfolioOverview from '../components/Portfolio/PortfolioOverview';
import LoadingSpinner from '../components/UI/LoadingSpinner';

const Portfolio: React.FC = () => {
  const [accounts, setAccounts] = useState<any[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Fetch accounts on mount
  const fetchAccounts = async () => {
    setIsLoading(true);
    try {
      const res = await apiService.getAccounts();
      if (res.success) {
        setAccounts(res.data);
      }
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