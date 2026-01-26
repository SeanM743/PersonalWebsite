import React, { useState, useEffect } from 'react';
import { Activity, TrendingUp, Database, Zap, AlertTriangle, CheckCircle } from 'lucide-react';
import ServiceHealthOverview from '../components/Monitoring/ServiceHealthOverview';
import MetricsCharts from '../components/Monitoring/MetricsCharts';
import ThrottlingMetrics from '../components/Monitoring/ThrottlingMetrics';
import CacheMetrics from '../components/Monitoring/CacheMetrics';
import AlertsPanel from '../components/Monitoring/AlertsPanel';
import { monitoringService } from '../services/monitoringService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';

const Monitoring: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'performance' | 'throttling' | 'cache' | 'alerts'>('overview');
  const [isLoading, setIsLoading] = useState(true);
  const [summary, setSummary] = useState<any>(null);
  const { error } = useNotification();

  useEffect(() => {
    const initializeMonitoring = async () => {
      try {
        // Connect to WebSocket for real-time updates
        await monitoringService.connect();

        // Subscribe to metrics updates after connection is established
        const unsubscribe = monitoringService.subscribeToMetricsSummary((data) => {
          setSummary(data);
        });

        // Store unsubscribe function for cleanup
        return unsubscribe;
      } catch (err) {
        console.error('Failed to connect to monitoring service:', err);
        // Fallback: still load data via HTTP even if WebSocket fails
        return () => { };
      }
    };

    loadSummary();
    let unsubscribe: (() => void) | undefined;

    initializeMonitoring().then((unsub) => {
      unsubscribe = unsub;
    });

    return () => {
      if (unsubscribe) unsubscribe();
      monitoringService.disconnect();
    };
  }, []);

  const loadSummary = async () => {
    try {
      setIsLoading(true);
      const data = await monitoringService.getMetricsSummary();
      setSummary(data);
    } catch (err: any) {
      error('Failed to load monitoring data', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  const tabs = [
    { id: 'overview', label: 'Overview', icon: Activity },
    { id: 'performance', label: 'Performance', icon: TrendingUp },
    { id: 'throttling', label: 'Throttling', icon: Zap },
    { id: 'cache', label: 'Cache', icon: Database },
    { id: 'alerts', label: 'Alerts', icon: AlertTriangle },
  ];

  return (
    <div className="min-h-screen bg-page">
      <div className="max-w-7xl mx-auto p-6">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-main mb-2">Service Health Dashboard</h1>
          <p className="text-muted">Real-time monitoring and performance metrics</p>
        </div>

        {/* Quick Stats */}
        {summary && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-card rounded-lg shadow-sm p-4 border border-border">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted">Health Score</p>
                  <p className="text-2xl font-bold text-main">{summary.healthScore || 0}</p>
                </div>
                <div className={`p-3 rounded-full ${(summary.healthScore || 0) >= 90 ? 'bg-green-100' :
                  (summary.healthScore || 0) >= 70 ? 'bg-yellow-100' : 'bg-red-100'
                  }`}>
                  <CheckCircle className={`h-6 w-6 ${(summary.healthScore || 0) >= 90 ? 'text-green-600' :
                    (summary.healthScore || 0) >= 70 ? 'text-yellow-600' : 'text-red-600'
                    }`} />
                </div>
              </div>
            </div>

            <div className="bg-card rounded-lg shadow-sm p-4 border border-border">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted">Avg Response Time</p>
                  <p className="text-2xl font-bold text-main">
                    {summary.avgResponseTime ? `${summary.avgResponseTime.toFixed(0)}ms` : 'N/A'}
                  </p>
                </div>
                <div className="p-3 rounded-full bg-blue-100">
                  <TrendingUp className="h-6 w-6 text-blue-600" />
                </div>
              </div>
            </div>

            <div className="bg-card rounded-lg shadow-sm p-4 border border-border">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted">Cache Hit Ratio</p>
                  <p className="text-2xl font-bold text-main">
                    {summary.cacheHitRatio ? `${(summary.cacheHitRatio * 100).toFixed(1)}%` : 'N/A'}
                  </p>
                </div>
                <div className="p-3 rounded-full bg-purple-100">
                  <Database className="h-6 w-6 text-purple-600" />
                </div>
              </div>
            </div>

            <div className="bg-card rounded-lg shadow-sm p-4 border border-border">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted">Throttling Rate</p>
                  <p className="text-2xl font-bold text-main">
                    {summary.throttlingRate ? `${(summary.throttlingRate * 100).toFixed(2)}%` : 'N/A'}
                  </p>
                </div>
                <div className="p-3 rounded-full bg-orange-100">
                  <Zap className="h-6 w-6 text-orange-600" />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tabs */}
        <div className="bg-card rounded-lg shadow-sm mb-6 border border-border">
          <div className="border-b border-border">
            <nav className="flex space-x-8 px-6" aria-label="Tabs">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id as any)}
                    className={`
                      flex items-center space-x-2 py-4 px-1 border-b-2 font-medium text-sm
                      ${activeTab === tab.id
                        ? 'border-primary text-primary'
                        : 'border-transparent text-muted hover:text-main hover:border-border'
                      }
                    `}
                  >
                    <Icon className="h-5 w-5" />
                    <span>{tab.label}</span>
                  </button>
                );
              })}
            </nav>
          </div>
        </div>

        {/* Tab Content */}
        <div className="space-y-6">
          {activeTab === 'overview' && <ServiceHealthOverview />}
          {activeTab === 'performance' && <MetricsCharts />}
          {activeTab === 'throttling' && <ThrottlingMetrics />}
          {activeTab === 'cache' && <CacheMetrics />}
          {activeTab === 'alerts' && <AlertsPanel />}
        </div>
      </div>
    </div>
  );
};

export default Monitoring;
