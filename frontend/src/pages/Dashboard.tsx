import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  Calendar,
  FileText,
  MessageSquare,
  RefreshCw
} from 'lucide-react';

interface DashboardData {
  portfolio?: any;
  upcomingEvents?: any[];
  recentPosts?: any[];
  quickFacts?: any;
}

const Dashboard: React.FC = () => {
  const [data, setData] = useState<DashboardData>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const { error, success } = useNotification();

  const loadDashboardData = async (showRefreshMessage = false) => {
    try {
      setIsRefreshing(true);
      
      const [portfolioRes, eventsRes, postsRes, factsRes] = await Promise.allSettled([
        apiService.getPortfolio(),
        apiService.getCalendarEvents(),
        apiService.getPosts(0, 5),
        apiService.getQuickFacts(),
      ]);

      const newData: DashboardData = {};

      if (portfolioRes.status === 'fulfilled') {
        newData.portfolio = portfolioRes.value.data;
      }
      if (eventsRes.status === 'fulfilled') {
        newData.upcomingEvents = eventsRes.value.data?.slice(0, 5) || [];
      }
      if (postsRes.status === 'fulfilled') {
        newData.recentPosts = postsRes.value.data?.content || [];
      }
      if (factsRes.status === 'fulfilled') {
        newData.quickFacts = factsRes.value.data;
      }

      setData(newData);
      
      if (showRefreshMessage) {
        success('Dashboard refreshed successfully');
      }
    } catch (err: any) {
      error('Failed to load dashboard', err.message);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  const handleRefresh = () => {
    loadDashboardData(true);
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
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <button
          onClick={handleRefresh}
          disabled={isRefreshing}
          className="btn-secondary flex items-center space-x-2"
        >
          <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Portfolio Summary */}
      {data.portfolio && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Portfolio Overview</h2>
            <TrendingUp className="h-5 w-5 text-green-500" />
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-gray-900">
                ${data.portfolio.currentValue?.toFixed(2) || '0.00'}
              </div>
              <div className="text-sm text-gray-500">Current Value</div>
            </div>
            
            <div className="text-center">
              <div className={`text-2xl font-bold ${
                (data.portfolio.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {(data.portfolio.totalGainLoss || 0) >= 0 ? '+' : ''}
                ${data.portfolio.totalGainLoss?.toFixed(2) || '0.00'}
              </div>
              <div className="text-sm text-gray-500">Total Gain/Loss</div>
            </div>
            
            <div className="text-center">
              <div className="text-2xl font-bold text-gray-900">
                {data.portfolio.totalPositions || 0}
              </div>
              <div className="text-sm text-gray-500">Positions</div>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Upcoming Events */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Upcoming Events</h2>
            <Calendar className="h-5 w-5 text-blue-500" />
          </div>
          
          {data.upcomingEvents && data.upcomingEvents.length > 0 ? (
            <div className="space-y-3">
              {data.upcomingEvents.map((event, index) => (
                <div key={index} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                  <div className="flex-1">
                    <div className="font-medium text-gray-900">{event.title}</div>
                    <div className="text-sm text-gray-500">
                      {new Date(event.startTime).toLocaleDateString()}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              No upcoming events
            </div>
          )}
        </div>

        {/* Recent Posts */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Recent Posts</h2>
            <FileText className="h-5 w-5 text-purple-500" />
          </div>
          
          {data.recentPosts && data.recentPosts.length > 0 ? (
            <div className="space-y-3">
              {data.recentPosts.map((post) => (
                <div key={post.id} className="p-3 bg-gray-50 rounded-lg">
                  <div className="font-medium text-gray-900">{post.title}</div>
                  <div className="text-sm text-gray-500 mt-1">
                    {new Date(post.createdAt).toLocaleDateString()}
                  </div>
                  {post.content && (
                    <div className="text-sm text-gray-600 mt-2 line-clamp-2">
                      {post.content.substring(0, 100)}...
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              No recent posts
            </div>
          )}
        </div>
      </div>

      {/* Quick Facts */}
      {data.quickFacts && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Quick Facts</h2>
            <MessageSquare className="h-5 w-5 text-green-500" />
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Object.entries(data.quickFacts).map(([key, value]) => (
              <div key={key} className="p-3 bg-gray-50 rounded-lg">
                <div className="font-medium text-gray-900 capitalize">
                  {key.replace(/([A-Z])/g, ' $1').trim()}
                </div>
                <div className="text-sm text-gray-600 mt-1">
                  {String(value)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Quick Actions */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <button className="btn-secondary flex flex-col items-center space-y-2 p-4">
            <TrendingUp className="h-6 w-6" />
            <span className="text-sm">Add Stock</span>
          </button>
          <button className="btn-secondary flex flex-col items-center space-y-2 p-4">
            <Calendar className="h-6 w-6" />
            <span className="text-sm">New Event</span>
          </button>
          <button className="btn-secondary flex flex-col items-center space-y-2 p-4">
            <FileText className="h-6 w-6" />
            <span className="text-sm">Create Post</span>
          </button>
          <button className="btn-secondary flex flex-col items-center space-y-2 p-4">
            <MessageSquare className="h-6 w-6" />
            <span className="text-sm">Ask AI</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;