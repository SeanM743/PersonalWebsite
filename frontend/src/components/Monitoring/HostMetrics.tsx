import React, { useState, useEffect } from 'react';
import { Server, Cpu, HardDrive, TrendingUp, TrendingDown } from 'lucide-react';
import { monitoringService } from '../../services/monitoringService';
import { useNotification } from '../../contexts/NotificationContext';
import LoadingSpinner from '../UI/LoadingSpinner';

interface TimeSeriesData {
    timestamp: number;
    value: number;
}

interface MetricData {
    metric: string;
    values: TimeSeriesData[];
}

const HostMetrics: React.FC = () => {
    const [timeRange, setTimeRange] = useState<number>(24); // hours
    const [cpuData, setCpuData] = useState<MetricData[]>([]);
    const [memoryData, setMemoryData] = useState<MetricData[]>([]);
    const [jvmMemoryData, setJvmMemoryData] = useState<MetricData[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const { error } = useNotification();

    useEffect(() => {
        loadMetrics();
        const interval = setInterval(loadMetrics, 30000); // Refresh every 30 seconds
        return () => clearInterval(interval);
    }, [timeRange]);

    const loadMetrics = async () => {
        try {
            setIsLoading(true);
            const end = Math.floor(Date.now() / 1000);
            const start = end - (timeRange * 3600);
            const step = '15s';

            // CPU Utilization Query: 100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
            const cpuQuery = '100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)';

            // Memory Usage Query: 100 * (1 - ((node_memory_MemFree_bytes + node_memory_Buffers_bytes + node_memory_Cached_bytes) / node_memory_MemTotal_bytes))
            const memQuery = '100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))';

            // JVM Memory Utilization
            const jvmQuery = '(sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"})) * 100';

            const [cpuReq, memReq, jvmReq] = await Promise.all([
                monitoringService.prometheusQueryRange(cpuQuery, start, end, step),
                monitoringService.prometheusQueryRange(memQuery, start, end, step),
                monitoringService.prometheusQueryRange(jvmQuery, start, end, step)
            ]);

            if (cpuReq.status === 'success') {
                // Wrap in our internal structure to match other charts
                setCpuData([{ metric: 'cpu_usage', values: extractValues(cpuReq.result) }]);
            }
            if (memReq.status === 'success') {
                setMemoryData([{ metric: 'memory_usage', values: extractValues(memReq.result) }]);
            }
            if (jvmReq.status === 'success') {
                setJvmMemoryData([{ metric: 'jvm_usage', values: extractValues(jvmReq.result) }]);
            }
        } catch (err: any) {
            error('Failed to load host metrics', err.message);
        } finally {
            setIsLoading(false);
        }
    };

    const extractValues = (result: any[]): TimeSeriesData[] => {
        if (!result || result.length === 0 || !result[0].values) return [];
        // Combine multiple instances if needed, or just take the first one (node exporter usually has 1 instance on small setups)
        return result[0].values.map((v: any[]) => ({
            timestamp: v[0],
            value: parseFloat(v[1])
        }));
    }

    const formatTimestamp = (timestamp: number) => {
        const date = new Date(timestamp * 1000);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const calculateAverage = (data: MetricData[]) => {
        if (!data.length || !data[0]?.values?.length) return 0;
        const values = data[0].values.map(v => v.value);
        // Ignore NaNs which Prometheus might return
        const validValues = values.filter(v => !isNaN(v));
        if (validValues.length === 0) return 0;
        return validValues.reduce((a, b) => a + b, 0) / validValues.length;
    };

    const calculateTrend = (data: MetricData[]) => {
        if (!data.length || !data[0]?.values?.length || data[0].values.length < 2) return 0;
        const values = data[0].values.map(v => v.value).filter(v => !isNaN(v));
        if (values.length < 20) return 0;

        const recent = values.slice(-10).reduce((a, b) => a + b, 0) / 10;
        const previous = values.slice(-20, -10).reduce((a, b) => a + b, 0) / 10;
        if (previous === 0) return 0;
        return ((recent - previous) / previous) * 100;
    };

    const renderMiniChart = (data: MetricData[], color: string) => {
        if (!data.length || !data[0]?.values?.length) {
            return <div className="h-16 flex items-center justify-center text-gray-400">No data</div>;
        }

        const values = data[0].values.map(v => v.value).filter(v => !isNaN(v));
        const max = Math.max(...values, 100);
        const min = 0; // Usage metrics generally bottom out at 0
        const range = max - min || 1;

        return (
            <div className="h-16 flex items-end space-x-1">
                {values.slice(-20).map((value, index) => {
                    const height = ((value - min) / range) * 100;
                    return (
                        <div
                            key={index}
                            className={`flex-1 ${color} rounded-t`}
                            style={{ height: `${height}%`, minHeight: '2px' }}
                        />
                    );
                })}
            </div>
        );
    };

    const renderDetailedChart = (data: MetricData[], title: string, unit: string, color: string) => {
        if (!data.length || !data[0]?.values?.length) {
            return (
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
                    <div className="h-64 flex items-center justify-center text-gray-400">
                        No data available
                    </div>
                </div>
            );
        }

        // Filter out NaNs to prevent svg rendering issues
        const values = data[0].values.filter(v => !isNaN(v.value));
        if (values.length === 0) {
            return (
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
                    <div className="h-64 flex items-center justify-center text-gray-400">
                        No valid data points
                    </div>
                </div>
            );
        }

        const max = Math.max(...values.map(v => v.value));
        const min = Math.min(...values.map(v => v.value));
        const range = (max - min) < 1 ? 1 : (max - min); // Prevent divide by zero if completely flat

        return (
            <div className="bg-white rounded-lg shadow-sm p-6">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
                    <div className="text-sm text-gray-600">
                        Last {timeRange}h
                    </div>
                </div>

                {/* Chart */}
                <div className="relative h-64 mb-4">
                    {/* Y-axis labels */}
                    <div className="absolute left-0 top-0 bottom-0 w-12 flex flex-col justify-between text-xs text-gray-500">
                        <span>{max.toFixed(1)}{unit}</span>
                        <span>{((max + min) / 2).toFixed(1)}{unit}</span>
                        <span>{min.toFixed(1)}{unit}</span>
                    </div>

                    {/* Chart area */}
                    <div className="ml-12 h-full border-l border-b border-gray-200 relative">
                        <svg className="w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
                            <polyline
                                fill="none"
                                stroke={color}
                                strokeWidth="2"
                                vectorEffect="non-scaling-stroke"
                                points={values.map((point, index) => {
                                    const x = (index / (values.length - 1)) * 100;
                                    const y = 100 - (((point.value - min) / range) * 100);
                                    return `${x},${y}`;
                                }).join(' ')}
                            />
                            <polygon
                                fill={color}
                                fillOpacity="0.1"
                                stroke="none"
                                points={`0,100 ${values.map((point, index) => {
                                    const x = (index / (values.length - 1)) * 100;
                                    const y = 100 - (((point.value - min) / range) * 100);
                                    return `${x},${y}`;
                                }).join(' ')} 100,100`}
                            />
                        </svg>
                    </div>

                    {/* X-axis labels */}
                    <div className="ml-12 mt-2 flex justify-between text-xs text-gray-500">
                        <span>{formatTimestamp(values[0].timestamp)}</span>
                        <span>{formatTimestamp(values[Math.floor(values.length / 2)].timestamp)}</span>
                        <span>{formatTimestamp(values[values.length - 1].timestamp)}</span>
                    </div>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-3 gap-4 pt-4 border-t border-gray-200">
                    <div>
                        <p className="text-xs text-gray-600">Current</p>
                        <p className="text-lg font-semibold text-gray-900">
                            {values[values.length - 1].value.toFixed(1)}{unit}
                        </p>
                    </div>
                    <div>
                        <p className="text-xs text-gray-600">Average</p>
                        <p className="text-lg font-semibold text-gray-900">
                            {calculateAverage([{ metric: 'avg', values }]).toFixed(1)}{unit}
                        </p>
                    </div>
                    <div>
                        <p className="text-xs text-gray-600">Peak</p>
                        <p className="text-lg font-semibold text-gray-900">
                            {max.toFixed(1)}{unit}
                        </p>
                    </div>
                </div>
            </div>
        );
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <LoadingSpinner size="large" />
            </div>
        );
    }

    const avgCpu = calculateAverage(cpuData);
    const avgMem = calculateAverage(memoryData);
    const avgJvm = calculateAverage(jvmMemoryData);

    const cpuTrend = calculateTrend(cpuData);
    const memTrend = calculateTrend(memoryData);
    const jvmTrend = calculateTrend(jvmMemoryData);

    // We want to extract the last valid value for the cards
    const getCurrentValue = (data: MetricData[]) => {
        if (!data.length || !data[0]?.values?.length) return 0;
        const validPoints = data[0].values.filter(v => !isNaN(v.value));
        if (validPoints.length === 0) return 0;
        return validPoints[validPoints.length - 1].value;
    };

    const currentCpu = getCurrentValue(cpuData);
    const currentMem = getCurrentValue(memoryData);
    const currentJvm = getCurrentValue(jvmMemoryData);

    return (
        <div className="space-y-6">
            {/* Header with Time Range Selector */}
            <div className="flex items-center justify-between">
                <h2 className="text-2xl font-bold text-gray-900">Host Metrics</h2>
                <div className="flex items-center space-x-2">
                    <span className="text-sm text-gray-600">Time Range:</span>
                    <select
                        value={timeRange}
                        onChange={(e) => setTimeRange(Number(e.target.value))}
                        className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                        <option value={1}>Last 1 hour</option>
                        <option value={6}>Last 6 hours</option>
                        <option value={24}>Last 24 hours</option>
                        <option value={168}>Last 7 days</option>
                    </select>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* CPU Card */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-2">
                            <Cpu className="h-5 w-5 text-blue-600" />
                            <h3 className="font-semibold text-gray-900">Host CPU</h3>
                        </div>
                        <div className={`flex items-center space-x-1 text-sm ${cpuTrend > 0 ? 'text-orange-600' : 'text-green-600'
                            }`}>
                            {cpuTrend > 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
                            <span>{Math.abs(cpuTrend).toFixed(1)}%</span>
                        </div>
                    </div>
                    <p className="text-3xl font-bold text-gray-900 mb-2">
                        {currentCpu.toFixed(1)}%
                    </p>
                    <p className="text-sm text-gray-600 mb-4">Current CPU Load</p>
                    {renderMiniChart(cpuData, 'bg-blue-500')}
                </div>

                {/* Memory Card */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-2">
                            <HardDrive className="h-5 w-5 text-indigo-600" />
                            <h3 className="font-semibold text-gray-900">Host Memory</h3>
                        </div>
                        <div className={`flex items-center space-x-1 text-sm ${memTrend > 0 ? 'text-orange-600' : 'text-green-600'
                            }`}>
                            {memTrend > 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
                            <span>{Math.abs(memTrend).toFixed(1)}%</span>
                        </div>
                    </div>
                    <p className="text-3xl font-bold text-gray-900 mb-2">
                        {currentMem.toFixed(1)}%
                    </p>
                    <p className="text-sm text-gray-600 mb-4">System Memory Used</p>
                    {renderMiniChart(memoryData, 'bg-indigo-500')}
                </div>

                {/* JVM Memory Card */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-2">
                            <Server className="h-5 w-5 text-purple-600" />
                            <h3 className="font-semibold text-gray-900">JVM Memory</h3>
                        </div>
                        <div className={`flex items-center space-x-1 text-sm ${jvmTrend > 0 ? 'text-orange-600' : 'text-green-600'
                            }`}>
                            {jvmTrend > 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
                            <span>{Math.abs(jvmTrend).toFixed(1)}%</span>
                        </div>
                    </div>
                    <p className="text-3xl font-bold text-gray-900 mb-2">
                        {currentJvm.toFixed(1)}%
                    </p>
                    <p className="text-sm text-gray-600 mb-4">Backend Heap Used</p>
                    {renderMiniChart(jvmMemoryData, 'bg-purple-500')}
                </div>
            </div>

            {/* Detailed Charts */}
            <div className="space-y-6">
                {renderDetailedChart(cpuData, 'Host CPU Utilization', '%', '#3B82F6')}
                {renderDetailedChart(memoryData, 'Host Memory Usage', '%', '#4F46E5')}
                {renderDetailedChart(jvmMemoryData, 'JVM Memory Utilization', '%', '#9333EA')}
            </div>
        </div>
    );
};

export default HostMetrics;
