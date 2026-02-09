import axios, { AxiosInstance } from 'axios';
import { cacheService } from './cacheService';

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

  // Helper method for cached GET requests
  private async cachedGet<T>(url: string, cacheKey: string, ttlMs?: number): Promise<T> {
    // Check cache first
    const cached = cacheService.get<T>(cacheKey);
    if (cached) {
      return cached;
    }

    // Fetch from API
    const response = await this.api.get(url);
    const data = response.data;

    // Cache the response
    cacheService.set(cacheKey, data, ttlMs);

    return data;
  }

  // Portfolio API - with caching
  async getPortfolio(detailed = false) {
    const cacheKey = `portfolio-${detailed}`;
    return this.cachedGet(`/portfolio?detailed=${detailed}`, cacheKey, 5 * 60 * 1000); // 5 minutes
  }

  async getCompletePortfolio(detailed = true) {
    const cacheKey = `complete-portfolio-${detailed}`;
    return this.cachedGet(`/portfolio/complete?detailed=${detailed}`, cacheKey, 5 * 60 * 1000); // 5 minutes
  }

  async refreshPortfolio() {
    // Clear portfolio cache when refreshing
    cacheService.delete('portfolio-true');
    cacheService.delete('portfolio-false');
    cacheService.delete('complete-portfolio-true');
    cacheService.delete('complete-portfolio-false');
    cacheService.delete('holdings');

    const response = await this.api.post('/portfolio/refresh');
    return response.data;
  }

  async getPortfolioPerformance(period = '1d') {
    const cacheKey = `portfolio-performance-${period}`;
    return this.cachedGet(`/portfolio/performance?period=${period}`, cacheKey, 1 * 60 * 1000); // 1 minute
  }

  async getHoldings() {
    // No caching to ensure we trigger backend balance sync
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

  async getTransactions() {
    // No caching for transactions
    const response = await this.api.get('/portfolio/transactions');
    return response.data;
  }

  async addTransaction(transaction: any) {
    // Clear caches when adding transaction
    cacheService.delete('accounts');
    const response = await this.api.post('/portfolio/transactions', transaction);
    return response.data;
  }

  // Account API
  async getAccounts() {
    // No caching for accounts to ensure we get the latest balance updated by getHoldings
    const response = await this.api.get('/accounts');
    return response.data;
  }

  async getAccount(id: number) {
    const response = await this.api.get(`/accounts/${id}`);
    return response.data;
  }

  async createAccount(account: any) {
    cacheService.delete('accounts');
    const response = await this.api.post('/accounts', account);
    return response.data;
  }

  async updateAccount(id: number, account: any) {
    cacheService.delete('accounts');
    const response = await this.api.put(`/accounts/${id}`, account);
    return response.data;
  }

  async deleteAccount(id: number) {
    cacheService.delete('accounts');
    const response = await this.api.delete(`/accounts/${id}`);
    return response.data;
  }

  async updateAccountBalance(id: number, balance: number) {
    cacheService.delete('accounts');
    const response = await this.api.put(`/accounts/${id}/balance`, { balance });
    return response.data;
  }

  // Watchlist API
  async getWatchlist() {
    const response = await this.api.get('/watchlist');
    return response.data;
  }

  async addToWatchlist(symbol: string) {
    const response = await this.api.post('/watchlist', { symbol });
    return response.data;
  }

  async removeFromWatchlist(id: number) {
    const response = await this.api.delete(`/watchlist/${id}`);
    return response.data;
  }

  // Paper Trading API
  async getPaperTransactions() {
    const response = await this.api.get('/paper-trading');
    return response.data;
  }

  async addPaperTransaction(txn: any) {
    const response = await this.api.post('/paper-trading', txn);
    return response.data;
  }

  async resetPaperPortfolio() {
    const response = await this.api.delete('/paper-trading/reset');
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

  // Content API - with caching for read operations
  async getPosts(page = 0, size = 10) {
    const cacheKey = `posts-${page}-${size}`;
    return this.cachedGet(`/content/posts/paginated?page=${page}&size=${size}`, cacheKey, 1 * 60 * 1000); // 1 minute
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
    const cacheKey = 'quick-facts';
    return this.cachedGet('/content/facts', cacheKey, 30 * 1000); // 30 seconds
  }

  async getQuickFactDetails(key: string) {
    const cacheKey = `quick-fact-details-${key}`;
    return this.cachedGet(`/content/facts/${key}/details`, cacheKey, 5 * 60 * 1000); // 5 minutes
  }

  async updateQuickFacts(fact: { key: string; value: string; category?: string }) {
    // Clear cache when updating
    cacheService.delete('quick-facts');
    cacheService.delete(`quick-fact-details-${fact.key}`);

    const response = await this.api.post('/content/facts', fact);
    return response.data;
  }

  async enrichQuickFact(key: string) {
    // Clear cache when enriching
    cacheService.delete('quick-facts');
    cacheService.delete(`quick-fact-details-${key}`);

    const response = await this.api.post(`/content/facts/${key}/enrich`);
    return response.data;
  }

  async deleteQuickFact(key: string) {
    // Clear cache when deleting
    cacheService.delete('quick-facts');
    cacheService.delete(`quick-fact-details-${key}`);

    const response = await this.api.delete(`/content/facts/${key}`);
    return response.data;
  }

  // Cache management
  clearCache(pattern?: string) {
    if (pattern) {
      // Clear specific cache entries matching pattern
      const stats = cacheService.getStats();
      stats.entries.forEach(entry => {
        if (entry.key.includes(pattern)) {
          cacheService.delete(entry.key);
        }
      });
    } else {
      // Clear all cache
      cacheService.clear();
    }
  }

  // Clear cache for a specific key
  clearCacheKey(key: string) {
    cacheService.delete(key);
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
  async sendChatMessage(message: string, sessionId?: string) {
    const response = await this.api.post('/chat', {
      message,
      sessionId: sessionId || 'dashboard-session'
    });
    return response.data;
  }

  async getChatHistory(sessionId?: string) {
    const response = await this.api.get(`/chat/history/${sessionId || 'dashboard-session'}`);
    return response.data;
  }

  // Life Log API
  async getLifeLogEntries(type?: string, status?: string, page = 0, size = 10) {
    const params = new URLSearchParams();
    if (type) params.append('type', type);
    if (status) params.append('status', status);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await this.api.get(`/lifelog/paginated?${params.toString()}`);
    return response.data;
  }

  async getLifeLogEntry(id: number) {
    const response = await this.api.get(`/lifelog/${id}`);
    return response.data;
  }

  async createLifeLogEntry(entry: any) {
    const response = await this.api.post('/lifelog', entry);
    return response.data;
  }

  async updateLifeLogEntry(id: number, entry: any) {
    const response = await this.api.put(`/lifelog/${id}`, entry);
    return response.data;
  }

  async deleteLifeLogEntry(id: number) {
    const response = await this.api.delete(`/lifelog/${id}`);
    return response.data;
  }

  async getLifeLogTimeline() {
    const response = await this.api.get('/lifelog/timeline');
    return response.data;
  }

  async getActiveLifeLogEntries() {
    const response = await this.api.get('/lifelog/active');
    return response.data;
  }

  async searchMetadata(query: string, type: string) {
    const response = await this.api.get(`/lifelog/search-metadata?q=${encodeURIComponent(query)}&type=${type}`);
    return response.data;
  }

  // Digital Garden API
  async getGardenNotes(growthStage?: string) {
    const params = growthStage ? `?growthStage=${growthStage}` : '';
    const response = await this.api.get(`/garden${params}`);
    return response.data;
  }

  async getGardenNote(id: number) {
    const response = await this.api.get(`/garden/${id}`);
    return response.data;
  }

  async createGardenNote(note: any) {
    const response = await this.api.post('/garden', note);
    return response.data;
  }

  async updateGardenNote(id: number, note: any) {
    const response = await this.api.put(`/garden/${id}`, note);
    return response.data;
  }

  async deleteGardenNote(id: number) {
    const response = await this.api.delete(`/garden/${id}`);
    return response.data;
  }

  async linkGardenNoteToLifeLog(noteId: number, lifelogId: number) {
    const response = await this.api.post(`/garden/${noteId}/link/${lifelogId}`);
    return response.data;
  }

  async unlinkGardenNoteFromLifeLog(noteId: number, lifelogId: number) {
    const response = await this.api.delete(`/garden/${noteId}/link/${lifelogId}`);
    return response.data;
  }

  // Life Signals API
  async getBearsTracker() {
    const response = await this.api.get('/signals/bears');
    return response.data;
  }

  async updateBearsTracker(data: any) {
    const response = await this.api.put('/signals/bears', data);
    return response.data;
  }

  async getBerkeleyCountdown() {
    const response = await this.api.get('/signals/countdown');
    return response.data;
  }

  async updateBerkeleyCountdown(date: string) {
    const response = await this.api.put('/signals/countdown/berkeley', date, {
      headers: { 'Content-Type': 'text/plain' }
    });
    return response.data;
  }

  async getNFLDraftCountdown() {
    const response = await this.api.get('/signals/countdown/nfl-draft');
    return response.data;
  }

  async updateNFLDraftCountdown(date: string) {
    const response = await this.api.put('/signals/countdown/nfl-draft', null, {
      params: { date }
    });
    return response.data;
  }

  async getFamilyPulse() {
    const response = await this.api.get('/signals/family');
    return response.data;
  }

  async updateFamilyMember(id: number, member: any) {
    const response = await this.api.put(`/signals/family/${id}`, member);
    return response.data;
  }

  // Portfolio History
  async getPortfolioHistory(period: string = '1M') {
    const cacheKey = `portfolio-history-${period}`;
    // Cache for 5 minutes since history doesn't change rapidly
    return this.cachedGet(`/portfolio/history?period=${period}`, cacheKey, 5 * 60 * 1000);
  }

  async getStockHistory(period: string = '1M') {
    const cacheKey = `stock-history-${period}`;
    return this.cachedGet(`/portfolio/stock-history?period=${period}`, cacheKey, 5 * 60 * 1000);
  }

  // Market Indices API
  async getMarketIndices() {
    try {
      const response = await this.api.get('/portfolio/market-indices');
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Failed to fetch market indices:', error);
      return { success: false, error };
    }
  }

  // Stock Chart Data API
  async getStockChart(symbol: string, period: string = '1M') {
    try {
      const response = await this.api.get(`/portfolio/stock-chart/${symbol}?period=${period}`);
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Failed to fetch stock chart:', error);
      return { success: false, error };
    }
  }
  // News API
  async getNews() {
    const response = await this.api.get('/news');
    return response.data;
  }

  async getCategories() {
    const response = await this.api.get('/news/categories');
    return response.data;
  }

  async addNewsCategory(topic: string, query?: string, tab?: string) {
    const response = await this.api.post('/news/categories', { topic, query, tab });
    return response.data;
  }

  async deleteNewsCategory(id: number) {
    const response = await this.api.delete(`/news/categories/${id}`);
    return response.data;
  }

  async updateNewsCategory(id: number, topic: string, query?: string, tab?: string) {
    const response = await this.api.put(`/news/categories/${id}`, { topic, query, tab });
    return response.data;
  }

  async deleteNewsArticle(id: number) {
    const response = await this.api.delete(`/news/articles/${id}`);
    return response.data;
  }

  async refreshNews(force: boolean = false) {
    const response = await this.api.post(`/news/refresh?force=${force}`);
    return response.data;
  }
}

// Sports Service API (Python/FastAPI on port 8000)
const sportsApi = axios.create({
  baseURL: 'http://localhost:8000/api/sports',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Helper for sports caching
const cachedSportsGet = async <T>(url: string, cacheKey: string, ttlMs: number = 60 * 60 * 1000): Promise<T> => {
  const cached = cacheService.get<T>(cacheKey);
  if (cached) return cached;
  const response = await sportsApi.get(url);
  const data = response.data;
  cacheService.set(cacheKey, data, ttlMs);
  return data;
};

export const sportsService = {
  async getStandings(year: number) {
    return cachedSportsGet(`/standings/${year}`, `sports-standings-${year}`);
  },

  async getBearsSummary(year: number) {
    // 30 min cache for summary as it might update during games
    return cachedSportsGet(`/bears/summary/${year}`, `sports-summary-${year}`, 30 * 60 * 1000);
  },

  async getBearsRoster(year: number) {
    // Long cache for roster
    return cachedSportsGet(`/bears/roster/${year}`, `sports-roster-${year}`, 24 * 60 * 60 * 1000);
  }
};

export const apiService = new ApiService();