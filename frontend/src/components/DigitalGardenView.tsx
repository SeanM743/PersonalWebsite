import React, { useState, useEffect } from 'react';
import { 
  Sprout,
  Leaf,
  TreePine,
  Search,
  Filter,
  Plus
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from './UI/LoadingSpinner';
import GardenNoteForm from './GardenNoteForm';
import GardenNoteCard from './GardenNoteCard';

export enum GrowthStage {
  SEEDLING = 'SEEDLING',
  BUDDING = 'BUDDING',
  EVERGREEN = 'EVERGREEN'
}

export interface GardenNote {
  id: number;
  title: string;
  content: string;
  growthStage: GrowthStage;
  linkedEntries: LinkedEntry[];
  createdAt: string;
  updatedAt: string;
}

export interface LinkedEntry {
  id: number;
  title: string;
  type: string;
}

interface DigitalGardenViewProps {
  className?: string;
}

const DigitalGardenView: React.FC<DigitalGardenViewProps> = ({ className = "" }) => {
  const [notes, setNotes] = useState<GardenNote[]>([]);
  const [filteredNotes, setFilteredNotes] = useState<GardenNote[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedStages, setSelectedStages] = useState<Set<GrowthStage>>(
    new Set(Object.values(GrowthStage))
  );
  const [showFilters, setShowFilters] = useState(false);
  const [editingNote, setEditingNote] = useState<GardenNote | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
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

  // Load garden notes
  const loadNotes = async () => {
    try {
      setIsLoading(true);
      const response = await apiService.getGardenNotes();
      
      if (response.success) {
        const notesData = response.data || [];
        setNotes(notesData);
      }
    } catch (err: any) {
      error('Failed to load garden notes', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Filter notes based on search term and selected stages
  useEffect(() => {
    let filtered = notes.filter(note => selectedStages.has(note.growthStage));
    
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(note =>
        note.title.toLowerCase().includes(term) ||
        note.content.toLowerCase().includes(term) ||
        note.linkedEntries.some(entry => 
          entry.title.toLowerCase().includes(term)
        )
      );
    }
    
    setFilteredNotes(filtered);
  }, [notes, searchTerm, selectedStages]);

  // Toggle growth stage filter
  const toggleStageFilter = (stage: GrowthStage) => {
    const newSelectedStages = new Set(selectedStages);
    if (newSelectedStages.has(stage)) {
      newSelectedStages.delete(stage);
    } else {
      newSelectedStages.add(stage);
    }
    setSelectedStages(newSelectedStages);
  };

  // Select all stages
  const selectAllStages = () => {
    setSelectedStages(new Set(Object.values(GrowthStage)));
  };

  // Clear all stages
  const clearAllStages = () => {
    setSelectedStages(new Set());
  };

  // Delete note
  const handleDeleteNote = async (noteId: number) => {
    try {
      const response = await apiService.deleteGardenNote(noteId);
      
      if (response.success) {
        setNotes(prev => prev.filter(note => note.id !== noteId));
        success('Note deleted successfully');
      }
    } catch (err: any) {
      error('Failed to delete note', err.message);
    }
  };

  // Handle note creation
  const handleCreateNote = () => {
    setEditingNote(null);
    setIsFormOpen(true);
  };

  // Handle note editing
  const handleEditNote = (note: GardenNote) => {
    setEditingNote(note);
    setIsFormOpen(true);
  };

  // Handle form save
  const handleFormSave = (savedNote: GardenNote) => {
    if (editingNote) {
      // Update existing note
      setNotes(prev => prev.map(note => 
        note.id === savedNote.id ? savedNote : note
      ));
    } else {
      // Add new note
      setNotes(prev => [savedNote, ...prev]);
    }
    setIsFormOpen(false);
    setEditingNote(null);
  };

  // Handle form close
  const handleFormClose = () => {
    setIsFormOpen(false);
    setEditingNote(null);
  };

  // Handle linked entry click
  const handleLinkedEntryClick = (entry: LinkedEntry) => {
    // TODO: Navigate to Life Log entry or show details
    console.log('Clicked linked entry:', entry);
  };

  // Get filter statistics
  const getFilterStats = () => {
    const totalNotes = notes.length;
    const filteredCount = filteredNotes.length;
    const stageStats = Object.values(GrowthStage).map(stage => ({
      stage,
      count: notes.filter(note => note.growthStage === stage).length,
      visible: selectedStages.has(stage)
    }));
    
    return { totalNotes, filteredCount, stageStats };
  };

  useEffect(() => {
    loadNotes();
  }, []);

  if (isLoading) {
    return (
      <div className={`bg-white rounded-xl shadow-sm p-6 ${className}`}>
        <div className="flex items-center justify-center h-64">
          <LoadingSpinner size="large" />
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-white rounded-xl shadow-sm p-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Digital Garden</h2>
          <p className="text-sm text-gray-500 mt-1">
            Cultivate your knowledge and ideas as they grow
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="p-2 text-gray-400 hover:text-blue-600 transition-colors rounded-lg hover:bg-blue-50"
            title="Toggle filters"
          >
            <Filter className="h-5 w-5" />
          </button>
          <button
            onClick={handleCreateNote}
            className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="h-4 w-4" />
            <span>New Note</span>
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search notes, content, or linked entries..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      {/* Growth Stage Filters */}
      {showFilters && (
        <div className="mb-6 p-4 bg-gray-50 rounded-lg">
          {/* Filter Controls */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-medium text-gray-700">Filter by Growth Stage</h3>
              <span className="text-xs text-gray-500">
                ({getFilterStats().filteredCount} of {getFilterStats().totalNotes} notes)
              </span>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={selectAllStages}
                className="text-xs text-blue-600 hover:text-blue-700 transition-colors"
              >
                Select All
              </button>
              <span className="text-gray-300">|</span>
              <button
                onClick={clearAllStages}
                className="text-xs text-gray-600 hover:text-gray-700 transition-colors"
              >
                Clear All
              </button>
            </div>
          </div>
          
          {/* Stage Filter Buttons */}
          <div className="flex flex-wrap gap-2 mb-4">
            {Object.values(GrowthStage).map(stage => {
              const config = getStageConfig(stage);
              const isSelected = selectedStages.has(stage);
              const stageCount = notes.filter(note => note.growthStage === stage).length;
              
              return (
                <button
                  key={stage}
                  onClick={() => toggleStageFilter(stage)}
                  className={`flex items-center space-x-2 px-3 py-2 rounded-lg border-2 transition-all duration-200 hover:scale-105 active:scale-95 ${
                    isSelected 
                      ? `${config.bg} ${config.border} ${config.color} shadow-md` 
                      : `bg-white ${config.border} ${config.color} hover:${config.bg}`
                  }`}
                >
                  <span className="text-lg">{config.emoji}</span>
                  <span className="text-sm font-medium">{config.label}</span>
                  <span className={`text-xs px-1.5 py-0.5 rounded-full ${
                    isSelected ? 'bg-white bg-opacity-50' : 'bg-gray-200'
                  }`}>
                    {stageCount}
                  </span>
                </button>
              );
            })}
          </div>
          
          {/* Stage Descriptions */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 text-xs">
            {Object.values(GrowthStage).map(stage => {
              const config = getStageConfig(stage);
              const isVisible = selectedStages.has(stage);
              
              return (
                <div
                  key={stage}
                  className={`p-2 rounded ${
                    isVisible ? config.bg : 'bg-gray-100'
                  } transition-colors duration-200`}
                >
                  <div className={`flex items-center space-x-1 mb-1 ${
                    isVisible ? config.color : 'text-gray-500'
                  }`}>
                    <span>{config.emoji}</span>
                    <span className="font-medium">{config.label}</span>
                  </div>
                  <p className={isVisible ? config.color : 'text-gray-500'}>
                    {config.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Notes Grid */}
      {filteredNotes.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredNotes.map(note => (
            <GardenNoteCard
              key={note.id}
              note={note}
              onEdit={handleEditNote}
              onDelete={handleDeleteNote}
              onLinkedEntryClick={handleLinkedEntryClick}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-12">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <TreePine className="h-8 w-8 text-gray-400" />
          </div>
          <p className="text-gray-500 mb-2">
            {selectedStages.size === 0 
              ? 'Select growth stages to view notes.' 
              : searchTerm 
                ? 'No notes match your search criteria.'
                : 'Your digital garden is empty.'
            }
          </p>
          <p className="text-sm text-gray-400">
            {searchTerm 
              ? 'Try adjusting your search terms or filters.'
              : 'Start cultivating knowledge by creating your first note.'
            }
          </p>
        </div>
      )}

      {/* Garden Statistics */}
      {notes.length > 0 && (
        <div className="mt-6 pt-4 border-t border-gray-200">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
            <div>
              <div className="text-2xl font-bold text-gray-900">{notes.length}</div>
              <div className="text-sm text-gray-500">Total Notes</div>
            </div>
            {Object.values(GrowthStage).map(stage => {
              const config = getStageConfig(stage);
              const count = notes.filter(note => note.growthStage === stage).length;
              
              return (
                <div key={stage}>
                  <div className={`text-2xl font-bold ${config.color}`}>
                    {count}
                  </div>
                  <div className="text-sm text-gray-500 flex items-center justify-center space-x-1">
                    <span>{config.emoji}</span>
                    <span>{config.label}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Garden Note Form Modal */}
      <GardenNoteForm
        note={editingNote || undefined}
        isOpen={isFormOpen}
        onClose={handleFormClose}
        onSave={handleFormSave}
      />
    </div>
  );
};

export default DigitalGardenView;