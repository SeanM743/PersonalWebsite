import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface User {
  id: number;
  username: string;
  role: 'GUEST' | 'ADMIN';
}

export interface BackendAuthResponse {
  token: string;
  username: string;
  role: 'GUEST' | 'ADMIN';
  type: string;
  message: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

class AuthService {
  private api = axios.create({
    baseURL: `${API_BASE_URL}/api/auth`,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  constructor() {
    // Add token to requests if available
    this.api.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    // Handle token expiration
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('authToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  async login(username: string, password: string): Promise<LoginResponse> {
    try {
      const response = await this.api.post('/login', { username, password });
      const backendResponse: BackendAuthResponse = response.data;
      
      // Transform backend response to frontend format
      const loginResponse: LoginResponse = {
        token: backendResponse.token,
        user: {
          id: 0, // Backend doesn't provide ID, using 0 as placeholder
          username: backendResponse.username,
          role: backendResponse.role
        }
      };
      
      return loginResponse;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Login failed');
    }
  }

  async getCurrentUser(): Promise<User> {
    try {
      const response = await this.api.get('/me');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to get user info');
    }
  }

  logout(): void {
    localStorage.removeItem('authToken');
  }
}

export const authService = new AuthService();