import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { Plus, FileText, Book, Film, Music, MapPin } from 'lucide-react';

interface Post {
  id: number;
  title: string;
  content: string;
  imageUrl?: string;
  createdAt: string;
}

interface MediaActivity {
  id: number;
  type: string;
  title: string;
  status: string;
  rating?: number;
  notes?: string;
  createdAt: string;
}

const Content: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [mediaActivities, setMediaActivities] = useState<MediaActivity[]>([]);
  const [quickFacts, setQuickFacts] = useState<any>({});
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'posts' | 'media' | 'facts'>('posts');
  const { error, success } = useNotification();

  const loadContentData = async () => {
    try {
      setIsLoading(true);
      
      const [postsRes, mediaRes, factsRes] = await Promise.allSettled([
        apiService.getPosts(),
        apiService.getMediaActivities(),
        apiService.getQuickFacts(),
      ]);

      if (postsRes.status === 'fulfilled' && postsRes.value.success) {
        setPosts(postsRes.value.data.content || []);
      }
      
      if (mediaRes.status === 'fulfilled' && mediaRes.value.success) {
        setMediaActivities(mediaRes.value.data || []);
      }
      
      if (factsRes.status === 'fulfilled' && factsRes.value.success) {
        setQuickFacts(factsRes.value.data || {});
      }
    } catch (err: any) {
      error('Failed to load content', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadContentData();
  }, []);

  const getMediaIcon = (type: string) => {
    switch (type.toLowerCase()) {
      case 'book':
        return <Book className="h-5 w-5" />;
      case 'movie':
      case 'tv_show':
        return <Film className="h-5 w-5" />;
      case 'music':
      case 'podcast':
        return <Music className="h-5 w-5" />;
      case 'trip':
        return <MapPin className="h-5 w-5" />;
      default:
        return <FileText className="h-5 w-5" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'completed':
      case 'finished':
        return 'bg-green-100 text-green-800';
      case 'in_progress':
      case 'reading':
      case 'watching':
        return 'bg-blue-100 text-blue-800';
      case 'planned':
      case 'want_to_read':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Content</h1>
        <button className="btn-primary flex items-center space-x-2">
          <Plus className="h-4 w-4" />
          <span>Create New</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {[
            { key: 'posts', label: 'Posts', count: posts.length },
            { key: 'media', label: 'Media & Activities', count: mediaActivities.length },
            { key: 'facts', label: 'Quick Facts', count: Object.keys(quickFacts).length },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.key
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
              {tab.count > 0 && (
                <span className="ml-2 bg-gray-100 text-gray-900 py-0.5 px-2 rounded-full text-xs">
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {/* Posts Tab */}
      {activeTab === 'posts' && (
        <div className="space-y-4">
          {posts.length > 0 ? (
            posts.map((post) => (
              <div key={post.id} className="card">
                <div className="flex items-start space-x-4">
                  {post.imageUrl && (
                    <img
                      src={post.imageUrl}
                      alt={post.title}
                      className="w-16 h-16 rounded-lg object-cover"
                    />
                  )}
                  <div className="flex-1">
                    <h3 className="font-medium text-gray-900">{post.title}</h3>
                    <p className="text-sm text-gray-600 mt-1 line-clamp-3">
                      {post.content}
                    </p>
                    <div className="text-xs text-gray-500 mt-2">
                      {new Date(post.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-12">
              <FileText className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No posts</h3>
              <p className="mt-1 text-sm text-gray-500">
                Get started by creating your first post.
              </p>
            </div>
          )}
        </div>
      )}

      {/* Media & Activities Tab */}
      {activeTab === 'media' && (
        <div className="space-y-4">
          {mediaActivities.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {mediaActivities.map((activity) => (
                <div key={activity.id} className="card">
                  <div className="flex items-start space-x-3">
                    <div className="text-gray-400">
                      {getMediaIcon(activity.type)}
                    </div>
                    <div className="flex-1">
                      <h3 className="font-medium text-gray-900">{activity.title}</h3>
                      <div className="flex items-center space-x-2 mt-1">
                        <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(activity.status)}`}>
                          {activity.status.replace('_', ' ')}
                        </span>
                        <span className="text-xs text-gray-500 capitalize">
                          {activity.type.replace('_', ' ')}
                        </span>
                      </div>
                      {activity.rating && (
                        <div className="text-sm text-gray-600 mt-1">
                          Rating: {activity.rating}/5
                        </div>
                      )}
                      {activity.notes && (
                        <p className="text-sm text-gray-600 mt-2 line-clamp-2">
                          {activity.notes}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <Book className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No activities</h3>
              <p className="mt-1 text-sm text-gray-500">
                Start tracking your books, movies, music, and trips.
              </p>
            </div>
          )}
        </div>
      )}

      {/* Quick Facts Tab */}
      {activeTab === 'facts' && (
        <div className="card">
          {Object.keys(quickFacts).length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {Object.entries(quickFacts).map(([key, value]) => (
                <div key={key} className="p-4 bg-gray-50 rounded-lg">
                  <div className="font-medium text-gray-900 capitalize">
                    {key.replace(/([A-Z])/g, ' $1').trim()}
                  </div>
                  <div className="text-sm text-gray-600 mt-1">
                    {String(value)}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <FileText className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No quick facts</h3>
              <p className="mt-1 text-sm text-gray-500">
                Add some quick facts about yourself.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Content;