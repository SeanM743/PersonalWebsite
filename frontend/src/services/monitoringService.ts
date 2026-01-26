import axios, { AxiosInstance } from 'axios';
import SockJS from 'sockjs-client';
import { Client, Frame, Message } from '@stomp/stompjs';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class MonitoringService {
  private api: AxiosInstance;
  private stompClient: Client | null = null;
  private subscriptions: Map<string, any> = new Map();

  constructor() {
    this.api = axios.create({
      baseURL: `${API_BASE_URL}/api`,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add token to requests
    this.api.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }

  // WebSocket Connection
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.stompClient?.connected) {
        resolve();
        return;
      }

      const socket = new SockJS(`${API_BASE_URL}/ws`);
      this.stompClient = new Client({
        webSocketFactory: () => socket as any,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
          console.log('STOMP:', str);
        },
        onConnect: (frame: Frame) => {
          console.log('Connected to WebSocket:', frame);
          resolve();
        },
        onStompError: (frame: Frame) => {
          console.error('STOMP error:', frame);
          reject(new Error('WebSocket connection failed'));
        },
      });

      this.stompClient.activate();
    });
  }

  disconnect() {
    if (this.stompClient) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  // Subscribe to health updates
  subscribeToHealth(callback: (data: any) => void): () => void {
    if (!this.stompClient?.connected) {
      console.warn('WebSocket not connected');
      return () => {};
    }

    const subscription = this.stompClient.subscribe('/topic/health', (message: Message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set('health', subscription);

    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete('health');
    };
  }

  // Subscribe to throttling updates
  subscribeToThrottling(callback: (data: any) => void): () => void {
    if (!this.stompClient?.connected) {
      console.warn('WebSocket not connected');
      return () => {};
    }

    const subscription = this.stompClient.subscribe('/topic/throttling', (message: Message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set('throttling', subscription);

    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete('throttling');
    };
  }

  // Subscribe to cache updates
  subscribeToCache(callback: (data: any) => void): () => void {
    if (!this.stompClient?.connected) {
      console.warn('WebSocket not connected');
      return () => {};
    }

    const subscription = this.stompClient.subscribe('/topic/cache', (message: Message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set('cache', subscription);

    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete('cache');
    };
  }

  // Subscribe to metrics summary
  subscribeToMetricsSummary(callback: (data: any) => void): () => void {
    if (!this.stompClient?.connected) {
      console.warn('WebSocket not connected');
      return () => {};
    }

    const subscription = this.stompClient.subscribe('/topic/metrics-summary', (message: Message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set('metrics-summary', subscription);

    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete('metrics-summary');
    };
  }

  // Subscribe to alerts
  subscribeToAlerts(callback: (data: any) => void): () => void {
    if (!this.stompClient?.connected) {
      console.warn('WebSocket not connected');
      return () => {};
    }

    const subscription = this.stompClient.subscribe('/topic/alerts', (message: Message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set('alerts', subscription);

    return () => {
      subscription.unsubscribe();
      this.subscriptions.delete('alerts');
    };
  }

  // Health API
  async getOverallHealth() {
    const response = await this.api.get('/monitoring/health');
    return response.data;
  }

  async getAllServicesHealth() {
    const response = await this.api.get('/monitoring/health/services');
    return response.data;
  }

  async getServiceHealth(serviceName: string) {
    const response = await this.api.get(`/monitoring/health/services/${serviceName}`);
    return response.data;
  }

  async triggerHealthCheck() {
    const response = await this.api.post('/monitoring/health/check');
    return response.data;
  }

  // Metrics API
  async getThrottlingMetrics() {
    const response = await this.api.get('/monitoring/metrics/throttling');
    return response.data;
  }

  async getCacheMetrics() {
    const response = await this.api.get('/monitoring/metrics/cache');
    return response.data;
  }

  async getCacheMetricsByName(cacheName: string) {
    const response = await this.api.get(`/monitoring/metrics/cache/${cacheName}`);
    return response.data;
  }

  async getMetricsSummary() {
    const response = await this.api.get('/monitoring/metrics/summary');
    return response.data;
  }

  async resetThrottlingMetrics() {
    const response = await this.api.post('/monitoring/metrics/throttling/reset');
    return response.data;
  }

  async resetCacheMetrics(cacheName: string) {
    const response = await this.api.post(`/monitoring/metrics/cache/${cacheName}/reset`);
    return response.data;
  }

  // Prometheus API
  async prometheusQuery(query: string) {
    const response = await this.api.get('/prometheus/query', {
      params: { query }
    });
    return response.data;
  }

  async prometheusQueryRange(query: string, start: number, end: number, step: string) {
    const response = await this.api.get('/prometheus/query_range', {
      params: { query, start, end, step }
    });
    return response.data;
  }

  async getHealthScoreTimeSeries(hours: number = 24) {
    const response = await this.api.get('/prometheus/metrics/health-score', {
      params: { hours }
    });
    return response.data;
  }

  async getThrottlingRateTimeSeries(hours: number = 24) {
    const response = await this.api.get('/prometheus/metrics/throttling-rate', {
      params: { hours }
    });
    return response.data;
  }

  async getCacheHitRatioTimeSeries(hours: number = 24) {
    const response = await this.api.get('/prometheus/metrics/cache-hit-ratio', {
      params: { hours }
    });
    return response.data;
  }

  async getResponseTimeTimeSeries(hours: number = 24) {
    const response = await this.api.get('/prometheus/metrics/response-time', {
      params: { hours }
    });
    return response.data;
  }

  async getErrorRateTimeSeries(hours: number = 24) {
    const response = await this.api.get('/prometheus/metrics/error-rate', {
      params: { hours }
    });
    return response.data;
  }

  async getRequestRateTimeSeries(hours: number = 24) {
    const response = await this.api.get('/prometheus/metrics/request-rate', {
      params: { hours }
    });
    return response.data;
  }

  async getDashboardData() {
    const response = await this.api.get('/prometheus/dashboard');
    return response.data;
  }

  // Reporting API
  async getSLAReport(startDate: string, endDate: string) {
    const response = await this.api.get('/reports/sla', {
      params: { startDate, endDate }
    });
    return response.data;
  }

  async getPerformanceReport(startDate: string, endDate: string) {
    const response = await this.api.get('/reports/performance', {
      params: { startDate, endDate }
    });
    return response.data;
  }

  async getCacheReport(startDate: string, endDate: string) {
    const response = await this.api.get('/reports/cache', {
      params: { startDate, endDate }
    });
    return response.data;
  }

  async getDailySummary(date: string) {
    const response = await this.api.get('/reports/summary/daily', {
      params: { date }
    });
    return response.data;
  }

  async getWeeklySummary(startDate: string) {
    const response = await this.api.get('/reports/summary/weekly', {
      params: { startDate }
    });
    return response.data;
  }

  async getMonthlySummary(yearMonth: string) {
    const response = await this.api.get('/reports/summary/monthly', {
      params: { yearMonth }
    });
    return response.data;
  }

  async getHistoricalData(metric: string, startDate: string, endDate: string) {
    const response = await this.api.get(`/reports/historical/${metric}`, {
      params: { startDate, endDate }
    });
    return response.data;
  }

  // Export API
  async exportMetricCSV(metric: string, startDate: string, endDate: string) {
    const response = await this.api.get(`/reports/export/${metric}/csv`, {
      params: { startDate, endDate },
      responseType: 'blob'
    });
    return response.data;
  }

  async exportMetricJSON(metric: string, startDate: string, endDate: string) {
    const response = await this.api.get(`/reports/export/${metric}/json`, {
      params: { startDate, endDate }
    });
    return response.data;
  }

  async exportBulkCSV(metrics: string[], startDate: string, endDate: string) {
    const response = await this.api.post('/reports/export/bulk/csv', {
      metrics,
      startDate,
      endDate
    }, {
      responseType: 'blob'
    });
    return response.data;
  }

  async exportReportCSV(reportType: string, startDate: string, endDate: string) {
    const response = await this.api.get(`/reports/export/report/${reportType}/csv`, {
      params: { startDate, endDate },
      responseType: 'blob'
    });
    return response.data;
  }

  async exportReportJSON(reportType: string, startDate: string, endDate: string) {
    const response = await this.api.get(`/reports/export/report/${reportType}/json`, {
      params: { startDate, endDate }
    });
    return response.data;
  }

  // Helper method to download blob as file
  downloadFile(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}

export const monitoringService = new MonitoringService();
