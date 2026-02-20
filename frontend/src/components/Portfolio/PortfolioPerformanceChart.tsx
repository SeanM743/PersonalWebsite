import React, { useState } from 'react';
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
import { TrendingUp } from 'lucide-react';
import TimeRangeSelector from './TimeRangeSelector';
import LoadingSpinner from '../UI/LoadingSpinner';

interface PortfolioPerformanceChartProps {
    selectedPeriod: string;
    onSelectPeriod: (period: string) => void;
    isHistoryLoading: boolean;
    historyData: any[]; // Assuming generic structure from previous code
}

const PortfolioPerformanceChart: React.FC<PortfolioPerformanceChartProps> = ({
    selectedPeriod,
    onSelectPeriod,
    isHistoryLoading,
    historyData
}) => {
    const [activeValue, setActiveValue] = useState<number | null>(null);

    return (
        <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold text-main">Portfolio Performance</h2>
                <TimeRangeSelector
                    selectedPeriod={selectedPeriod}
                    onSelectPeriod={onSelectPeriod}
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
    );
};

export default PortfolioPerformanceChart;
