import React, { useState, useEffect } from 'react';
import { AlertTriangle, CheckCircle, XCircle, Clock, Bell, BellOff, ExternalLink } from 'lucide-react';
import { monitoringService } from '../../services/monitoringService';
import { useNotification } from '../../contexts/NotificationContext';
import LoadingSpinner from '../UI/LoadingSpinner';

interface Alert {
  id: string;
  name: string;
  severity: 'critical' | 'warning' | 'info';
  status: 'firing' | 'resolved' | 'pending';
  message: string;
  labels: Record<string, string>;
  annotations: Record<string, string>;
  startsAt: string;
  endsAt?: string;
  generatorURL?: string;
}

const AlertsPanel: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [filter, setFilter] = useState<'all' | 'firing' | 'resolved'>('all');
  const [severityFilter, setSeverityFilter] = useState<'all' | 'critical' | 'warning' | 'info'>('all');
  const [isLoading, setIsLoading] = useState(true);
  const { error: showError } = useNotification();

  useEffect(() => {
    loadAlerts();

    // Subscribe to real-time alert updates
    const unsubscribe = monitoringService.subscribeToAlerts((data) => {
      if (data.alerts) {
        setAlerts(data.alerts);
      }
    });

    // Refresh alerts every 30 seconds
    const interval = setInterval(loadAlerts, 30000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const loadAlerts = async () => {
    try {
      setIsLoading(true);
      // Note: This would need a backend endpoint to fetch alerts from Prometheus/AlertManager
      // For now, we'll use mock data structure
      // const response = await monitoringService.getAlerts();
      // if (response.success) {
      //   setAlerts(response.data);
      // }
      
      // Mock data for demonstration
      setAlerts([
        {
          id: '1',
          name: 'HighResponseTime',
          severity: 'warning',
          status: 'firing',
          message: 'Response time p95 is above 2s',
          labels: {
            service: 'api',
            endpoint: '/api/portfolio'
          },
          annotations: {
            summary: 'High response time detected',
            description: 'The p95 response time for /api/portfolio is 2.5s, exceeding the 2s threshold'
          },
          startsAt: new Date(Date.now() - 3600000).toISOString(),
          generatorURL: 'http://localhost:9090/graph?g0.expr=...'
        },
        {
          id: '2',
          name: 'HighErrorRate',
          severity: 'critical',
          status: 'firing',
          message: 'Error rate is above 5%',
          labels: {
            service: 'api',
            endpoint: '/api/calendar'
          },
          annotations: {
            summary: 'Critical error rate detected',
            description: 'The error rate for /api/calendar is 7.2%, exceeding the 5% critical threshold'
          },
          startsAt: new Date(Date.now() - 1800000).toISOString(),
          generatorURL: 'http://localhost:9090/graph?g0.expr=...'
        }
      ]);
    } catch (err: any) {
      showError('Failed to load alerts', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'critical':
        return <XCircle className="h-6 w-6 text-red-600" />;
      case 'warning':
        return <AlertTriangle className="h-6 w-6 text-yellow-600" />;
      case 'info':
        return <Bell className="h-6 w-6 text-blue-600" />;
      default:
        return <Bell className="h-6 w-6 text-gray-600" />;
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'critical':
        return 'border-red-300 bg-red-50';
      case 'warning':
        return 'border-yellow-300 bg-yellow-50';
      case 'info':
        return 'border-blue-300 bg-blue-50';
      default:
        return 'border-gray-300 bg-gray-50';
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'firing':
        return <span className="px-2 py-1 bg-red-100 text-red-700 text-xs font-semibold rounded-full">Firing</span>;
      case 'resolved':
        return <span className="px-2 py-1 bg-green-100 text-green-700 text-xs font-semibold rounded-full">Resolved</span>;
      case 'pending':
        return <span className="px-2 py-1 bg-yellow-100 text-yellow-700 text-xs font-semibold rounded-full">Pending</span>;
      default:
        return <span className="px-2 py-1 bg-gray-100 text-gray-700 text-xs font-semibold rounded-full">Unknown</span>;
    }
  };

  const formatDuration = (startTime: string) => {
    const start = new Date(startTime).getTime();
    const now = Date.now();
    const diff = now - start;
    
    const hours = Math.floor(diff / 3600000);
    const minutes = Math.floor((diff % 3600000) / 60000);
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const filteredAlerts = alerts.filter(alert => {
    if (filter !== 'all' && alert.status !== filter) return false;
    if (severityFilter !== 'all' && alert.severity !== severityFilter) return false;
    return true;
  });

  const firingCount = alerts.filter(a => a.status === 'firing').length;
  const criticalCount = alerts.filter(a => a.severity === 'critical' && a.status === 'firing').length;
  const warningCount = alerts.filter(a => a.severity === 'warning' && a.status === 'firing').length;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">Alerts</h2>
        <a
          href="http://localhost:9093"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          <ExternalLink className="h-4 w-4" />
          <span>Open AlertManager</span>
        </a>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg shadow-sm p-4 border-l-4 border-red-500">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Critical Alerts</p>
              <p className="text-3xl font-bold text-gray-900">{criticalCount}</p>
            </div>
            <XCircle className="h-10 w-10 text-red-500" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-4 border-l-4 border-yellow-500">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Warning Alerts</p>
              <p className="text-3xl font-bold text-gray-900">{warningCount}</p>
            </div>
            <AlertTriangle className="h-10 w-10 text-yellow-500" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-4 border-l-4 border-blue-500">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Firing</p>
              <p className="text-3xl font-bold text-gray-900">{firingCount}</p>
            </div>
            <Bell className="h-10 w-10 text-blue-500" />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center space-x-2">
            <span className="text-sm font-medium text-gray-700">Status:</span>
            <div className="flex space-x-2">
              {['all', 'firing', 'resolved'].map((f) => (
                <button
                  key={f}
                  onClick={() => setFilter(f as any)}
                  className={`px-3 py-1 rounded-lg text-sm font-medium ${
                    filter === f
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {f.charAt(0).toUpperCase() + f.slice(1)}
                </button>
              ))}
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-sm font-medium text-gray-700">Severity:</span>
            <div className="flex space-x-2">
              {['all', 'critical', 'warning', 'info'].map((s) => (
                <button
                  key={s}
                  onClick={() => setSeverityFilter(s as any)}
                  className={`px-3 py-1 rounded-lg text-sm font-medium ${
                    severityFilter === s
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {s.charAt(0).toUpperCase() + s.slice(1)}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Alerts List */}
      <div className="space-y-4">
        {filteredAlerts.length > 0 ? (
          filteredAlerts.map((alert) => (
            <div
              key={alert.id}
              className={`border-2 rounded-lg p-6 ${getSeverityColor(alert.severity)}`}
            >
              {/* Alert Header */}
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-start space-x-3">
                  {getSeverityIcon(alert.severity)}
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900">{alert.name}</h3>
                    <p className="text-sm text-gray-600">{alert.message}</p>
                  </div>
                </div>
                {getStatusBadge(alert.status)}
              </div>

              {/* Alert Details */}
              {alert.annotations && (
                <div className="mb-4 p-3 bg-white bg-opacity-50 rounded">
                  {alert.annotations.summary && (
                    <p className="font-medium text-gray-900 mb-1">{alert.annotations.summary}</p>
                  )}
                  {alert.annotations.description && (
                    <p className="text-sm text-gray-700">{alert.annotations.description}</p>
                  )}
                </div>
              )}

              {/* Labels */}
              {alert.labels && Object.keys(alert.labels).length > 0 && (
                <div className="mb-4">
                  <p className="text-xs font-medium text-gray-700 mb-2">Labels:</p>
                  <div className="flex flex-wrap gap-2">
                    {Object.entries(alert.labels).map(([key, value]) => (
                      <span
                        key={key}
                        className="px-2 py-1 bg-white bg-opacity-70 text-xs font-medium text-gray-700 rounded"
                      >
                        {key}: {value}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Footer */}
              <div className="flex items-center justify-between text-sm text-gray-600">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-1">
                    <Clock className="h-4 w-4" />
                    <span>Started {formatDuration(alert.startsAt)} ago</span>
                  </div>
                  {alert.endsAt && (
                    <div className="flex items-center space-x-1">
                      <CheckCircle className="h-4 w-4" />
                      <span>Resolved {formatDuration(alert.endsAt)} ago</span>
                    </div>
                  )}
                </div>
                {alert.generatorURL && (
                  <a
                    href={alert.generatorURL}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center space-x-1 text-blue-600 hover:text-blue-700"
                  >
                    <span>View in Prometheus</span>
                    <ExternalLink className="h-4 w-4" />
                  </a>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="bg-white rounded-lg shadow-sm p-12 text-center">
            <BellOff className="h-16 w-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No Alerts</h3>
            <p className="text-gray-600">
              {filter === 'all' 
                ? 'No alerts found. Your system is running smoothly!'
                : `No ${filter} alerts found.`}
            </p>
          </div>
        )}
      </div>

      {/* Alert Configuration Info */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Alert Configuration
        </h3>
        <div className="space-y-2 text-sm text-gray-700">
          <p>• Alerts are configured in Prometheus and routed through AlertManager</p>
          <p>• Critical alerts trigger immediate notifications</p>
          <p>• Warning alerts are grouped and sent in batches</p>
          <p>• Configure notification channels in AlertManager: <code className="px-2 py-1 bg-white rounded">monitoring/alertmanager/alertmanager.yml</code></p>
        </div>
      </div>
    </div>
  );
};

export default AlertsPanel;
