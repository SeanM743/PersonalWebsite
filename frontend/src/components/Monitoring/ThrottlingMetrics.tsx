import React, { useState, useEffect } from 'react';
import { Zap, TrendingUp, TrendingDown, AlertTriangle, RefreshCw } from 'lucide-react';
import { monitoringService } from '../../services/monitoringService';
import { useNotification } from '../../contexts/NotificationContext';
import LoadingSpinner from '../UI/LoadingSpinner';

interface ThrottlingData {
  throttlingRate: number;
  totalRequests: number;
  throttledRequests: number;
  byEndpoint: Record<string, {
    total: number;
    throttled: number;
    rate: number;
  }>;
  timeWindow: string;
  timestamp: string;
}

const ThrottlingMetrics: React.FC = () => {
  const [throttlingData, setThrottlingData] = useState<ThrottlingData | null>(null);
  const [timeSeriesData, setTimeSeriesData] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const { error, success } = useNotification();

  useEffect(() => {
    loadThrottlingData();
    loadTimeSeries();

    // Subscribe to real-time throttling updates
    const unsubscribe = monitoringService.subscribeToThrottling((data) => {
      setThrottlingData(data);
    });

    // Refresh time series every 30 seconds
    const interval = setInterval(loadTimeSeries, 30000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadThrottlingData = async () => {
    try {
      setIsLoading(true);
      const response = await monitoringService.getThrottlingMetrics();
      if (response.success) {
        setThrottlingData(response.data);
      }
    } catch (err: any) {
      error('Failed to load throttling metrics', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const loadTimeSeries = async () => {
    try {
      const response = await monitoringService.getThrottlingRateTimeSeries(24);
      if (response.success && response.data) {
        setTimeSeriesData(response.data);
      }
    } catch (err: any) {
      console.error('Failed to load time series:', err);
    }
  };

  const handleReset = async () => {
    try {
      setIsRefreshing(true);
      await monitoringService.resetThrottlingMetrics();
      await loadThrottlingData();
      success('Throttling metrics reset successfully');
    } catch (err: any) {
      error('Failed to reset throttling metrics', err.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  const getThrottlingStatus = (rate: number) => {
    if (rate >= 0.15) return { color: 'red', label: 'Critical', icon: AlertTriangle };
    if (rate >= 0.05) return { color: 'yellow', label: 'Warning', icon: AlertTriangle };
    return { color: 'green', label: 'Normal', icon: Zap };
  };

  const renderMiniChart = (data: any[]) => {
    if (!data.length || !data[0]?.values?.length) {
      return <div className="h-16 flex items-center justify-center text-gray-400">No data</div>;
    }

    const values = data[0].values.map((v: any) => v.value);
    const max = Math.max(...values, 0.01);

    return (
      <div className="h-16 flex items-end space-x-1">
        {values.slice(-30).map((value: number, index: number) => {
          const height = (value / max) * 100;
          const status = getThrottlingStatus(value);
          return (
            <div
              key={index}
              className={`flex-1 bg-${status.color}-500 rounded-t`}
              style={{ height: `${height}%`, minHeight: '2px' }}
            />
          );
        })}
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

  if (!throttlingData) {
    return (
      <div className="text-center py-12 text-gray-500">
        No throttling data available
      </div>
    );
  }

  const status = getThrottlingStatus(throttlingData.throttlingRate);
  const StatusIcon = status.icon;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Throttling Metrics</h2>
        <button
          onClick={handleReset}
          disabled={isRefreshing}
          className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Reset Metrics</span>
        </button>
      </div>

      {/* Overall Status Card */}
      <div className={`bg-${status.color}-50 border-2 border-${status.color}-300 rounded-lg p-6`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className={`p-3 bg-${status.color}-100 rounded-full`}>
              <StatusIcon className={`h-8 w-8 text-${status.color}-600`} />
            </div>
            <div>
              <h3 className="text-2xl font-bold text-gray-900">
                {(throttlingData.throttlingRate * 100).toFixed(2)}%
              </h3>
              <p className="text-sm text-gray-600">Current Throttling Rate</p>
            </div>
          </div>
          <div className={`px-4 py-2 bg-${status.color}-100 rounded-full`}>
            <span className={`text-sm font-semibold text-${status.color}-700`}>
              {status.label}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4 pt-4 border-t border-gray-300">
          <div>
            <p className="text-sm text-gray-600">Total Requests</p>
            <p className="text-2xl font-bold text-gray-900">
              {throttlingData.totalRequests.toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Throttled Requests</p>
            <p className="text-2xl font-bold text-gray-900">
              {throttlingData.throttledRequests.toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Time Window</p>
            <p className="text-2xl font-bold text-gray-900">
              {throttlingData.timeWindow}
            </p>
          </div>
        </div>
      </div>

      {/* Throttling Rate Over Time */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Throttling Rate Trend (Last 24 Hours)
        </h3>
        {renderMiniChart(timeSeriesData)}
        <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
          <span>24h ago</span>
          <span>12h ago</span>
          <span>Now</span>
        </div>
      </div>

      {/* Throttling by Endpoint */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Throttling by Endpoint
        </h3>

        {throttlingData.byEndpoint && Object.keys(throttlingData.byEndpoint).length > 0 ? (
          <div className="space-y-4">
            {Object.entries(throttlingData.byEndpoint)
              .sort(([, a], [, b]) => b.rate - a.rate)
              .map(([endpoint, data]) => {
                const endpointStatus = getThrottlingStatus(data.rate);
                return (
                  <div key={endpoint} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex-1">
                        <h4 className="font-medium text-gray-900">{endpoint}</h4>
                        <p className="text-sm text-gray-600">
                          {data.throttled.toLocaleString()} / {data.total.toLocaleString()} requests throttled
                        </p>
                      </div>
                      <div className={`px-3 py-1 bg-${endpointStatus.color}-100 rounded-full`}>
                        <span className={`text-sm font-semibold text-${endpointStatus.color}-700`}>
                          {(data.rate * 100).toFixed(2)}%
                        </span>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full bg-${endpointStatus.color}-500`}
                        style={{ width: `${Math.min(data.rate * 100, 100)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            No endpoint-specific throttling data available
          </div>
        )}
      </div>

      {/* Throttling Thresholds Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Throttling Thresholds
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex items-center space-x-3">
            <div className="w-4 h-4 bg-green-500 rounded-full" />
            <div>
              <p className="font-medium text-gray-900">Normal</p>
              <p className="text-sm text-gray-600">&lt; 5%</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <div className="w-4 h-4 bg-yellow-500 rounded-full" />
            <div>
              <p className="font-medium text-gray-900">Warning</p>
              <p className="text-sm text-gray-600">5% - 15%</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <div className="w-4 h-4 bg-red-500 rounded-full" />
            <div>
              <p className="font-medium text-gray-900">Critical</p>
              <p className="text-sm text-gray-600">&gt; 15%</p>
            </div>
          </div>
        </div>
      </div>

      {/* Last Updated */}
      <div className="text-sm text-gray-500 text-center">
        Last updated: {new Date(throttlingData.timestamp).toLocaleString()}
      </div>
    </div>
  );
};

export default ThrottlingMetrics;
