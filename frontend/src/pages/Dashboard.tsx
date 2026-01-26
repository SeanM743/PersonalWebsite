import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import ErrorBoundary from '../components/ErrorBoundary';
import NowSection from '../components/NowSection';
import LifeLogView from '../components/LifeLogView';
import Timeline from '../components/Timeline';
import DigitalGardenView from '../components/DigitalGardenView';

import {
  Heart,
  MessageCircle,
  Share,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

interface Post {
  id: number;
  title: string;
  content: string;
  platform: string;
  createdAt: string;
  updatedAt: string;
}

interface QuickFact {
  key: string;
  value: string;
  category: string;
  description?: string;
  updatedAt: string;
  externalId?: string;
  imageUrl?: string;
  metadata?: string;
  sourceUrl?: string;
  isEnriched?: boolean;
}

interface ContentResponse<T> {
  data: T;
  success: boolean;
  message?: string;
  metadata?: Record<string, any>;
  timestamp: string;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}



const Dashboard: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [quickFacts, setQuickFacts] = useState<QuickFact[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const { error } = useNotification();

  const loadDashboardData = async () => {
    try {
      setIsLoading(true);

      // Load posts from database
      const postsResponse = await apiService.getPosts(currentPage - 1, 5) as ContentResponse<PageResponse<Post>>;
      if (postsResponse.success) {
        setPosts(postsResponse.data.content || []);
        setTotalPages(postsResponse.data.totalPages || 1);
      }

      // Load quick facts from database
      const quickFactsResponse = await apiService.getQuickFacts() as ContentResponse<QuickFact[]>;
      if (quickFactsResponse.success) {
        setQuickFacts(quickFactsResponse.data || []);
      }

    } catch (err: any) {
      error('Failed to load dashboard data', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickFactsChange = () => {
    loadDashboardData(); // Reload data when quick facts change
  };

  useEffect(() => {
    loadDashboardData();
  }, [currentPage]);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6 bg-gray-50 min-h-screen">
      {/* Digital Command Center - Bento Grid Layout */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 xl:grid-cols-6 gap-4 auto-rows-min">

        {/* Now Section - Quick Facts (spans 2 columns) */}
        <ErrorBoundary>
          <NowSection
            quickFacts={quickFacts}
            onQuickFactsChange={handleQuickFactsChange}
            className="md:col-span-2 lg:col-span-2 xl:col-span-3"
          />
        </ErrorBoundary>

        {/* Life Log Section (spans 2 columns) */}
        <ErrorBoundary>
          <LifeLogView className="md:col-span-2 lg:col-span-2 xl:col-span-3" compact={true} />
        </ErrorBoundary>

        {/* Timeline Section (spans full width on small screens, 3 columns on large screens) */}
        <ErrorBoundary>
          <Timeline className="md:col-span-2 lg:col-span-4 xl:col-span-3" />
        </ErrorBoundary>

        {/* Digital Garden Section (spans 2 columns) */}
        <ErrorBoundary>
          <DigitalGardenView className="md:col-span-2 lg:col-span-2 xl:col-span-3" />
        </ErrorBoundary>



        {/* Legacy Posts Section (spans full width, moved to bottom) */}
        <div className="md:col-span-2 lg:col-span-4 xl:col-span-6 bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-6">Recent Updates</h2>

          {posts.length > 0 ? (
            <div className="space-y-6">
              {posts.map((post) => (
                <div key={post.id} className="border-b border-gray-100 pb-6 last:border-b-0">
                  {/* Post Header */}
                  <div className="flex items-center space-x-3 mb-3">
                    <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white font-medium text-sm">
                      {post.platform ? post.platform.charAt(0).toUpperCase() : 'P'}
                    </div>
                    <div>
                      <div className="font-medium text-gray-900">{post.platform || 'Personal'}</div>
                      <div className="text-sm text-gray-500">
                        {new Date(post.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>

                  {/* Post Content */}
                  <div className="mb-2">
                    <h3 className="font-medium text-gray-900 mb-2">{post.title}</h3>
                    <div className="text-gray-700 leading-relaxed">
                      {post.content}
                    </div>
                  </div>

                  {/* Post Actions */}
                  <div className="flex items-center space-x-6 text-gray-500">
                    <button className="flex items-center space-x-2 hover:text-red-500 transition-colors">
                      <Heart className="h-4 w-4" />
                      <span className="text-sm">Like</span>
                    </button>

                    <button className="flex items-center space-x-2 hover:text-blue-500 transition-colors">
                      <MessageCircle className="h-4 w-4" />
                      <span className="text-sm">Comment</span>
                    </button>

                    <button className="flex items-center space-x-2 hover:text-green-500 transition-colors">
                      <Share className="h-4 w-4" />
                      <span className="text-sm">Share</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              No posts available. Create your first post to get started!
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-100">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                className="flex items-center space-x-2 text-gray-500 hover:text-gray-700 disabled:opacity-50"
              >
                <ChevronLeft className="h-4 w-4" />
                <span>Previous</span>
              </button>

              <span className="text-sm text-gray-500">
                Page {currentPage} of {totalPages}
              </span>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                className="flex items-center space-x-2 text-gray-500 hover:text-gray-700 disabled:opacity-50"
              >
                <span>Next</span>
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;