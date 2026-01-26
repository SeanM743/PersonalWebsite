import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Activity, Clock, AlertTriangle } from 'lucide-react';
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

const MetricsCharts: React.FC = () => {
  const [timeRange, setTimeRange] = useState<number>(24); // hours
  const [responseTimeData, setResponseTimeData] = useState<MetricData[]>([]);
  const [errorRateData, setErrorRateData] = useState<MetricData[]>([]);
  const [requestRateData, setRequestRateData] = useState<MetricData[]>([]);
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
      
      const [responseTime, errorRate, requestRate] = await Promise.all([
        monitoringService.getResponseTimeTimeSeries(timeRange),
        monitoringService.getErrorRateTimeSeries(timeRange),
        monitoringService.getRequestRateTimeSeries(timeRange),
      ]);

      if (responseTime.success) setResponseTimeData(responseTime.data || []);
      if (errorRate.success) setErrorRateData(errorRate.data || []);
      if (requestRate.success) setRequestRateData(requestRate.data || []);
    } catch (err: any) {
      error('Failed to load metrics', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp * 1000);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const calculateAverage = (data: MetricData[]) => {
    if (!data.length || !data[0]?.values?.length) return 0;
    const values = data[0].values.map(v => v.value);
    return values.reduce((a, b) => a + b, 0) / values.length;
  };

  const calculateTrend = (data: MetricData[]) => {
    if (!data.length || !data[0]?.values?.length || data[0].values.length < 2) return 0;
    const values = data[0].values;
    const recent = values.slice(-10).reduce((a, b) => a + b.value, 0) / 10;
    const previous = values.slice(-20, -10).reduce((a, b) => a + b.value, 0) / 10;
    return ((recent - previous) / previous) * 100;
  };

  const renderMiniChart = (data: MetricData[], color: string) => {
    if (!data.length || !data[0]?.values?.length) {
      return <div className="h-16 flex items-center justify-center text-gray-400">No data</div>;
    }

    const values = data[0].values.map(v => v.value);
    const max = Math.max(...values);
    const min = Math.min(...values);
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

    const values = data[0].values;
    const max = Math.max(...values.map(v => v.value));
    const min = Math.min(...values.map(v => v.value));
    const range = max - min || 1;

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
            <span>{max.toFixed(2)}{unit}</span>
            <span>{((max + min) / 2).toFixed(2)}{unit}</span>
            <span>{min.toFixed(2)}{unit}</span>
          </div>

          {/* Chart area */}
          <div className="ml-12 h-full border-l border-b border-gray-200 relative">
            <svg className="w-full h-full" preserveAspectRatio="none">
              <polyline
                fill="none"
                stroke={color}
                strokeWidth="2"
                points={values.map((point, index) => {
                  const x = (index / (values.length - 1)) * 100;
                  const y = 100 - (((point.value - min) / range) * 100);
                  return `${x}%,${y}%`;
                }).join(' ')}
              />
              <polyline
                fill={color}
                fillOpacity="0.1"
                stroke="none"
                points={`0,100 ${values.map((point, index) => {
                  const x = (index / (values.length - 1)) * 100;
                  const y = 100 - (((point.value - min) / range) * 100);
                  return `${x}%,${y}%`;
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
              {values[values.length - 1].value.toFixed(2)}{unit}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-600">Average</p>
            <p className="text-lg font-semibold text-gray-900">
              {calculateAverage(data).toFixed(2)}{unit}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-600">Peak</p>
            <p className="text-lg font-semibold text-gray-900">
              {max.toFixed(2)}{unit}
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

  const avgResponseTime = calculateAverage(responseTimeData);
  const avgErrorRate = calculateAverage(errorRateData);
  const avgRequestRate = calculateAverage(requestRateData);
  
  const responseTrend = calculateTrend(responseTimeData);
  const errorTrend = calculateTrend(errorRateData);
  const requestTrend = calculateTrend(requestRateData);

  return (
    <div className="space-y-6">
      {/* Header with Time Range Selector */}
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Performance Metrics</h2>
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
        {/* Response Time Card */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <Clock className="h-5 w-5 text-blue-600" />
              <h3 className="font-semibold text-gray-900">Response Time</h3>
            </div>
            <div className={`flex items-center space-x-1 text-sm ${
              responseTrend > 0 ? 'text-red-600' : 'text-green-600'
            }`}>
              {responseTrend > 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
              <span>{Math.abs(responseTrend).toFixed(1)}%</span>
            </div>
          </div>
          <p className="text-3xl font-bold text-gray-900 mb-2">
            {avgResponseTime.toFixed(0)}ms
          </p>
          <p className="text-sm text-gray-600 mb-4">Average response time</p>
          {renderMiniChart(responseTimeData, 'bg-blue-500')}
        </div>

        {/* Error Rate Card */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <AlertTriangle className="h-5 w-5 text-red-600" />
              <h3 className="font-semibold text-gray-900">Error Rate</h3>
            </div>
            <div className={`flex items-center space-x-1 text-sm ${
              errorTrend > 0 ? 'text-red-600' : 'text-green-600'
            }`}>
              {errorTrend > 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
              <span>{Math.abs(errorTrend).toFixed(1)}%</span>
            </div>
          </div>
          <p className="text-3xl font-bold text-gray-900 mb-2">
            {(avgErrorRate * 100).toFixed(2)}%
          </p>
          <p className="text-sm text-gray-600 mb-4">Error percentage</p>
          {renderMiniChart(errorRateData, 'bg-red-500')}
        </div>

        {/* Request Rate Card */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <Activity className="h-5 w-5 text-green-600" />
              <h3 className="font-semibold text-gray-900">Request Rate</h3>
            </div>
            <div className={`flex items-center space-x-1 text-sm ${
              requestTrend > 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {requestTrend > 0 ? <TrendingUp className="h-4 w-4" /> : <TrendingDown className="h-4 w-4" />}
              <span>{Math.abs(requestTrend).toFixed(1)}%</span>
            </div>
          </div>
          <p className="text-3xl font-bold text-gray-900 mb-2">
            {avgRequestRate.toFixed(1)}/s
          </p>
          <p className="text-sm text-gray-600 mb-4">Requests per second</p>
          {renderMiniChart(requestRateData, 'bg-green-500')}
        </div>
      </div>

      {/* Detailed Charts */}
      <div className="space-y-6">
        {renderDetailedChart(responseTimeData, 'Response Time Over Time', 'ms', '#3B82F6')}
        {renderDetailedChart(errorRateData, 'Error Rate Over Time', '%', '#EF4444')}
        {renderDetailedChart(requestRateData, 'Request Rate Over Time', '/s', '#10B981')}
      </div>
    </div>
  );
};

export default MetricsCharts;
