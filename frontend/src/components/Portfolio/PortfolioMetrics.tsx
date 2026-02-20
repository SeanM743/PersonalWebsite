import React, { useState } from 'react';
import { TrendingUp, TrendingDown, DollarSign } from 'lucide-react';

interface Holding {
    symbol: string;
    currentValue?: number;
    totalInvestment: number;
}

interface CompletePortfolioSummary {
    totalPortfolioValue: number;
    totalStockValue: number;
    totalGainLossYTD: number;
    totalGainLossPercentageYTD: number;
    totalGainLoss7d?: number;
    totalGainLossPercentage7d?: number;
    totalGainLoss1m?: number;
    totalGainLossPercentage1m?: number;
    totalGainLoss3m?: number;
    totalGainLossPercentage3m?: number;
}

interface PortfolioMetricsProps {
    completePortfolio: CompletePortfolioSummary | null;
    topHolding: Holding | undefined;
}

type ReturnPeriod = '7d' | '1m' | '3m' | 'ytd';

const PortfolioMetrics: React.FC<PortfolioMetricsProps> = ({ completePortfolio, topHolding }) => {
    const [returnPeriod, setReturnPeriod] = useState<ReturnPeriod>('ytd');

    const getReturnData = () => {
        if (!completePortfolio) return { gain: 0, pct: 0 };
        switch (returnPeriod) {
            case '7d': return { gain: completePortfolio.totalGainLoss7d || 0, pct: completePortfolio.totalGainLossPercentage7d || 0 };
            case '1m': return { gain: completePortfolio.totalGainLoss1m || 0, pct: completePortfolio.totalGainLossPercentage1m || 0 };
            case '3m': return { gain: completePortfolio.totalGainLoss3m || 0, pct: completePortfolio.totalGainLossPercentage3m || 0 };
            case 'ytd':
            default: return { gain: completePortfolio.totalGainLossYTD || 0, pct: completePortfolio.totalGainLossPercentageYTD || 0 };
        }
    };

    const { gain: returnGain, pct: returnPct } = getReturnData();
    const returnLabel = {
        '7d': '7-Day Return',
        '1m': '1-Month Return',
        '3m': '3-Month Return',
        'ytd': `YTD Gain/Loss (${new Date().getFullYear()})`
    }[returnPeriod];

    const topHoldingPercentage = topHolding && completePortfolio && completePortfolio.totalStockValue ?
        ((topHolding.currentValue || topHolding.totalInvestment) / completePortfolio.totalStockValue * 100).toFixed(1) : '0';

    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
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

            <div className="bg-card rounded-lg p-4 shadow-sm border border-border flex flex-col justify-between">
                <div className="flex items-start justify-between mb-2">
                    <div>
                        <div className="flex items-center text-muted text-sm mb-1">
                            <TrendingUp className="h-4 w-4 mr-1" />
                            {returnLabel}
                        </div>
                        <div className={`text-2xl font-bold ${returnGain >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                            {returnGain >= 0 ? '+' : '-'}${Math.abs(returnGain).toLocaleString('en-US', {
                                minimumFractionDigits: 0,
                                maximumFractionDigits: 0
                            })}
                        </div>
                        <div className={`text-sm mt-1 flex items-center ${returnPct >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                            {returnPct >= 0 ? <TrendingUp className="h-3 w-3 mr-1" /> : <TrendingDown className="h-3 w-3 mr-1" />}
                            {Math.abs(returnPct).toFixed(2)}%
                        </div>
                    </div>
                </div>

                <div className="flex bg-muted/30 rounded-md p-1 mt-2">
                    {(['7d', '1m', '3m', 'ytd'] as ReturnPeriod[]).map((period) => (
                        <button
                            key={period}
                            onClick={() => setReturnPeriod(period)}
                            className={`flex-1 px-2 py-1 text-xs font-medium rounded-sm transition-colors ${returnPeriod === period
                                ? 'bg-primary text-primary-foreground shadow-sm'
                                : 'text-muted hover:text-main hover:bg-muted/50'
                                }`}
                        >
                            {period.toUpperCase()}
                        </button>
                    ))}
                </div>
            </div>

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
    );
};

export default PortfolioMetrics;
