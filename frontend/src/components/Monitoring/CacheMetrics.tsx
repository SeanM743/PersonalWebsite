import React, { useState, useEffect } from 'react';
import { Database, TrendingUp, TrendingDown, RefreshCw, CheckCircle, XCircle } from 'lucide-react';
import { monitoringService } from '../../services/monitoringService';
import { useNotification } from '../../contexts/NotificationContext';
import LoadingSpinner from '../UI/LoadingSpinner';

interface CacheData {
  cacheName: string;
  hitRatio: number;
  hits: number;
  misses: number;
  evictions: number;
  size: number;
  maxSize: number;
  avgLoadTime: number;
  timestamp: string;
}

const CacheMetrics: React.FC = () => {
  const [cacheData, setCacheData] = useState<CacheData[]>([]);
  const [selectedCache, setSelectedCache] = useState<string | null>(null);
  const [timeSeriesData, setTimeSeriesData] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const { error, success } = useNotification();

  useEffect(() => {
    loadCacheData();
    loadTimeSeries();

    // Subscribe to real-time cache updates
    const unsubscribe = monitoringService.subscribeToCache((data) => {
      if (data.caches) {
        setCacheData(data.caches);
      }
    });

    // Refresh time series every 30 seconds
    const interval = setInterval(loadTimeSeries, 30000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadCacheData = async () => {
    try {
      setIsLoading(true);
      const response = await monitoringService.getCacheMetrics();
      if (response.success && response.data) {
        setCacheData(response.data);
        if (!selectedCache && response.data.length > 0) {
          setSelectedCache(response.data[0].cacheName);
        }
      }
    } catch (err: any) {
      error('Failed to load cache metrics', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const loadTimeSeries = async () => {
    try {
      const response = await monitoringService.getCacheHitRatioTimeSeries(24);
      if (response.success && response.data) {
        setTimeSeriesData(response.data);
      }
    } catch (err: any) {
      console.error('Failed to load time series:', err);
    }
  };

  const handleReset = async (cacheName: string) => {
    try {
      setIsRefreshing(true);
      await monitoringService.resetCacheMetrics(cacheName);
      await loadCacheData();
      success(`Cache metrics for ${cacheName} reset successfully`);
    } catch (err: any) {
      error('Failed to reset cache metrics', err.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  const getCacheStatus = (hitRatio: number) => {
    if (hitRatio >= 0.8) return { color: 'green', label: 'Excellent' };
    if (hitRatio >= 0.6) return { color: 'yellow', label: 'Good' };
    return { color: 'red', label: 'Poor' };
  };

  const renderMiniChart = (data: any[]) => {
    if (!data.length || !data[0]?.values?.length) {
      return <div className="h-16 flex items-center justify-center text-gray-400">No data</div>;
    }

    const values = data[0].values.map((v: any) => v.value);
    const max = Math.max(...values, 1);

    return (
      <div className="h-16 flex items-end space-x-1">
        {values.slice(-30).map((value: number, index: number) => {
          const height = (value / max) * 100;
          const status = getCacheStatus(value);
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

  if (cacheData.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        No cache data available
      </div>
    );
  }

  const totalHits = cacheData.reduce((sum, cache) => sum + cache.hits, 0);
  const totalMisses = cacheData.reduce((sum, cache) => sum + cache.misses, 0);
  const overallHitRatio = totalHits / (totalHits + totalMisses) || 0;
  const overallStatus = getCacheStatus(overallHitRatio);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Cache Performance</h2>
      </div>

      {/* Overall Cache Performance */}
      <div className={`bg-${overallStatus.color}-50 border-2 border-${overallStatus.color}-300 rounded-lg p-6`}>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className={`p-3 bg-${overallStatus.color}-100 rounded-full`}>
              <Database className={`h-8 w-8 text-${overallStatus.color}-600`} />
            </div>
            <div>
              <h3 className="text-2xl font-bold text-gray-900">
                {(overallHitRatio * 100).toFixed(1)}%
              </h3>
              <p className="text-sm text-gray-600">Overall Cache Hit Ratio</p>
            </div>
          </div>
          <div className={`px-4 py-2 bg-${overallStatus.color}-100 rounded-full`}>
            <span className={`text-sm font-semibold text-${overallStatus.color}-700`}>
              {overallStatus.label}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-4 gap-4 pt-4 border-t border-gray-300">
          <div>
            <p className="text-sm text-gray-600">Total Hits</p>
            <p className="text-2xl font-bold text-gray-900">
              {totalHits.toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Total Misses</p>
            <p className="text-2xl font-bold text-gray-900">
              {totalMisses.toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Total Evictions</p>
            <p className="text-2xl font-bold text-gray-900">
              {cacheData.reduce((sum, cache) => sum + cache.evictions, 0).toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Active Caches</p>
            <p className="text-2xl font-bold text-gray-900">
              {cacheData.length}
            </p>
          </div>
        </div>
      </div>

      {/* Cache Hit Ratio Over Time */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Cache Hit Ratio Trend (Last 24 Hours)
        </h3>
        {renderMiniChart(timeSeriesData)}
        <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
          <span>24h ago</span>
          <span>12h ago</span>
          <span>Now</span>
        </div>
      </div>

      {/* Individual Cache Performance */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Cache Performance by Type
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {cacheData.map((cache) => {
            const status = getCacheStatus(cache.hitRatio);
            const utilizationPercent = (cache.size / cache.maxSize) * 100;

            return (
              <div
                key={cache.cacheName}
                className={`border-2 border-${status.color}-300 rounded-lg p-4 bg-${status.color}-50`}
              >
                {/* Cache Header */}
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-semibold text-gray-900">{cache.cacheName}</h4>
                  <button
                    onClick={() => handleReset(cache.cacheName)}
                    disabled={isRefreshing}
                    className="p-1 hover:bg-gray-200 rounded"
                    title="Reset metrics"
                  >
                    <RefreshCw className={`h-4 w-4 text-gray-600 ${isRefreshing ? 'animate-spin' : ''}`} />
                  </button>
                </div>

                {/* Hit Ratio */}
                <div className="mb-3">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-gray-600">Hit Ratio</span>
                    <span className={`text-lg font-bold text-${status.color}-700`}>
                      {(cache.hitRatio * 100).toFixed(1)}%
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full bg-${status.color}-500`}
                      style={{ width: `${cache.hitRatio * 100}%` }}
                    />
                  </div>
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-2 gap-2 mb-3 text-sm">
                  <div className="flex items-center space-x-1">
                    <CheckCircle className="h-4 w-4 text-green-600" />
                    <span className="text-gray-600">Hits:</span>
                    <span className="font-medium text-gray-900">{cache.hits.toLocaleString()}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <XCircle className="h-4 w-4 text-red-600" />
                    <span className="text-gray-600">Misses:</span>
                    <span className="font-medium text-gray-900">{cache.misses.toLocaleString()}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-gray-600">Evictions:</span>
                    <span className="font-medium text-gray-900 ml-1">{cache.evictions.toLocaleString()}</span>
                  </div>
                </div>

                {/* Cache Size */}
                <div className="mb-3">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm text-gray-600">Utilization</span>
                    <span className="text-sm font-medium text-gray-900">
                      {cache.size} / {cache.maxSize}
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full ${
                        utilizationPercent > 90 ? 'bg-red-500' :
                        utilizationPercent > 70 ? 'bg-yellow-500' : 'bg-blue-500'
                      }`}
                      style={{ width: `${Math.min(utilizationPercent, 100)}%` }}
                    />
                  </div>
                </div>

                {/* Avg Load Time */}
                <div className="text-sm">
                  <span className="text-gray-600">Avg Load Time:</span>
                  <span className="font-medium text-gray-900 ml-1">
                    {cache.avgLoadTime ? `${cache.avgLoadTime.toFixed(2)}ms` : 'N/A'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Cache Performance Thresholds */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Cache Performance Thresholds
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex items-center space-x-3">
            <div className="w-4 h-4 bg-green-500 rounded-full" />
            <div>
              <p className="font-medium text-gray-900">Excellent</p>
              <p className="text-sm text-gray-600">&gt; 80% hit ratio</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <div className="w-4 h-4 bg-yellow-500 rounded-full" />
            <div>
              <p className="font-medium text-gray-900">Good</p>
              <p className="text-sm text-gray-600">60% - 80% hit ratio</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <div className="w-4 h-4 bg-red-500 rounded-full" />
            <div>
              <p className="font-medium text-gray-900">Poor</p>
              <p className="text-sm text-gray-600">&lt; 60% hit ratio</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CacheMetrics;
