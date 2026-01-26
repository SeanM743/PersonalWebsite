import React, { useState, useEffect } from 'react';
import {
  Search,
  Book,
  Film,
  Tv,
  Music,
  Gamepad2,
  Star,
  Calendar,
  X,
  Save,
  AlertCircle
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import { LifeLogEntry, LifeLogType, EntryStatus } from './LifeLogView';

interface LifeLogEntryFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: () => void;
  entry?: LifeLogEntry;
  isNew?: boolean;
}

interface FormData {
  title: string;
  type: LifeLogType;
  startDate: string;
  endDate: string;
  status: EntryStatus;
  rating: number;
  keyTakeaway: string;
  intensity: number;
  imageUrl: string;
  metadata?: string;
}

interface BookMetadata {
  title: string;
  author_name?: string[];
  isbn?: string[];
  cover_i?: number;
  coverUrl?: string;
  publisher?: string[];
  first_publish_year?: number;
  description?: string | { value: string };
  subject?: string[];
  number_of_pages_median?: number;
}

const LifeLogEntryForm: React.FC<LifeLogEntryFormProps> = ({
  isOpen,
  onClose,
  onSave,
  entry,
  isNew = true
}) => {
  const [formData, setFormData] = useState<FormData>({
    title: '',
    type: LifeLogType.BOOK,
    startDate: '',
    endDate: '',
    status: EntryStatus.PLANNED,
    rating: 0,
    keyTakeaway: '',
    intensity: 1,
    imageUrl: '',
    metadata: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isFetchingMetadata, setIsFetchingMetadata] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const { error, success } = useNotification();

  // Initialize form data when entry changes
  useEffect(() => {
    if (entry && !isNew) {
      setFormData({
        title: entry.title || '',
        type: entry.type || LifeLogType.BOOK,
        startDate: entry.startDate ? entry.startDate.split('T')[0] : '',
        endDate: entry.endDate ? entry.endDate.split('T')[0] : '',
        status: entry.status || EntryStatus.PLANNED,
        rating: entry.rating || 0,
        keyTakeaway: entry.keyTakeaway || '',
        intensity: entry.intensity || 1,
        imageUrl: entry.imageUrl || '',
        metadata: entry.metadata || ''
      });
    } else {
      // Reset form for new entry
      setFormData({
        title: '',
        type: LifeLogType.BOOK,
        startDate: '',
        endDate: '',
        status: EntryStatus.PLANNED,
        rating: 0,
        keyTakeaway: '',
        intensity: 1,
        imageUrl: '',
        metadata: ''
      });
    }
    setErrors({});
  }, [entry, isNew, isOpen]);

  // Icon mapping for Life Log types
  const getIconForType = (type: LifeLogType) => {
    switch (type) {
      case LifeLogType.BOOK:
        return <Book className="h-5 w-5" />;
      case LifeLogType.MOVIE:
        return <Film className="h-5 w-5" />;
      case LifeLogType.SHOW:
        return <Tv className="h-5 w-5" />;
      case LifeLogType.ALBUM:
        return <Music className="h-5 w-5" />;
      case LifeLogType.HOBBY:
        return <Gamepad2 className="h-5 w-5" />;
      default:
        return <Book className="h-5 w-5" />;
    }
  };

  // Color mapping for Life Log types
  const getColorForType = (type: LifeLogType) => {
    switch (type) {
      case LifeLogType.BOOK:
        return 'bg-gradient-to-br from-amber-500 to-orange-600';
      case LifeLogType.MOVIE:
        return 'bg-gradient-to-br from-red-500 to-pink-600';
      case LifeLogType.SHOW:
        return 'bg-gradient-to-br from-purple-500 to-indigo-600';
      case LifeLogType.ALBUM:
        return 'bg-gradient-to-br from-blue-500 to-cyan-600';
      case LifeLogType.HOBBY:
        return 'bg-gradient-to-br from-green-500 to-emerald-600';
      default:
        return 'bg-gradient-to-br from-gray-500 to-slate-600';
    }
  };

  // Format type for display
  const formatType = (type: LifeLogType) => {
    return type.toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  // Format status for display
  const formatStatus = (status: EntryStatus) => {
    return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  // Handle form field changes
  const handleChange = (field: keyof FormData, value: string | number) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    // Clear error for this field
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  // Validate form data
  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Title is required
    if (!formData.title.trim()) {
      newErrors.title = 'Title is required';
    }

    // Intensity is required for HOBBY type and must be 1-5
    if (formData.type === LifeLogType.HOBBY) {
      if (!formData.intensity || formData.intensity < 1 || formData.intensity > 5) {
        newErrors.intensity = 'Intensity must be between 1 and 5 for hobbies';
      }
    }

    // Rating must be 0-5 if provided
    if (formData.rating && (formData.rating < 0 || formData.rating > 5)) {
      newErrors.rating = 'Rating must be between 0 and 5';
    }

    // End date must be after start date if both provided
    if (formData.startDate && formData.endDate) {
      const startDate = new Date(formData.startDate);
      const endDate = new Date(formData.endDate);
      if (endDate < startDate) {
        newErrors.endDate = 'End date must be after start date';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle metadata fetch
  const handleFetchMetadata = async () => {
    if (!formData.title.trim()) {
      setErrors(prev => ({ ...prev, title: 'Please enter a title to fetch info' }));
      return;
    }

    // Only support books 
    if (formData.type !== LifeLogType.BOOK) {
      return;
    }

    setIsFetchingMetadata(true);
    try {
      const response = await apiService.searchMetadata(formData.title, formData.type);
      if (response.success && response.data) {
        const metadata = response.data;

        // Update form with fetched data
        setFormData(prev => ({
          ...prev,
          title: metadata.title || prev.title,
          // Store basic image
          imageUrl: metadata.coverUrl || metadata.posterUrl || prev.imageUrl,
          // Store full metadata as JSON string for the modal
          metadata: JSON.stringify(metadata)
        }));

        success('Found book information!');
      } else {
        error('No information found for this title');
      }
    } catch (err: any) {
      error('Failed to fetch information', err.message);
    } finally {
      setIsFetchingMetadata(false);
    }
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const submitData = {
        title: formData.title.trim(),
        type: formData.type,
        startDate: formData.startDate || null,
        endDate: formData.endDate || null,
        status: formData.status,
        rating: formData.rating || null,
        keyTakeaway: formData.keyTakeaway.trim() || null,
        intensity: formData.type === LifeLogType.HOBBY ? formData.intensity : null,
        imageUrl: formData.imageUrl.trim() || null,
        metadata: formData.metadata || null
      };

      if (isNew) {
        await apiService.createLifeLogEntry(submitData);
        success('Life Log entry created successfully');
      } else {
        await apiService.updateLifeLogEntry(entry!.id, submitData);
        success('Life Log entry updated successfully');
      }

      onSave();
      onClose();
    } catch (err: any) {
      error(`Failed to ${isNew ? 'create' : 'update'} Life Log entry`, err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Render star rating selector
  const renderRatingSelector = () => (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-gray-700">
        Mahoney Rating (Optional)
      </label>
      <div className="flex items-center space-x-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => handleChange('rating', star === formData.rating ? 0 : star)}
            className="focus:outline-none"
          >
            <Star
              className={`h-6 w-6 transition-colors ${star <= formData.rating
                ? 'text-yellow-400 fill-current hover:text-yellow-500'
                : 'text-gray-300 hover:text-gray-400'
                }`}
            />
          </button>
        ))}
        {formData.rating > 0 && (
          <span className="text-sm text-gray-600 ml-2">({formData.rating}/5)</span>
        )}
      </div>
    </div>
  );

  // Helper to get parsed metadata
  const getBookMetadata = (): BookMetadata | null => {
    if (!formData.metadata) return null;
    try {
      return JSON.parse(formData.metadata);
    } catch (e) {
      return null;
    }
  };

  const bookMetadata = getBookMetadata();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <form onSubmit={handleSubmit}>
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <div className="flex items-center space-x-3">
              <div className={`w-10 h-10 ${getColorForType(formData.type)} rounded-xl flex items-center justify-center text-white`}>
                {getIconForType(formData.type)}
              </div>
              <h2 className="text-xl font-semibold text-gray-900">
                {isNew ? 'Add New Entry' : 'Edit Entry'}
              </h2>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          {/* Form Content */}
          <div className="p-6 space-y-6">
            {/* Title */}
            <div className="space-y-2">
              <label className="block text-sm font-medium text-gray-700">
                Title *
              </label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) => handleChange('title', e.target.value)}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${errors.title ? 'border-red-300' : 'border-gray-300'
                  }`}
                placeholder="Enter title..."
              />
              {errors.title && (
                <div className="flex items-center space-x-1 text-red-600 text-sm">
                  <AlertCircle className="h-4 w-4" />
                  <span>{errors.title}</span>
                </div>
              )}

              {formData.type === LifeLogType.BOOK && (
                <div className="space-y-3">
                  <button
                    type="button"
                    onClick={handleFetchMetadata}
                    disabled={isFetchingMetadata || !formData.title.trim()}
                    className="text-sm text-blue-600 hover:text-blue-800 flex items-center space-x-1 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isFetchingMetadata ? (
                      <span>Searching...</span>
                    ) : (
                      <>
                        <Search className="h-3 w-3" />
                        <span>Fetch Book Info</span>
                      </>
                    )}
                  </button>

                  {/* Read-only Book Metadata */}
                  {bookMetadata && (
                    <div className="bg-gray-50 rounded-lg p-4 border border-gray-200 text-sm space-y-3 animate-fadeIn">
                      <h3 className="font-medium text-gray-900 flex items-center space-x-2">
                        <Book className="h-4 w-4 text-gray-500" />
                        <span>Book Details</span>
                      </h3>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* Author */}
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">Author</label>
                          <input
                            type="text"
                            value={bookMetadata.author_name?.join(', ') || 'Unknown'}
                            disabled
                            className="w-full px-2 py-1.5 bg-gray-100 border border-gray-300 rounded text-gray-600 cursor-not-allowed text-xs"
                          />
                        </div>

                        {/* Publisher */}
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">Publisher</label>
                          <input
                            type="text"
                            value={(Array.isArray(bookMetadata.publisher) ? bookMetadata.publisher[0] : bookMetadata.publisher) || 'Unknown'}
                            disabled
                            className="w-full px-2 py-1.5 bg-gray-100 border border-gray-300 rounded text-gray-600 cursor-not-allowed text-xs"
                          />
                        </div>

                        {/* Year */}
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">First Published</label>
                          <input
                            type="text"
                            value={bookMetadata.first_publish_year || 'Unknown'}
                            disabled
                            className="w-full px-2 py-1.5 bg-gray-100 border border-gray-300 rounded text-gray-600 cursor-not-allowed text-xs"
                          />
                        </div>

                        {/* Pages */}
                        <div>
                          <label className="block text-xs font-medium text-gray-500 mb-1">Pages</label>
                          <input
                            type="text"
                            value={bookMetadata.number_of_pages_median || 'Unknown'}
                            disabled
                            className="w-full px-2 py-1.5 bg-gray-100 border border-gray-300 rounded text-gray-600 cursor-not-allowed text-xs"
                          />
                        </div>

                        {/* ISBN */}
                        {bookMetadata.isbn && (
                          <div className="md:col-span-2">
                            <label className="block text-xs font-medium text-gray-500 mb-1">ISBN</label>
                            <input
                              type="text"
                              value={Array.isArray(bookMetadata.isbn) ? bookMetadata.isbn[0] : bookMetadata.isbn}
                              disabled
                              className="w-full px-2 py-1.5 bg-gray-100 border border-gray-300 rounded text-gray-600 cursor-not-allowed text-xs font-mono"
                            />
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Type and Status Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Type */}
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  Type *
                </label>
                <select
                  value={formData.type}
                  onChange={(e) => handleChange('type', e.target.value as LifeLogType)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {Object.values(LifeLogType).map(type => (
                    <option key={type} value={type}>{formatType(type)}</option>
                  ))}
                </select>
              </div>

              {/* Status */}
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  Status *
                </label>
                <select
                  value={formData.status}
                  onChange={(e) => handleChange('status', e.target.value as EntryStatus)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {Object.values(EntryStatus).map(status => (
                    <option key={status} value={status}>{formatStatus(status)}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Dates Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Start Date */}
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  Start Date
                </label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    type="date"
                    value={formData.startDate}
                    onChange={(e) => handleChange('startDate', e.target.value)}
                    className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>

              {/* End Date */}
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  End Date
                </label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    type="date"
                    value={formData.endDate}
                    onChange={(e) => handleChange('endDate', e.target.value)}
                    className={`w-full pl-10 pr-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${errors.endDate ? 'border-red-300' : 'border-gray-300'
                      }`}
                  />
                </div>
                {errors.endDate && (
                  <div className="flex items-center space-x-1 text-red-600 text-sm">
                    <AlertCircle className="h-4 w-4" />
                    <span>{errors.endDate}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Intensity (only for HOBBY) */}
            {formData.type === LifeLogType.HOBBY && (
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  Intensity * (1-5)
                </label>
                <div className="flex items-center space-x-4">
                  <input
                    type="range"
                    min="1"
                    max="5"
                    value={formData.intensity}
                    onChange={(e) => handleChange('intensity', parseInt(e.target.value))}
                    className="flex-1"
                  />
                  <span className="text-sm font-medium text-gray-700 w-8">
                    {formData.intensity}
                  </span>
                </div>
                <p className="text-xs text-gray-500">
                  How much time/focus is this hobby consuming?
                </p>
                {errors.intensity && (
                  <div className="flex items-center space-x-1 text-red-600 text-sm">
                    <AlertCircle className="h-4 w-4" />
                    <span>{errors.intensity}</span>
                  </div>
                )}
              </div>
            )}

            {/* Rating */}
            {renderRatingSelector()}
            {errors.rating && (
              <div className="flex items-center space-x-1 text-red-600 text-sm">
                <AlertCircle className="h-4 w-4" />
                <span>{errors.rating}</span>
              </div>
            )}

            {/* Key Takeaway */}
            <div className="space-y-2">
              <label className="block text-sm font-medium text-gray-700">
                Key Takeaway
              </label>
              <textarea
                value={formData.keyTakeaway}
                onChange={(e) => handleChange('keyTakeaway', e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="What did you learn or find most interesting?"
              />
            </div>

            {/* Image URL */}
            <div className="space-y-2">
              <label className="block text-sm font-medium text-gray-700">
                Image URL (Optional)
              </label>
              <input
                type="url"
                value={formData.imageUrl}
                onChange={(e) => handleChange('imageUrl', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="https://example.com/image.jpg"
              />
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end space-x-3 p-6 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Save className="h-4 w-4" />
              <span>{isSubmitting ? 'Saving...' : (isNew ? 'Create Entry' : 'Update Entry')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LifeLogEntryForm;