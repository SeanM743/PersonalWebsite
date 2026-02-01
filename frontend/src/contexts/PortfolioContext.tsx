import React, { createContext, useContext, useState, useCallback } from 'react';
import { apiService } from '../services/apiService';

interface PortfolioContextType {
    historyData: any[];
    historyPeriod: string;
    isHistoryLoading: boolean;
    loadHistory: (period: string) => Promise<void>;
    prefetchHistory: () => void;
}

const PortfolioContext = createContext<PortfolioContextType | undefined>(undefined);

export const PortfolioProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [historyData, setHistoryData] = useState<any[]>([]);
    const [historyPeriod, setHistoryPeriod] = useState('1M');
    const [isHistoryLoading, setIsHistoryLoading] = useState(false);
    const [hasPrefetched, setHasPrefetched] = useState(false);

    const loadHistory = useCallback(async (period: string) => {
        setIsHistoryLoading(true);
        setHistoryPeriod(period);
        try {
            const res = await apiService.getStockHistory(period);
            if ((res as any).success) {
                setHistoryData((res as any).data);
            }
        } catch (err) {
            console.error('Failed to load history', err);
        } finally {
            setIsHistoryLoading(false);
        }
    }, []);

    const prefetchHistory = useCallback(() => {
        if (!hasPrefetched) {
            setHasPrefetched(true);
            loadHistory('1M'); // Default period
        }
    }, [hasPrefetched, loadHistory]);

    return (
        <PortfolioContext.Provider value={{
            historyData,
            historyPeriod,
            isHistoryLoading,
            loadHistory,
            prefetchHistory
        }}>
            {children}
        </PortfolioContext.Provider>
    );
};

export const usePortfolio = () => {
    const context = useContext(PortfolioContext);
    if (!context) {
        throw new Error('usePortfolio must be used within PortfolioProvider');
    }
    return context;
};
