import React, { useState, useEffect } from 'react';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis, ReferenceLine } from 'recharts';
import { apiService } from '../../services/apiService';
import LoadingSpinner from '../UI/LoadingSpinner';

interface ChartDataPoint {
    date: string;
    price: number;
}

interface StockPerformanceChartProps {
    holdings: Array<{
        symbol: string;
        quantity: number;
        currentValue: number;
        currentPrice?: number;
        dailyChange?: number;
        dailyChangePercentage?: number;
    }>;
    externalSymbol?: string; // Symbol set externally (e.g., from watchlist click)
}

type TimePeriod = '1D' | '1W' | '1M' | '6M' | '1Y';

const TIME_PERIODS: { value: TimePeriod; label: string }[] = [
    { value: '1D', label: '1D' },
    { value: '1W', label: '1W' },
    { value: '1M', label: '1M' },
    { value: '6M', label: '6M' },
    { value: '1Y', label: '1Y' },
];

const StockPerformanceChart: React.FC<StockPerformanceChartProps> = ({ holdings, externalSymbol }) => {
    const [internalSymbol, setInternalSymbol] = useState<string>('');
    const [selectedPeriod, setSelectedPeriod] = useState<TimePeriod>('1M');
    const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Use external symbol if provided, otherwise use internal
    const selectedSymbol = externalSymbol || internalSymbol;

    // Cache key for localStorage
    const getCacheKey = (symbol: string, period: string) =>
        `stock_chart_${symbol}_${period}`;
    const CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    // Set default symbol to first holding (only when no external symbol)
    useEffect(() => {
        if (holdings.length > 0 && !internalSymbol && !externalSymbol) {
            setInternalSymbol(holdings[0].symbol);
        }
    }, [holdings, internalSymbol, externalSymbol]);

    // Load chart data when symbol or period changes
    useEffect(() => {
        if (selectedSymbol) {
            loadChartData();
        }
    }, [selectedSymbol, selectedPeriod]);

    const loadChartData = async () => {
        if (!selectedSymbol) return;

        const cacheKey = getCacheKey(selectedSymbol, selectedPeriod);

        // Check cache first
        const cached = localStorage.getItem(cacheKey);
        if (cached) {
            const { data, timestamp } = JSON.parse(cached);
            if (Date.now() - timestamp < CACHE_DURATION_MS) {
                setChartData(data);
                setError(null);
                return;
            }
        }

        setIsLoading(true);
        setError(null);

        try {
            const response = await apiService.getStockChart(selectedSymbol, selectedPeriod);
            if (response && (response as any).success) {
                const data = (response as any).data || [];
                setChartData(data);
                // Cache the result
                localStorage.setItem(cacheKey, JSON.stringify({
                    data,
                    timestamp: Date.now()
                }));
            } else {
                setError('Failed to load chart data');
            }
        } catch (err) {
            console.error('Failed to load stock chart:', err);
            setError('Failed to load chart data');
            // Try to use cached data even if expired
            if (cached) {
                const { data } = JSON.parse(cached);
                setChartData(data);
                setError(null);
            }
        } finally {
            setIsLoading(false);
        }
    };

    if (holdings.length === 0) {
        return (
            <div className="bg-card rounded-lg p-6 shadow-sm border border-border h-full flex items-center justify-center">
                <p className="text-muted text-sm">No holdings to display</p>
            </div>
        );
    }

    // Determine price change values
    let priceChange = 0;
    let priceChangePercent = 0;
    let previousClose = 0;

    const currentHolding = holdings.find(h => h.symbol === selectedSymbol);

    if (selectedPeriod === '1D' && currentHolding?.dailyChange !== undefined) {
        // Correct 1D logic: use dailyChange from holding data
        priceChange = currentHolding.dailyChange;
        priceChangePercent = currentHolding.dailyChangePercentage || 0;
        previousClose = (currentHolding.currentPrice || 0) - priceChange;
    } else if (chartData.length >= 2) {
        // Standard chart logic for other periods
        priceChange = (chartData[chartData.length - 1]?.price || 0) - (chartData[0]?.price || 0);
        priceChangePercent = (chartData[0]?.price || 0) > 0
            ? ((priceChange / (chartData[0]?.price || 1)) * 100)
            : 0;
    }

    const isPositive = priceChange >= 0;

    return (
        <div className="bg-card rounded-lg p-4 shadow-sm border border-border h-full flex flex-col">
            {/* Header with stock selector */}
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center space-x-2">
                    <select
                        value={selectedSymbol}
                        onChange={(e) => setInternalSymbol(e.target.value)}
                        className="bg-page border border-border rounded px-2 py-1 text-sm text-main font-medium"
                    >
                        {holdings.map((h) => (
                            <option key={h.symbol} value={h.symbol}>
                                {h.symbol}
                            </option>
                        ))}
                    </select>
                    {(chartData.length > 0) && (
                        <span className={`text-sm font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                            {isPositive ? '+' : ''}{priceChangePercent.toFixed(2)}%
                        </span>
                    )}
                </div>

                {/* Time period selector */}
                <div className="flex space-x-1">
                    {TIME_PERIODS.map((period) => (
                        <button
                            key={period.value}
                            onClick={() => setSelectedPeriod(period.value)}
                            className={`px-2 py-1 text-xs font-medium rounded transition-colors ${selectedPeriod === period.value
                                ? 'bg-primary text-white'
                                : 'text-muted hover:text-main hover:bg-muted/20'
                                }`}
                        >
                            {period.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Chart area */}
            <div className="flex-1 min-h-[200px]">
                {isLoading ? (
                    <div className="h-full flex items-center justify-center">
                        <LoadingSpinner size="medium" />
                    </div>
                ) : error ? (
                    <div className="h-full flex items-center justify-center text-muted text-sm">
                        {error}
                    </div>
                ) : chartData.length === 0 ? (
                    <div className="h-full flex items-center justify-center text-muted text-sm">
                        No data available
                    </div>
                ) : (
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={chartData} margin={{ top: 5, right: 5, left: 0, bottom: 5 }}>
                            <defs>
                                <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor={isPositive ? '#22c55e' : '#ef4444'} stopOpacity={0.3} />
                                    <stop offset="95%" stopColor={isPositive ? '#22c55e' : '#ef4444'} stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <XAxis
                                dataKey="date"
                                tick={{ fontSize: 10 }}
                                tickFormatter={(str) => {
                                    // Safety check
                                    if (!str || typeof str !== 'string') return '';

                                    if (selectedPeriod === '1D') {
                                        // Expecting "YYYY-MM-DD HH:mm"
                                        const parts = str.split(' ');
                                        return parts.length > 1 ? parts[1] : str;
                                    }
                                    try {
                                        const date = new Date(str);
                                        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                                    } catch (e) {
                                        return str;
                                    }
                                }}
                                stroke="#9ca3af"
                                tickMargin={5}
                                minTickGap={20}
                            />
                            <YAxis
                                tick={{ fontSize: 10 }}
                                tickFormatter={(value) => `$${value.toFixed(0)}`}
                                stroke="#9ca3af"
                                domain={['auto', 'auto']}
                                width={45}
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: '#1f2937',
                                    borderColor: '#374151',
                                    borderRadius: '0.5rem',
                                }}
                                itemStyle={{ color: '#e5e7eb' }}
                                labelStyle={{ color: '#9ca3af' }}
                                formatter={(value: number) => [`$${value.toFixed(2)}`, 'Price']}
                            />
                            <Area
                                type="monotone"
                                dataKey="price"
                                stroke={isPositive ? '#22c55e' : '#ef4444'}
                                strokeWidth={2}
                                fill="url(#chartGradient)"
                            />
                            {selectedPeriod === '1D' && previousClose > 0 && (
                                <ReferenceLine
                                    y={previousClose}
                                    stroke="#9ca3af"
                                    strokeDasharray="3 3"
                                    opacity={0.5}
                                    label={{
                                        value: 'Prev Close',
                                        position: 'right',
                                        fill: '#9ca3af',
                                        fontSize: 10
                                    }}
                                />
                            )}
                        </AreaChart>
                    </ResponsiveContainer>
                )}
            </div>
        </div>
    );
};

export default StockPerformanceChart;
