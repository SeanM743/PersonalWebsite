import React, { useState, useEffect } from 'react';
import { X, Save, Loader, Book, Film, Music, MapPin, Calendar } from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
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

interface QuickFactEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: () => void;
  quickFact?: QuickFact;
  isNew?: boolean;
}

const QuickFactEditModal: React.FC<QuickFactEditModalProps> = ({
  isOpen,
  onClose,
  onSave,
  quickFact,
  isNew = false
}) => {
  const [formData, setFormData] = useState({
    key: '',
    value: '',
    category: 'general'
  });
  const [isSaving, setIsSaving] = useState(false);
  const [isEnriching, setIsEnriching] = useState(false);
  const [previewData, setPreviewData] = useState<QuickFact | null>(null);
  const { error, success } = useNotification();

  useEffect(() => {
    if (quickFact) {
      setFormData({
        key: quickFact.key,
        value: quickFact.value,
        category: quickFact.category
      });
      setPreviewData(quickFact);
    } else {
      setFormData({
        key: '',
        value: '',
        category: 'general'
      });
      setPreviewData(null);
    }
  }, [quickFact, isOpen]);

  const categories = [
    { value: 'reading', label: 'Reading', icon: Book, placeholder: 'e.g., "Ender\'s Game" or "The Great Gatsby by F. Scott Fitzgerald"' },
    { value: 'recently watched', label: 'Recently Watched', icon: Film, placeholder: 'e.g., "Inception"' },
    { value: 'listening to', label: 'Listening To', icon: Music, placeholder: 'e.g., "Bohemian Rhapsody by Queen"' },
    { value: 'next trip', label: 'Next Trip', icon: MapPin, placeholder: 'e.g., "Paris, France"' },
    { value: 'location', label: 'Current Location', icon: MapPin, placeholder: 'e.g., "San Francisco, CA"' },
    { value: 'general', label: 'General', icon: Calendar, placeholder: 'Enter any status or information' }
  ];

  const selectedCategory = categories.find(cat => cat.value === formData.category) || categories[0];

  const handleSave = async () => {
    if (!formData.key.trim() || !formData.value.trim()) {
      error('Validation Error', 'Please fill in both key and value fields');
      return;
    }

    setIsSaving(true);
    try {
      await apiService.updateQuickFacts({
        key: formData.key.trim(),
        value: formData.value.trim(),
        category: formData.category
      });

      success('Quick fact saved successfully');
      onSave();
      onClose();
    } catch (err: any) {
      error('Failed to save quick fact', err.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handlePreview = async () => {
    if (!formData.key.trim() || !formData.value.trim()) {
      error('Validation Error', 'Please fill in both key and value fields to preview');
      return;
    }

    setIsEnriching(true);
    try {
      // Clear cache for this key to ensure fresh data
      apiService.clearCacheKey(`quick-fact-details-${formData.key.trim()}`);
      apiService.clearCacheKey('quick-facts');
      
      // First save the fact with new data
      await apiService.updateQuickFacts({
        key: formData.key.trim(),
        value: formData.value.trim(),
        category: formData.category
      });

      // Force enrichment of the updated fact
      const enrichResponse = await apiService.enrichQuickFact(formData.key.trim());
      if (enrichResponse.success) {
        setPreviewData(enrichResponse.data);
        success('Preview updated with enriched data');
      } else {
        // If enrichment fails, get the basic saved data
        const response = await apiService.getQuickFactDetails(formData.key.trim());
        if (response.success) {
          setPreviewData(response.data);
        }
      }
    } catch (err: any) {
      error('Failed to generate preview', err.message);
    } finally {
      setIsEnriching(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900">
              {isNew ? 'Add New Quick Fact' : 'Edit Quick Fact'}
            </h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Form Section */}
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Key/Label
                </label>
                <input
                  type="text"
                  value={formData.key}
                  onChange={(e) => setFormData(prev => ({ ...prev, key: e.target.value }))}
                  placeholder="e.g., Currently Reading, Recently Watched"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  disabled={!isNew} // Don't allow editing key for existing facts
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Category
                </label>
                <select
                  value={formData.category}
                  onChange={(e) => setFormData(prev => ({ ...prev, category: e.target.value }))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {categories.map(category => (
                    <option key={category.value} value={category.value}>
                      {category.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  <div className="flex items-center space-x-2">
                    <selectedCategory.icon className="h-4 w-4" />
                    <span>Value</span>
                  </div>
                </label>
                <textarea
                  value={formData.value}
                  onChange={(e) => setFormData(prev => ({ ...prev, value: e.target.value }))}
                  placeholder={selectedCategory.placeholder}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="text-xs text-gray-500 mt-1">
                  {selectedCategory.value === 'reading' && 'Enter book title (e.g., "Ender\'s Game" or "Ender\'s Game by Orson Scott Card")'}
                  {selectedCategory.value === 'recently watched' && 'Enter movie title for automatic movie details'}
                  {selectedCategory.value === 'listening to' && 'Format: "Song by Artist" for automatic music details'}
                  {(selectedCategory.value === 'next trip' || selectedCategory.value === 'location') && 'Enter location for automatic place details'}
                </p>
              </div>

              {/* Action Buttons */}
              <div className="flex space-x-3 pt-4">
                <button
                  onClick={handlePreview}
                  disabled={isEnriching || isSaving}
                  className="flex-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
                >
                  {isEnriching ? (
                    <>
                      <Loader className="h-4 w-4 mr-2 animate-spin" />
                      Enriching...
                    </>
                  ) : (
                    'Preview & Enrich'
                  )}
                </button>
                
                <button
                  onClick={handleSave}
                  disabled={isSaving || isEnriching}
                  className="flex-1 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
                >
                  {isSaving ? (
                    <>
                      <LoadingSpinner size="small" className="mr-2" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4 mr-2" />
                      Save
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* Preview Section */}
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="text-sm font-medium text-gray-700 mb-3">Preview</h3>
              
              {previewData ? (
                <div className="space-y-4">
                  {/* Image Preview */}
                  {previewData.imageUrl && (
                    <div className="flex justify-center">
                      <img
                        src={previewData.imageUrl}
                        alt={previewData.value}
                        className="max-w-32 max-h-48 object-cover rounded-lg shadow-md"
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    </div>
                  )}

                  {/* Basic Info */}
                  <div className="bg-white rounded-lg p-3">
                    <div className="grid grid-cols-1 gap-2">
                      <div>
                        <span className="text-xs font-medium text-gray-500">Key:</span>
                        <p className="text-sm text-gray-900">{previewData.key}</p>
                      </div>
                      <div>
                        <span className="text-xs font-medium text-gray-500">Value:</span>
                        <p className="text-sm text-gray-900">{previewData.value}</p>
                      </div>
                      <div>
                        <span className="text-xs font-medium text-gray-500">Category:</span>
                        <p className="text-sm text-gray-900 capitalize">{previewData.category}</p>
                      </div>
                    </div>
                  </div>

                  {/* Enriched Data */}
                  {previewData.metadata && (
                    <div className="bg-white rounded-lg p-3">
                      <h4 className="text-xs font-medium text-gray-500 mb-2">Enriched Details</h4>
                      {(() => {
                        try {
                          const metadata = JSON.parse(previewData.metadata);
                          return (
                            <div className="space-y-1">
                              {Object.entries(metadata).map(([key, value]) => (
                                <div key={key} className="flex justify-between">
                                  <span className="text-xs text-gray-500 capitalize">
                                    {key.replace(/([A-Z])/g, ' $1').trim()}:
                                  </span>
                                  <span className="text-xs text-gray-900">{String(value)}</span>
                                </div>
                              ))}
                            </div>
                          );
                        } catch {
                          return <p className="text-xs text-gray-500">Unable to parse metadata</p>;
                        }
                      })()}
                    </div>
                  )}

                  {/* Status */}
                  <div className="text-xs text-gray-500">
                    {previewData.isEnriched ? (
                      <span className="text-green-600">✓ Enhanced with external data</span>
                    ) : (
                      <span className="text-gray-500">• Basic information only</span>
                    )}
                  </div>
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <p className="text-sm">Fill in the form and click "Preview & Enrich" to see how your quick fact will look with enriched data.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default QuickFactEditModal;