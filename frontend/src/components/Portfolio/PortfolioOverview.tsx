import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import { usePortfolio } from '../../contexts/PortfolioContext';
import LoadingSpinner from '../UI/LoadingSpinner';
import TimeRangeSelector from './TimeRangeSelector';
import WatchlistWidget from './WatchlistWidget';
import MarketIndices from './MarketIndices';
import MarketHeatMap from './MarketHeatMap';
import { Link } from 'react-router-dom';
import {
    TrendingUp,
    TrendingDown,
    DollarSign,
    FlaskConical,
    ExternalLink
} from 'lucide-react';
import {
    AreaChart,
    Area,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    ReferenceLine
} from 'recharts';

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

    // Period Returns
    totalGainLossYTD: number;
    totalGainLossPercentageYTD: number;
    totalGainLoss7d?: number;
    totalGainLossPercentage7d?: number;
    totalGainLoss1m?: number;
    totalGainLossPercentage1m?: number;
    totalGainLoss3m?: number;
    totalGainLossPercentage3m?: number;
}


interface PortfolioResponse<T> {
    success: boolean;
    data?: T;
    message?: string;
}

type ReturnPeriod = '7d' | '1m' | '3m' | 'ytd';

const PortfolioOverview: React.FC = () => {
    const [completePortfolio, setCompletePortfolio] = useState<CompletePortfolioSummary | null>(null);
    const [holdings, setHoldings] = useState<Holding[]>([]);
    const [selectedPeriod, setSelectedPeriod] = useState('1M');
    const [returnPeriod, setReturnPeriod] = useState<ReturnPeriod>('ytd');

    // Track active value for horizontal crosshair line
    const [activeValue, setActiveValue] = useState<number | null>(null);

    // Use Portfolio Context for history data
    const { historyData, isHistoryLoading, loadHistory } = usePortfolio();

    const loadPortfolioData = async () => {
        try {
            const [completeRes, holdingsRes] = await Promise.all([
                apiService.getCompletePortfolio(true) as Promise<PortfolioResponse<CompletePortfolioSummary>>,
                apiService.getHoldings() as Promise<PortfolioResponse<Holding[]>>,
            ]);

            if (completeRes.success) {
                setCompletePortfolio(completeRes.data || null);
            }

            if (holdingsRes.success) {
                setHoldings(holdingsRes.data || []);
            }
        } catch (err: any) {
            console.error('Failed to load portfolio', err);
        }
    };

    useEffect(() => {
        loadPortfolioData();
    }, []);

    // Load history data when period changes (lazy loaded, not on mount)
    useEffect(() => {
        if (selectedPeriod) {
            loadHistory(selectedPeriod);
        }
    }, [selectedPeriod, loadHistory]);

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

    // Helper to get return data based on selected period
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

    // Don't block on isLoading - let cards render immediately with whatever data we have
    // The chart section has its own loading indicator via isHistoryLoading

    return (
        <div className="space-y-6 p-6 animate-fadeIn">
            <div className="grid grid-cols-1 xl:grid-cols-[3fr_1fr] gap-6">
                {/* Left Column: Metrics + Charts + Allocation */}
                <div className="space-y-6">
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

                        {/* Period Gain/Loss */}
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

                            {/* Toggle Buttons */}
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
                            <TimeRangeSelector
                                selectedPeriod={selectedPeriod}
                                onSelectPeriod={setSelectedPeriod}
                            />
                        </div>

                        <div className="h-[300px] w-full">
                            {isHistoryLoading && historyData.length === 0 ? (
                                <div className="h-full flex items-center justify-center text-muted">
                                    <LoadingSpinner />
                                </div>
                            ) : historyData.length === 0 ? (
                                <div className="h-full flex flex-col items-center justify-center text-muted">
                                    <TrendingUp className="h-12 w-12 mb-4 opacity-20" />
                                    <p>No history data available for this period</p>
                                </div>
                            ) : (
                                <ResponsiveContainer width="100%" height="100%">
                                    <AreaChart
                                        data={historyData.filter(d => d.value > 0)}
                                        margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                                        onMouseMove={(e: any) => {
                                            if (e.activePayload && e.activePayload[0]) {
                                                setActiveValue(e.activePayload[0].payload.value);
                                            }
                                        }}
                                        onMouseLeave={() => setActiveValue(null)}
                                    >
                                        <defs>
                                            <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#8884d8" stopOpacity={0.8} />
                                                <stop offset="95%" stopColor="#8884d8" stopOpacity={0} />
                                            </linearGradient>
                                        </defs>
                                        <XAxis
                                            dataKey="date"
                                            tickFormatter={(str) => {
                                                const [year, month, day] = str.split('-').map(Number);
                                                const date = new Date(year, month - 1, day);
                                                return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                                            }}
                                            stroke="#6b7280"
                                            fontSize={12}
                                        />
                                        <YAxis
                                            tickFormatter={(value) => `$${value.toLocaleString('en-US', { notation: 'compact', maximumFractionDigits: 2 })}`}
                                            stroke="#6b7280"
                                            fontSize={12}
                                            domain={['dataMin', 'dataMax']}
                                        />
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#374151" opacity={0.3} />
                                        <Tooltip
                                            contentStyle={{ backgroundColor: '#1f2937', borderColor: '#374151', borderRadius: '0.5rem' }}
                                            itemStyle={{ color: '#e5e7eb' }}
                                            labelStyle={{ color: '#9ca3af' }}
                                            formatter={(value: number) => [`$${value.toLocaleString()}`, 'Value']}
                                            labelFormatter={(label) => {
                                                const [year, month, day] = label.split('-').map(Number);
                                                return new Date(year, month - 1, day).toLocaleDateString('en-US', { dateStyle: 'medium' });
                                            }}
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="value"
                                            stroke="#8884d8"
                                            fillOpacity={1}
                                            fill="url(#colorValue)"
                                            isAnimationActive={false}
                                        />
                                        {activeValue !== null && (
                                            <ReferenceLine
                                                y={activeValue}
                                                stroke="#9ca3af"
                                                strokeDasharray="3 3"
                                                opacity={0.5}
                                            />
                                        )}
                                    </AreaChart>
                                </ResponsiveContainer>
                            )}
                        </div>
                    </div>

                    {/* Bottom Split: Allocation + Top Holdings */}
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

                    {/* Market Heat Map */}
                    <MarketHeatMap />
                </div>

                {/* Right Column: Sandbox + Market + Watchlist */}
                <div className="space-y-6">
                    {/* Sandbox Link */}
                    <Link to="/sandbox" className="block p-6 bg-card rounded-lg shadow-sm border border-border hover:border-blue-500 transition-all group">
                        <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center space-x-2 text-main">
                                <FlaskConical className="h-5 w-5 text-blue-500" />
                                <span className="font-semibold">Sandbox Portfolio</span>
                            </div>
                            <ExternalLink className="h-4 w-4 text-muted group-hover:text-blue-500 transition-colors" />
                        </div>
                        <p className="text-sm text-muted">Test trading strategies with paper money.</p>
                    </Link>

                    {/* Market Overview */}
                    <MarketIndices />

                    {/* Watchlist */}
                    <WatchlistWidget />
                </div>
            </div>
        </div>
    );
};

export default PortfolioOverview;
