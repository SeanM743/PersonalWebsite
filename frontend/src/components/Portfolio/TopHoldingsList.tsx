import React from 'react';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface Holding {
    symbol: string;
    quantity: number;
    currentValue?: number;
    totalInvestment: number;
    totalGainLoss?: number;
    totalGainLossPercentage?: number;
}

interface TopHoldingsListProps {
    holdings: Holding[];
}

const TopHoldingsList: React.FC<TopHoldingsListProps> = ({ holdings }) => {
    return (
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
    );
};

export default TopHoldingsList;
