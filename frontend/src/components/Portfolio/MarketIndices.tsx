import React, { useState, useEffect } from 'react';
import { RefreshCw } from 'lucide-react';
import { apiService } from '../../services/apiService';

interface MarketIndex {
    symbol: string;
    name: string;
    price: number;
    change: number;
    changePercent: number;
}

const MarketIndices: React.FC = () => {
    const [indices, setIndices] = useState<MarketIndex[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // Cache key for localStorage
    const CACHE_KEY = 'market_indices_cache';
    const CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    useEffect(() => {
        loadIndices();
    }, []);

    const loadIndices = async () => {
        // Check cache first
        const cached = localStorage.getItem(CACHE_KEY);
        if (cached) {
            const { data, timestamp } = JSON.parse(cached);
            if (Date.now() - timestamp < CACHE_DURATION_MS) {
                setIndices(Array.isArray(data) ? data : []);
                setIsLoading(false);
                return;
            }
        }

        setIsLoading(true);
        try {
            const response = await apiService.getMarketIndices();
            if (response && (response as any).success) {
                const data = (response as any).data || [];
                setIndices(data);
                // Cache the result
                localStorage.setItem(CACHE_KEY, JSON.stringify({
                    data,
                    timestamp: Date.now()
                }));
            }
        } catch (error) {
            console.error('Failed to load market indices:', error);
            // Use cached data if available even if expired
            if (cached) {
                const { data } = JSON.parse(cached);
                setIndices(data || []);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleRefresh = () => {
        localStorage.removeItem(CACHE_KEY);
        loadIndices();
    };

    if (isLoading && indices.length === 0) {
        return (
            <div className="bg-card rounded-lg p-3 shadow-sm border border-border animate-pulse">
                <div className="flex space-x-4">
                    {[1, 2, 3, 4, 5].map((i) => (
                        <div key={i} className="flex-1 h-12 bg-muted/20 rounded"></div>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="bg-card rounded-lg p-4 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-3">
                <span className="text-xs text-muted uppercase font-medium tracking-wider">Market Overview</span>
                <button
                    onClick={handleRefresh}
                    className="p-1 hover:bg-muted/20 rounded transition-colors"
                    title="Refresh indices"
                >
                    <RefreshCw className={`h-3 w-3 text-muted ${isLoading ? 'animate-spin' : ''}`} />
                </button>
            </div>
            <div className="space-y-2">
                {Array.isArray(indices) && indices.map((index) => (
                    <div key={index.symbol} className="flex items-center justify-between py-1">
                        <span className="text-sm text-muted">{index.name}</span>
                        <div className="text-right">
                            <span className="text-sm font-medium text-main">
                                {index.symbol === 'BTC-USD' || index.symbol === 'GC=F'
                                    ? `$${index.price.toLocaleString(undefined, { maximumFractionDigits: 0 })}`
                                    : index.price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
                                }
                            </span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default MarketIndices;
