import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiService {
  private api: AxiosInstance;

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

    // Handle errors
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('authToken');
          window.location.href = '/';
        }
        return Promise.reject(error);
      }
    );
  }

  // Portfolio API
  async getPortfolio(detailed = false) {
    const response = await this.api.get(`/portfolio?detailed=${detailed}`);
    return response.data;
  }

  async refreshPortfolio() {
    const response = await this.api.post('/portfolio/refresh');
    return response.data;
  }

  async getPortfolioPerformance(period = '1d') {
    const response = await this.api.get(`/portfolio/performance?period=${period}`);
    return response.data;
  }

  async getHoldings() {
    const response = await this.api.get('/portfolio/holdings');
    return response.data;
  }

  async addHolding(holding: any) {
    const response = await this.api.post('/portfolio/holdings', holding);
    return response.data;
  }

  async updateHolding(symbol: string, holding: any) {
    const response = await this.api.put(`/portfolio/holdings/${symbol}`, holding);
    return response.data;
  }

  async deleteHolding(symbol: string) {
    const response = await this.api.delete(`/portfolio/holdings/${symbol}`);
    return response.data;
  }

  async getPortfolioStatistics() {
    const response = await this.api.get('/portfolio/statistics');
    return response.data;
  }

  // Calendar API
  async getCalendarEvents(startDate?: string, endDate?: string) {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    const response = await this.api.get(`/calendar/events?${params.toString()}`);
    return response.data;
  }

  async createCalendarEvent(event: any) {
    const response = await this.api.post('/calendar/events', event);
    return response.data;
  }

  async updateCalendarEvent(eventId: string, event: any) {
    const response = await this.api.put(`/calendar/events/${eventId}`, event);
    return response.data;
  }

  async deleteCalendarEvent(eventId: string) {
    const response = await this.api.delete(`/calendar/events/${eventId}`);
    return response.data;
  }

  // Content API
  async getPosts(page = 0, size = 10) {
    const response = await this.api.get(`/content/posts?page=${page}&size=${size}`);
    return response.data;
  }

  async createPost(post: any) {
    const response = await this.api.post('/content/posts', post);
    return response.data;
  }

  async updatePost(postId: number, post: any) {
    const response = await this.api.put(`/content/posts/${postId}`, post);
    return response.data;
  }

  async deletePost(postId: number) {
    const response = await this.api.delete(`/content/posts/${postId}`);
    return response.data;
  }

  async getQuickFacts() {
    const response = await this.api.get('/content/quick-facts');
    return response.data;
  }

  async updateQuickFacts(facts: any) {
    const response = await this.api.put('/content/quick-facts', facts);
    return response.data;
  }

  async getMediaActivities(type?: string) {
    const params = type ? `?type=${type}` : '';
    const response = await this.api.get(`/content/media-activities${params}`);
    return response.data;
  }

  async addMediaActivity(activity: any) {
    const response = await this.api.post('/content/media-activities', activity);
    return response.data;
  }

  async updateMediaActivity(activityId: number, activity: any) {
    const response = await this.api.put(`/content/media-activities/${activityId}`, activity);
    return response.data;
  }

  async deleteMediaActivity(activityId: number) {
    const response = await this.api.delete(`/content/media-activities/${activityId}`);
    return response.data;
  }

  // Chat API
  async sendChatMessage(message: string) {
    const response = await this.api.post('/chat', { message });
    return response.data;
  }

  async getChatHistory() {
    const response = await this.api.get('/chat/history');
    return response.data;
  }
}

export const apiService = new ApiService();