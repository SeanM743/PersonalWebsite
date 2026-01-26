import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import LoadingSpinner from '../UI/LoadingSpinner';
import {
    TrendingUp,
    TrendingDown,
    DollarSign
} from 'lucide-react';

interface Holding {
    id: number;
    symbol: string;
    quantity: number;
    purchasePrice: number;
    totalInvestment: number;
    currentPrice?: number;
    currentValue?: number;
    totalGainLoss?: number;
    totalGainLossPercentage?: number;
}

interface CompletePortfolioSummary {
    stockPortfolio: any;
    totalPortfolioValue: number;
    // ... other fields matching original interface
    allocationByType: Record<string, number>;
    totalStockValue: number;
}

const PortfolioOverview: React.FC = () => {
    const [completePortfolio, setCompletePortfolio] = useState<CompletePortfolioSummary | null>(null);
    const [holdings, setHoldings] = useState<Holding[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedPeriod, setSelectedPeriod] = useState('6M');

    const loadPortfolioData = async () => {
        try {
            const [completeRes, holdingsRes] = await Promise.all([
                apiService.getCompletePortfolio(true),
                apiService.getHoldings(),
            ]);

            if (completeRes.success) {
                setCompletePortfolio(completeRes.data);
            }

            if (holdingsRes.success) {
                setHoldings(holdingsRes.data);
            }
        } catch (err: any) {
            console.error('Failed to load portfolio', err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadPortfolioData();
    }, []);

    // Calculate top holding by current value
    const topHolding = holdings.reduce((prev, current) => {
        if (!prev) return current;
        const prevValue = prev.currentValue || prev.totalInvestment;
        const currentValue = current.currentValue || current.totalInvestment;
        return (currentValue > prevValue) ? current : prev;
    }, holdings[0]);

    // Calculate portfolio percentage for top holding
    const topHoldingPercentage = topHolding && completePortfolio && completePortfolio.totalStockValue ?
        ((topHolding.currentValue || topHolding.totalInvestment) / completePortfolio.totalStockValue * 100).toFixed(1) : '0';

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <LoadingSpinner size="large" />
            </div>
        );
    }

    return (
        <div className="space-y-6 p-6 animate-fadeIn">
            {/* Top Metrics Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Total Portfolio Value */}
                <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                    <div className="flex items-start justify-between">
                        <div>
                            <div className="flex items-center text-muted text-sm mb-1">
                                <DollarSign className="h-4 w-4 mr-1" />
                                Total Portfolio Value
                            </div>
                            <div className="text-2xl font-bold text-main">
                                ${completePortfolio?.totalPortfolioValue?.toLocaleString('en-US', {
                                    minimumFractionDigits: 0,
                                    maximumFractionDigits: 0
                                }) || '0'}
                            </div>
                            <div className="flex items-center text-green-600 text-sm mt-1">
                                <TrendingUp className="h-3 w-3 mr-1" />
                                Current Value
                            </div>
                        </div>
                    </div>
                </div>

                {/* Total Gain/Loss */}
                <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                    <div className="flex items-start justify-between">
                        <div>
                            <div className="flex items-center text-muted text-sm mb-1">
                                <TrendingUp className="h-4 w-4 mr-1" />
                                Total Gain/Loss
                            </div>
                            <div className="text-2xl font-bold text-main">
                                ${completePortfolio?.stockPortfolio?.totalGainLoss?.toLocaleString('en-US', {
                                    minimumFractionDigits: 0,
                                    maximumFractionDigits: 0
                                }) || '0'}
                            </div>
                            <div className="text-gray-500 text-sm mt-1">
                                Stocks only
                            </div>
                        </div>
                    </div>
                </div>

                {/* Top Holding */}
                <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                    <div className="flex items-start justify-between">
                        <div>
                            <div className="flex items-center text-muted text-sm mb-1">
                                <div className="w-4 h-4 rounded-full bg-blue-500 mr-1"></div>
                                Top Holding
                            </div>
                            <div className="text-2xl font-bold text-main">
                                {topHolding?.symbol || 'N/A'}
                            </div>
                            <div className="text-muted text-sm mt-1">
                                {topHoldingPercentage}% of stocks
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Portfolio Performance Section */}
            <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-lg font-semibold text-main">Portfolio Performance</h2>
                    <div className="flex space-x-1 bg-page rounded-lg p-1 border border-border">
                        {['1M', '3M', '6M', '1Y'].map((period) => (
                            <button
                                key={period}
                                onClick={() => setSelectedPeriod(period)}
                                className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${selectedPeriod === period
                                    ? 'bg-primary text-white'
                                    : 'text-muted hover:text-main hover:bg-card'
                                    }`}
                            >
                                {period}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Performance metrics grid - using available data */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="text-center p-4 bg-page rounded-lg border border-border">
                        <div className="text-2xl font-bold text-muted">N/A</div>
                        <div className="text-sm text-muted">This Month</div>
                    </div>
                    <div className="text-center p-4 bg-page rounded-lg border border-border">
                        <div className="text-2xl font-bold text-muted">N/A</div>
                        <div className="text-sm text-muted">This Quarter</div>
                    </div>
                    <div className="text-center p-4 bg-page rounded-lg border border-border">
                        <div className={`text-2xl font-bold ${(completePortfolio?.stockPortfolio?.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'
                            }`}>
                            {completePortfolio?.stockPortfolio?.totalGainLossPercentage ?
                                `${(completePortfolio.stockPortfolio.totalGainLossPercentage >= 0 ? '+' : '')}${completePortfolio.stockPortfolio.totalGainLossPercentage.toFixed(1)}%` :
                                'N/A'
                            }
                        </div>
                        <div className="text-sm text-muted">Total Return</div>
                    </div>
                    <div className="text-center p-4 bg-page rounded-lg border border-border">
                        <div className="text-2xl font-bold text-primary">{holdings.length}</div>
                        <div className="text-sm text-muted">Total Positions</div>
                    </div>
                </div>
            </div>

            {/* Bottom Section */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Asset Allocation */}
                <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                    <h3 className="text-lg font-semibold text-main mb-4">Asset Allocation</h3>
                    <div className="space-y-4">
                        {completePortfolio?.allocationByType && Object.entries(completePortfolio.allocationByType).map(([type, value]) => {
                            const percentage = ((value / completePortfolio.totalPortfolioValue) * 100);
                            const colors: Record<string, string> = {
                                'Stocks': 'bg-blue-500',
                                'Cash': 'bg-green-500',
                                'Retirement': 'bg-purple-500',
                                'Other Investments': 'bg-orange-500'
                            };

                            return (
                                <div key={type} className="flex items-center justify-between">
                                    <div className="flex items-center space-x-3">
                                        <div className={`w-3 h-3 rounded-full ${colors[type] || 'bg-muted'}`}></div>
                                        <span className="text-sm font-medium text-main">{type}</span>
                                        <span className="text-sm text-muted">{percentage.toFixed(0)}%</span>
                                    </div>
                                    <div className="text-sm font-semibold text-main">
                                        ${value.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* Top Holdings */}
                <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                    <h3 className="text-lg font-semibold text-main mb-4">Top Holdings</h3>
                    <div className="space-y-3">
                        {holdings
                            .sort((a, b) => (b.currentValue || b.totalInvestment) - (a.currentValue || a.totalInvestment))
                            .slice(0, 5)
                            .map((holding) => {
                                const currentValue = holding.currentValue || holding.totalInvestment;
                                const gainLoss = holding.totalGainLoss || 0;
                                const gainLossPercentage = holding.totalGainLossPercentage || 0;
                                const isPositive = gainLoss >= 0;

                                return (
                                    <div key={holding.symbol} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                                        <div>
                                            <div className="font-medium text-main">{holding.symbol}</div>
                                            <div className="text-sm text-muted">
                                                {holding.quantity.toLocaleString()} shares
                                            </div>
                                        </div>
                                        <div className="text-right">
                                            <div className="font-semibold text-main">
                                                ${currentValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                            </div>
                                            <div className={`text-sm flex items-center justify-end ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                                                {isPositive ? <TrendingUp className="h-3 w-3 mr-1" /> : <TrendingDown className="h-3 w-3 mr-1" />}
                                                {isPositive ? '+' : ''}{gainLossPercentage.toFixed(2)}%
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PortfolioOverview;
