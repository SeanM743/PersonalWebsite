import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertCircle, XCircle, RefreshCw } from 'lucide-react';
import { monitoringService } from '../../services/monitoringService';
import { useNotification } from '../../contexts/NotificationContext';
import LoadingSpinner from '../UI/LoadingSpinner';

interface ServiceHealth {
  serviceName: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  healthScore: number;
  responseTime: number;
  lastCheckTime: string;
  details?: Record<string, any>;
}

const ServiceHealthOverview: React.FC = () => {
  const [services, setServices] = useState<ServiceHealth[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const { error, success } = useNotification();

  useEffect(() => {
    // Load initial data via HTTP
    loadServices();

    // Setup WebSocket subscription with connection handling
    const setupWebSocket = async () => {
      try {
        await monitoringService.connect();

        // Subscribe to real-time health updates
        const unsub = monitoringService.subscribeToHealth((data) => {
          if (data.services) {
            setServices(data.services);
          }
        });

        return unsub;
      } catch (err) {
        console.error('WebSocket connection failed:', err);
        return () => { };
      }
    };

    let unsubscribe: (() => void) | undefined;
    setupWebSocket().then((unsub) => {
      unsubscribe = unsub;
    });

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, []);

  const loadServices = async () => {
    try {
      setIsLoading(true);
      const response = await monitoringService.getAllServicesHealth();
      if (response.success && response.data) {
        setServices(response.data);
      }
    } catch (err: any) {
      error('Failed to load service health', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setIsRefreshing(true);
      await monitoringService.triggerHealthCheck();
      await loadServices();
      success('Health check completed');
    } catch (err: any) {
      error('Failed to trigger health check', err.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'UP':
        return <CheckCircle className="h-8 w-8 text-green-500" />;
      case 'DEGRADED':
        return <AlertCircle className="h-8 w-8 text-yellow-500" />;
      case 'DOWN':
        return <XCircle className="h-8 w-8 text-red-500" />;
      default:
        return <AlertCircle className="h-8 w-8 text-gray-500" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'UP':
        return 'bg-green-100 border-green-300';
      case 'DEGRADED':
        return 'bg-yellow-100 border-yellow-300';
      case 'DOWN':
        return 'bg-red-100 border-red-300';
      default:
        return 'bg-gray-100 border-gray-300';
    }
  };

  const getHealthScoreColor = (score: number) => {
    if (score >= 90) return 'text-green-600';
    if (score >= 70) return 'text-yellow-600';
    if (score >= 50) return 'text-orange-600';
    return 'text-red-600';
  };

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
        <h2 className="text-2xl font-bold text-gray-900">Service Health Status</h2>
        <button
          onClick={handleRefresh}
          disabled={isRefreshing}
          className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Services Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {services.map((service) => (
          <div
            key={service.serviceName}
            className={`border-2 rounded-lg p-6 ${getStatusColor(service.status)}`}
          >
            {/* Service Header */}
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">{service.serviceName}</h3>
              {getStatusIcon(service.status)}
            </div>

            {/* Health Score */}
            <div className="mb-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-gray-600">Health Score</span>
                <span className={`text-2xl font-bold ${getHealthScoreColor(service.healthScore)}`}>
                  {service.healthScore}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full ${service.healthScore >= 90 ? 'bg-green-500' :
                      service.healthScore >= 70 ? 'bg-yellow-500' :
                        service.healthScore >= 50 ? 'bg-orange-500' : 'bg-red-500'
                    }`}
                  style={{ width: `${service.healthScore}%` }}
                />
              </div>
            </div>

            {/* Response Time */}
            <div className="mb-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Response Time</span>
                <span className="text-sm font-medium text-gray-900">
                  {service.responseTime ? `${service.responseTime.toFixed(0)}ms` : 'N/A'}
                </span>
              </div>
            </div>

            {/* Last Check */}
            <div className="text-xs text-gray-500">
              Last checked: {new Date(service.lastCheckTime).toLocaleString()}
            </div>

            {/* Details */}
            {service.details && Object.keys(service.details).length > 0 && (
              <div className="mt-4 pt-4 border-t border-gray-300">
                <p className="text-xs font-medium text-gray-700 mb-2">Details:</p>
                <div className="space-y-1">
                  {Object.entries(service.details).map(([key, value]) => (
                    <div key={key} className="flex items-center justify-between text-xs">
                      <span className="text-gray-600">{key}:</span>
                      <span className="text-gray-900 font-medium">{String(value)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      {services.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          No services found. Health checks may not be configured.
        </div>
      )}
    </div>
  );
};

export default ServiceHealthOverview;
