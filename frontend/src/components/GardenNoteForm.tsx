import React, { useState, useEffect } from 'react';
import { 
  Sprout,
  Leaf,
  TreePine,
  Save,
  X,
  Link,
  Unlink,
  Search,
  Plus
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import { GrowthStage, GardenNote, LinkedEntry } from './DigitalGardenView';
import { LifeLogEntry } from './LifeLogView';
import LoadingSpinner from './UI/LoadingSpinner';

interface GardenNoteFormProps {
  note?: GardenNote;
  isOpen: boolean;
  onClose: () => void;
  onSave: (note: GardenNote) => void;
}

interface FormData {
  title: string;
  content: string;
  growthStage: GrowthStage;
  linkedEntries: LinkedEntry[];
}

const GardenNoteForm: React.FC<GardenNoteFormProps> = ({
  note,
  isOpen,
  onClose,
  onSave
}) => {
  const [formData, setFormData] = useState<FormData>({
    title: '',
    content: '',
    growthStage: GrowthStage.SEEDLING,
    linkedEntries: []
  });
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [availableEntries, setAvailableEntries] = useState<LifeLogEntry[]>([]);
  const [showLinkingModal, setShowLinkingModal] = useState(false);
  const [linkingSearch, setLinkingSearch] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const { success, error } = useNotification();

  // Growth stage configuration
  const getStageConfig = (stage: GrowthStage) => {
    switch (stage) {
      case GrowthStage.SEEDLING:
        return {
          icon: <Sprout className="h-4 w-4" />,
          emoji: '🌱',
          color: 'text-green-600',
          bg: 'bg-green-50',
          border: 'border-green-200',
          label: 'Seedling',
          description: 'New ideas and early thoughts'
        };
      case GrowthStage.BUDDING:
        return {
          icon: <Leaf className="h-4 w-4" />,
          emoji: '🌿',
          color: 'text-emerald-600',
          bg: 'bg-emerald-50',
          border: 'border-emerald-200',
          label: 'Budding',
          description: 'Developing concepts with connections'
        };
      case GrowthStage.EVERGREEN:
        return {
          icon: <TreePine className="h-4 w-4" />,
          emoji: '🌲',
          color: 'text-teal-600',
          bg: 'bg-teal-50',
          border: 'border-teal-200',
          label: 'Evergreen',
          description: 'Mature, well-developed knowledge'
        };
      default:
        return {
          icon: <Sprout className="h-4 w-4" />,
          emoji: '🌱',
          color: 'text-gray-600',
          bg: 'bg-gray-50',
          border: 'border-gray-200',
          label: 'Unknown',
          description: 'Unknown growth stage'
        };
    }
  };

  // Initialize form data when note changes
  useEffect(() => {
    if (note) {
      setFormData({
        title: note.title,
        content: note.content,
        growthStage: note.growthStage,
        linkedEntries: note.linkedEntries || []
      });
    } else {
      setFormData({
        title: '',
        content: '',
        growthStage: GrowthStage.SEEDLING,
        linkedEntries: []
      });
    }
    setErrors({});
  }, [note]);

  // Load available Life Log entries for linking
  const loadAvailableEntries = async () => {
    try {
      setIsLoading(true);
      const response = await apiService.getLifeLogEntries();
      
      if (response.success) {
        setAvailableEntries(response.data?.content || []);
      }
    } catch (err: any) {
      error('Failed to load Life Log entries', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Validate form data
  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.title.trim()) {
      newErrors.title = 'Title is required';
    } else if (formData.title.length > 200) {
      newErrors.title = 'Title must be less than 200 characters';
    }

    if (!formData.content.trim()) {
      newErrors.content = 'Content is required';
    } else if (formData.content.length > 5000) {
      newErrors.content = 'Content must be less than 5000 characters';
    }

    if (!formData.growthStage) {
      newErrors.growthStage = 'Growth stage is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      setIsSaving(true);
      
      const noteData = {
        title: formData.title.trim(),
        content: formData.content.trim(),
        growthStage: formData.growthStage
      };

      let response;
      if (note) {
        response = await apiService.updateGardenNote(note.id, noteData);
      } else {
        response = await apiService.createGardenNote(noteData);
      }

      if (response.success) {
        const savedNote = response.data;
        
        // Handle linking changes if this is an update
        if (note) {
          const currentLinkedIds = new Set(note.linkedEntries.map(e => e.id));
          const newLinkedIds = new Set(formData.linkedEntries.map(e => e.id));
          
          // Add new links
          for (const entryId of newLinkedIds) {
            if (!currentLinkedIds.has(entryId)) {
              await apiService.linkGardenNoteToLifeLog(savedNote.id, entryId);
            }
          }
          
          // Remove old links
          for (const entryId of currentLinkedIds) {
            if (!newLinkedIds.has(entryId)) {
              await apiService.unlinkGardenNoteFromLifeLog(savedNote.id, entryId);
            }
          }
        } else {
          // Add all links for new note
          for (const entry of formData.linkedEntries) {
            await apiService.linkGardenNoteToLifeLog(savedNote.id, entry.id);
          }
        }

        // Refresh the note with updated links
        const updatedResponse = await apiService.getGardenNote(savedNote.id);
        if (updatedResponse.success) {
          onSave(updatedResponse.data);
        } else {
          onSave(savedNote);
        }

        success(note ? 'Note updated successfully' : 'Note created successfully');
        onClose();
      }
    } catch (err: any) {
      error('Failed to save note', err.message);
    } finally {
      setIsSaving(false);
    }
  };

  // Handle input changes
  const handleInputChange = (field: keyof FormData, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  // Add linked entry
  const addLinkedEntry = (entry: LifeLogEntry) => {
    if (!formData.linkedEntries.some(e => e.id === entry.id)) {
      const linkedEntry: LinkedEntry = {
        id: entry.id,
        title: entry.title,
        type: entry.type
      };
      
      handleInputChange('linkedEntries', [...formData.linkedEntries, linkedEntry]);
    }
  };

  // Remove linked entry
  const removeLinkedEntry = (entryId: number) => {
    handleInputChange('linkedEntries', 
      formData.linkedEntries.filter(e => e.id !== entryId)
    );
  };

  // Filter available entries for linking
  const filteredAvailableEntries = availableEntries.filter(entry => {
    const matchesSearch = !linkingSearch || 
      entry.title.toLowerCase().includes(linkingSearch.toLowerCase()) ||
      entry.type.toLowerCase().includes(linkingSearch.toLowerCase());
    
    const notAlreadyLinked = !formData.linkedEntries.some(linked => linked.id === entry.id);
    
    return matchesSearch && notAlreadyLinked;
  });

  // Open linking modal
  const openLinkingModal = () => {
    setShowLinkingModal(true);
    loadAvailableEntries();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">
            {note ? 'Edit Garden Note' : 'Create Garden Note'}
          </h2>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-gray-600 transition-colors rounded-lg hover:bg-gray-100"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Title */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Title *
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => handleInputChange('title', e.target.value)}
              className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                errors.title ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="Enter note title..."
              maxLength={200}
            />
            {errors.title && (
              <p className="text-red-500 text-sm mt-1">{errors.title}</p>
            )}
            <p className="text-gray-500 text-xs mt-1">
              {formData.title.length}/200 characters
            </p>
          </div>

          {/* Growth Stage */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Growth Stage *
            </label>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {Object.values(GrowthStage).map(stage => {
                const config = getStageConfig(stage);
                const isSelected = formData.growthStage === stage;
                
                return (
                  <button
                    key={stage}
                    type="button"
                    onClick={() => handleInputChange('growthStage', stage)}
                    className={`p-4 rounded-lg border-2 transition-all duration-200 hover:scale-105 active:scale-95 ${
                      isSelected 
                        ? `${config.bg} ${config.border} ${config.color} shadow-md` 
                        : `bg-white ${config.border} ${config.color} hover:${config.bg}`
                    }`}
                  >
                    <div className="flex items-center space-x-2 mb-2">
                      <span className="text-2xl">{config.emoji}</span>
                      <span className="font-medium">{config.label}</span>
                    </div>
                    <p className="text-xs text-left">{config.description}</p>
                  </button>
                );
              })}
            </div>
            {errors.growthStage && (
              <p className="text-red-500 text-sm mt-1">{errors.growthStage}</p>
            )}
          </div>

          {/* Content */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Content *
            </label>
            <textarea
              value={formData.content}
              onChange={(e) => handleInputChange('content', e.target.value)}
              rows={8}
              className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-vertical ${
                errors.content ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="Write your note content here..."
              maxLength={5000}
            />
            {errors.content && (
              <p className="text-red-500 text-sm mt-1">{errors.content}</p>
            )}
            <p className="text-gray-500 text-xs mt-1">
              {formData.content.length}/5000 characters
            </p>
          </div>

          {/* Linked Entries */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="block text-sm font-medium text-gray-700">
                Linked Life Log Entries
              </label>
              <button
                type="button"
                onClick={openLinkingModal}
                className="flex items-center space-x-1 px-3 py-1 text-sm text-blue-600 hover:text-blue-700 transition-colors"
              >
                <Plus className="h-4 w-4" />
                <span>Add Link</span>
              </button>
            </div>
            
            {formData.linkedEntries.length > 0 ? (
              <div className="space-y-2">
                {formData.linkedEntries.map(entry => (
                  <div
                    key={entry.id}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex items-center space-x-2">
                      <Link className="h-4 w-4 text-gray-400" />
                      <div>
                        <div className="font-medium text-gray-900">{entry.title}</div>
                        <div className="text-sm text-gray-500">{entry.type}</div>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeLinkedEntry(entry.id)}
                      className="p-1 text-red-600 hover:text-red-700 transition-colors"
                      title="Remove link"
                    >
                      <Unlink className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-gray-500 bg-gray-50 rounded-lg">
                <Link className="h-8 w-8 mx-auto mb-2 text-gray-400" />
                <p className="text-sm">No linked entries</p>
                <p className="text-xs">Connect this note to Life Log entries for better organization</p>
              </div>
            )}
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              disabled={isSaving}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSaving ? (
                <LoadingSpinner size="small" />
              ) : (
                <Save className="h-4 w-4" />
              )}
              <span>{isSaving ? 'Saving...' : 'Save Note'}</span>
            </button>
          </div>
        </form>
      </div>

      {/* Linking Modal */}
      {showLinkingModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-60 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[70vh] overflow-hidden">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">Link Life Log Entries</h3>
              <button
                onClick={() => setShowLinkingModal(false)}
                className="p-2 text-gray-400 hover:text-gray-600 transition-colors rounded-lg hover:bg-gray-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Search */}
            <div className="p-4 border-b border-gray-200">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search entries..."
                  value={linkingSearch}
                  onChange={(e) => setLinkingSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Available Entries */}
            <div className="p-4 max-h-80 overflow-y-auto">
              {isLoading ? (
                <div className="flex items-center justify-center py-8">
                  <LoadingSpinner size="medium" />
                </div>
              ) : filteredAvailableEntries.length > 0 ? (
                <div className="space-y-2">
                  {filteredAvailableEntries.map(entry => (
                    <button
                      key={entry.id}
                      onClick={() => {
                        addLinkedEntry(entry);
                        setShowLinkingModal(false);
                        setLinkingSearch('');
                      }}
                      className="w-full p-3 text-left bg-gray-50 hover:bg-blue-50 rounded-lg transition-colors"
                    >
                      <div className="font-medium text-gray-900">{entry.title}</div>
                      <div className="text-sm text-gray-500">{entry.type}</div>
                      {entry.startDate && (
                        <div className="text-xs text-gray-400 mt-1">
                          {new Date(entry.startDate).toLocaleDateString()}
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <Search className="h-8 w-8 mx-auto mb-2 text-gray-400" />
                  <p className="text-sm">
                    {linkingSearch ? 'No entries match your search' : 'No entries available to link'}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default GardenNoteForm;