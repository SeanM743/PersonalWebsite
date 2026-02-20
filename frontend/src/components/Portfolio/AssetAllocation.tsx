import React from 'react';

interface AssetAllocationProps {
    allocationByType?: Record<string, number>;
    totalPortfolioValue?: number;
}

const AssetAllocation: React.FC<AssetAllocationProps> = ({ allocationByType, totalPortfolioValue }) => {
    return (
        <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
            <h3 className="text-lg font-semibold text-main mb-4">Asset Allocation</h3>
            <div className="space-y-4">
                {allocationByType && totalPortfolioValue && Object.entries(allocationByType).map(([type, value]) => {
                    const percentage = ((value / totalPortfolioValue) * 100);
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
    );
};

export default AssetAllocation;
