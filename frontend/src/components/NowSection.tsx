import React, { useState } from 'react';
import {
  Music,
  MapPin,
  Calendar,
  Zap,
  Smile,
  Circle,
  Edit,
  Plus,
  Trash2,
  Info
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import QuickFactEditModal from './QuickFactEditModal';
import QuickFactMetadataModal from './QuickFactMetadataModal';
import LoadingSpinner from './UI/LoadingSpinner';

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

interface NowSectionProps {
  quickFacts: QuickFact[];
  onQuickFactsChange: () => void;
  className?: string;
}

const NowSection: React.FC<NowSectionProps> = ({ quickFacts, onQuickFactsChange, className = "" }) => {
  const [selectedQuickFact, setSelectedQuickFact] = useState<QuickFact | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingQuickFact, setEditingQuickFact] = useState<QuickFact | null>(null);
  const [isMetadataModalOpen, setIsMetadataModalOpen] = useState(false);
  const [metadataQuickFact, setMetadataQuickFact] = useState<QuickFact | null>(null);
  const { error, success } = useNotification();

  // Icon mapping for quick facts with enhanced categorization
  const getIconForCategory = (category: string, key: string) => {
    const lowerKey = key.toLowerCase();
    const lowerCategory = category.toLowerCase();

    // Work Focus
    if (lowerKey.includes('work') || lowerKey.includes('focus') || lowerKey.includes('project') || lowerCategory.includes('work')) {
      return <Zap className="h-4 w-4" />;
    }
    // Music/Audio
    if (lowerKey.includes('listening') || lowerKey.includes('music') || lowerKey.includes('song') || lowerCategory.includes('music')) {
      return <Music className="h-4 w-4" />;
    }
    // Location
    if (lowerKey.includes('location') || lowerKey.includes('where') || lowerKey.includes('place') || lowerCategory.includes('location')) {
      return <MapPin className="h-4 w-4" />;
    }
    // Calendar/Time
    if (lowerKey.includes('meeting') || lowerKey.includes('next') || lowerKey.includes('schedule') || lowerCategory.includes('calendar')) {
      return <Calendar className="h-4 w-4" />;
    }
    // Energy/Activity
    if (lowerKey.includes('energy') || lowerKey.includes('level') || lowerKey.includes('active') || lowerCategory.includes('energy')) {
      return <Zap className="h-4 w-4" />;
    }
    // Mood/Emotion
    if (lowerKey.includes('mood') || lowerKey.includes('feeling') || lowerCategory.includes('mood')) {
      return <Smile className="h-4 w-4" />;
    }
    // Status
    if (lowerKey.includes('status') || lowerCategory.includes('status')) {
      return <Circle className="h-4 w-4" />;
    }
    return <Circle className="h-4 w-4" />;
  };

  // Enhanced color mapping with more vibrant colors for Bento cards
  const getColorForCategory = (category: string, key: string) => {
    const lowerKey = key.toLowerCase();
    const lowerCategory = category.toLowerCase();

    // Work Focus - Orange/Amber
    if (lowerKey.includes('work') || lowerKey.includes('focus') || lowerKey.includes('project') || lowerCategory.includes('work')) {
      return 'bg-gradient-to-br from-orange-500 to-amber-600';
    }
    // Music/Audio - Blue
    if (lowerKey.includes('listening') || lowerKey.includes('music') || lowerKey.includes('song') || lowerCategory.includes('music')) {
      return 'bg-gradient-to-br from-blue-500 to-indigo-600';
    }
    // Location - Red/Rose
    if (lowerKey.includes('location') || lowerKey.includes('where') || lowerKey.includes('place') || lowerCategory.includes('location')) {
      return 'bg-gradient-to-br from-red-500 to-rose-600';
    }
    // Calendar/Time - Purple
    if (lowerKey.includes('meeting') || lowerKey.includes('next') || lowerKey.includes('schedule') || lowerCategory.includes('calendar')) {
      return 'bg-gradient-to-br from-purple-500 to-violet-600';
    }
    // Energy/Activity - Green
    if (lowerKey.includes('energy') || lowerKey.includes('level') || lowerKey.includes('active') || lowerCategory.includes('energy')) {
      return 'bg-gradient-to-br from-green-500 to-emerald-600';
    }
    // Mood/Emotion - Pink
    if (lowerKey.includes('mood') || lowerKey.includes('feeling') || lowerCategory.includes('mood')) {
      return 'bg-gradient-to-br from-pink-500 to-rose-600';
    }
    // Status - Teal
    if (lowerKey.includes('status') || lowerCategory.includes('status')) {
      return 'bg-gradient-to-br from-teal-500 to-cyan-600';
    }
    return 'bg-gradient-to-br from-gray-500 to-slate-600';
  };

  const handleEditQuickFacts = () => {
    setIsEditModalOpen(true);
    setEditingQuickFact(null); // For adding new fact
  };

  const handleEditQuickFact = (fact: QuickFact) => {
    setEditingQuickFact(fact);
    setIsEditModalOpen(true);
  };

  const handleQuickFactSaved = () => {
    onQuickFactsChange(); // Notify parent to reload data
  };

  const handleDeleteQuickFact = async (fact: QuickFact) => {
    const confirmed = window.confirm(
      `Delete "${fact.key}"?\n\nThis will permanently remove:\n• ${fact.value}\n• Any enriched data (images, metadata)\n\nThis action cannot be undone.`
    );

    if (!confirmed) {
      return;
    }

    try {
      await apiService.deleteQuickFact(fact.key);
      success(`"${fact.key}" deleted successfully`);
      onQuickFactsChange(); // Notify parent to reload data
    } catch (err: any) {
      error('Failed to delete quick fact', err.message);
    }
  };

  const handleQuickFactClick = async (fact: QuickFact) => {
    setSelectedQuickFact(fact);
    setIsModalOpen(true);
    setIsLoadingDetails(true);

    try {
      const response = await apiService.getQuickFactDetails(fact.key) as ContentResponse<QuickFact>;
      if (response.success) {
        setSelectedQuickFact(response.data);
      }
    } catch (err: any) {
      error('Failed to load details', err.message);
    } finally {
      setIsLoadingDetails(false);
    }
  };

  const handleEnrichQuickFact = async (key: string) => {
    try {
      const response = await apiService.enrichQuickFact(key);
      if (response.success) {
        success('Quick fact enriched successfully');
        setSelectedQuickFact(response.data);
        onQuickFactsChange(); // Refresh the list
      }
    } catch (err: any) {
      error('Failed to enrich quick fact', err.message);
    }
  };

  const handleCancelEdit = () => {
    setIsEditModalOpen(false);
    setEditingQuickFact(null);
  };

  const handleShowMetadata = (fact: QuickFact) => {
    setMetadataQuickFact(fact);
    setIsMetadataModalOpen(true);
  };

  const handleCloseMetadata = () => {
    setIsMetadataModalOpen(false);
    setMetadataQuickFact(null);
  };

  return (
    <>
      <div className={`bg-white rounded-xl shadow-sm p-6 ${className}`}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Now</h2>
            <p className="text-sm text-gray-500 mt-1">Current status and focus</p>
          </div>
          <button
            onClick={handleEditQuickFacts}
            className="p-2 text-gray-400 hover:text-blue-600 transition-colors rounded-lg hover:bg-blue-50"
            title="Add new quick fact"
          >
            <Plus className="h-5 w-5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {quickFacts.length > 0 ? (
            quickFacts.map((fact) => (
              <div key={fact.key} className="group">
                <div className="bg-gradient-to-br from-gray-50 to-gray-100 rounded-xl p-5 hover:shadow-lg transition-all duration-300 border border-gray-100 hover:border-gray-200">
                  <div className="flex items-center space-x-3 mb-3">
                    <div className={`w-10 h-10 ${getColorForCategory(fact.category, fact.key)} rounded-xl flex items-center justify-center text-white shadow-sm`}>
                      {getIconForCategory(fact.category, fact.key)}
                    </div>
                    <div className="flex-1">
                      <div className="text-sm font-medium text-gray-600 uppercase tracking-wide">
                        {fact.key}
                      </div>
                    </div>
                  </div>

                  <div
                    className="font-semibold text-gray-900 cursor-pointer hover:text-blue-600 transition-colors text-lg leading-tight mb-3"
                    onClick={() => handleQuickFactClick(fact)}
                  >
                    {fact.value}
                    {fact.isEnriched && (
                      <span className="ml-2 inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                        ✓ Enhanced
                      </span>
                    )}
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="text-xs text-gray-500">
                      Updated {new Date(fact.updatedAt).toLocaleDateString()}
                    </div>
                    <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {fact.isEnriched && fact.metadata && (
                        <button
                          onClick={() => handleShowMetadata(fact)}
                          className="p-1.5 text-gray-400 hover:text-blue-600 transition-colors rounded-md hover:bg-blue-50"
                          title="View metadata"
                        >
                          <Info className="h-3.5 w-3.5" />
                        </button>
                      )}
                      <button
                        onClick={() => handleEditQuickFact(fact)}
                        className="p-1.5 text-gray-400 hover:text-blue-600 transition-colors rounded-md hover:bg-blue-50"
                        title="Edit"
                      >
                        <Edit className="h-3.5 w-3.5" />
                      </button>
                      <button
                        onClick={() => handleDeleteQuickFact(fact)}
                        className="p-1.5 text-gray-400 hover:text-red-600 transition-colors rounded-md hover:bg-red-50"
                        title="Delete"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="col-span-2 text-center py-12">
              <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Circle className="h-8 w-8 text-gray-400" />
              </div>
              <p className="text-gray-500 mb-2">No quick facts available.</p>
              <button
                onClick={handleEditQuickFacts}
                className="text-blue-500 hover:text-blue-600 text-sm font-medium"
              >
                Add your first quick fact
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Quick Fact Edit Modal */}
      <QuickFactEditModal
        isOpen={isEditModalOpen}
        onClose={handleCancelEdit}
        onSave={handleQuickFactSaved}
        quickFact={editingQuickFact || undefined}
        isNew={!editingQuickFact}
      />

      {/* Quick Fact Metadata Modal */}
      {isMetadataModalOpen && metadataQuickFact && (
        <QuickFactMetadataModal
          isOpen={isMetadataModalOpen}
          onClose={handleCloseMetadata}
          quickFact={metadataQuickFact}
        />
      )}

      {/* Quick Fact Details Modal */}
      {isModalOpen && selectedQuickFact && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              {/* Modal Header */}
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-semibold text-gray-900">
                  {selectedQuickFact.key}
                </h2>
                <button
                  onClick={() => setIsModalOpen(false)}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <Plus className="h-6 w-6 rotate-45" />
                </button>
              </div>

              {isLoadingDetails ? (
                <div className="flex items-center justify-center py-8">
                  <LoadingSpinner size="medium" />
                </div>
              ) : (
                <div className="space-y-4">
                  {/* Image */}
                  {selectedQuickFact.imageUrl && (
                    <div className="flex justify-center">
                      <img
                        src={selectedQuickFact.imageUrl}
                        alt={selectedQuickFact.value}
                        className="max-w-48 max-h-64 object-cover rounded-lg shadow-md"
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    </div>
                  )}

                  {/* Basic Info */}
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <span className="text-sm font-medium text-gray-500">Category:</span>
                        <p className="text-gray-900 capitalize">{selectedQuickFact.category}</p>
                      </div>
                      <div>
                        <span className="text-sm font-medium text-gray-500">Value:</span>
                        <p className="text-gray-900">{selectedQuickFact.value}</p>
                      </div>
                    </div>
                  </div>

                  {/* Description */}
                  {selectedQuickFact.description && (
                    <div>
                      <h3 className="text-sm font-medium text-gray-500 mb-2">Description</h3>
                      <p className="text-gray-700 leading-relaxed">{selectedQuickFact.description}</p>
                    </div>
                  )}

                  {/* Rich Metadata */}
                  {selectedQuickFact.metadata && (
                    <div>
                      <h3 className="text-sm font-medium text-gray-500 mb-2">Details</h3>
                      <div className="bg-gray-50 rounded-lg p-4">
                        {(() => {
                          try {
                            const metadata = JSON.parse(selectedQuickFact.metadata);
                            return (
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                {Object.entries(metadata).map(([key, value]) => (
                                  <div key={key}>
                                    <span className="text-sm font-medium text-gray-500 capitalize">
                                      {key.replace(/([A-Z])/g, ' $1').trim()}:
                                    </span>
                                    <p className="text-gray-900">{String(value)}</p>
                                  </div>
                                ))}
                              </div>
                            );
                          } catch {
                            return <p className="text-gray-500">Unable to parse metadata</p>;
                          }
                        })()}
                      </div>
                    </div>
                  )}

                  {/* Actions */}
                  <div className="flex space-x-3 pt-4 border-t border-gray-200">
                    {selectedQuickFact.sourceUrl && (
                      <a
                        href={selectedQuickFact.sourceUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors text-sm"
                      >
                        View Source
                      </a>
                    )}

                    <button
                      onClick={() => handleEnrichQuickFact(selectedQuickFact.key)}
                      className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors text-sm"
                    >
                      Refresh Data
                    </button>
                  </div>

                  {/* Status */}
                  <div className="text-xs text-gray-500 pt-2 border-t border-gray-200">
                    Last updated: {new Date(selectedQuickFact.updatedAt).toLocaleString()}
                    {selectedQuickFact.isEnriched && (
                      <span className="ml-2 text-green-600">• Enhanced with external data</span>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default NowSection;