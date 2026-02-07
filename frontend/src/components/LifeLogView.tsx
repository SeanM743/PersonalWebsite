import React, { useState, useEffect } from 'react';
import {
  Book,
  Filter,
  Plus,
  Search,
  ChevronDown
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from './UI/LoadingSpinner';
import LifeLogEntryForm from './LifeLogEntryForm';
import LifeLogEntryCard from './LifeLogEntryCard';

// Types based on backend LifeLogEntry entity
export enum LifeLogType {
  BOOK = 'BOOK',
  MOVIE = 'MOVIE',
  SHOW = 'SHOW',
  ALBUM = 'ALBUM',
  HOBBY = 'HOBBY'
}

export enum EntryStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  PAUSED = 'PAUSED',
  DROPPED = 'DROPPED'
}

export interface LifeLogEntry {
  id: number;
  title: string;
  type: LifeLogType;
  startDate?: string;
  endDate?: string;
  status: EntryStatus;
  rating?: number; // Mahoney Rating 1-5
  keyTakeaway?: string;
  intensity?: number; // 1-5, required for HOBBY
  imageUrl?: string;
  externalId?: string;
  metadata?: string;
  createdAt: string;
  updatedAt: string;
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

interface LifeLogViewProps {
  className?: string;
  compact?: boolean;
  onEntryChange?: () => void;
}

const LifeLogView: React.FC<LifeLogViewProps> = ({ className = "", compact = false, onEntryChange }) => {
  const [entries, setEntries] = useState<LifeLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedType, setSelectedType] = useState<LifeLogType | 'ALL'>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<EntryStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<LifeLogEntry | undefined>(undefined);
  const { error, success } = useNotification();

  // Format status for display
  const formatStatus = (status: EntryStatus) => {
    return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  // Format type for display
  const formatType = (type: LifeLogType) => {
    return type.toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  // Load Life Log entries
  const loadEntries = async () => {
    try {
      setIsLoading(true);

      const typeFilter = selectedType === 'ALL' ? undefined : selectedType;
      const statusFilter = selectedStatus === 'ALL' ? undefined : selectedStatus;

      const response = await apiService.getLifeLogEntries(typeFilter, statusFilter, 0, 50) as ContentResponse<PageResponse<LifeLogEntry>>;

      if (response.success) {
        let filteredEntries = response.data.content || [];

        // Apply search filter
        if (searchQuery.trim()) {
          const query = searchQuery.toLowerCase();
          filteredEntries = filteredEntries.filter(entry =>
            entry.title.toLowerCase().includes(query) ||
            entry.keyTakeaway?.toLowerCase().includes(query)
          );
        }

        // Sort by startDate (earliest first)
        filteredEntries.sort((a, b) => {
          if (!a.startDate) return 1;
          if (!b.startDate) return -1;
          return new Date(a.startDate).getTime() - new Date(b.startDate).getTime();
        });

        setEntries(filteredEntries);
      }
    } catch (err: any) {
      error('Failed to load Life Log entries', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadEntries();
  }, [selectedType, selectedStatus, searchQuery]);

  // Handle form actions
  const handleAddEntry = () => {
    setEditingEntry(undefined);
    setIsFormOpen(true);
  };

  const handleEditEntry = (entry: LifeLogEntry) => {
    setEditingEntry(entry);
    setIsFormOpen(true);
  };

  const handleDeleteEntry = async (entry: LifeLogEntry) => {
    const confirmed = window.confirm(
      `Delete "${entry.title}"?\n\nThis action cannot be undone.`
    );

    if (!confirmed) return;

    try {
      await apiService.deleteLifeLogEntry(entry.id);
      success('Entry deleted successfully');
      loadEntries(); // Reload entries
      if (onEntryChange) onEntryChange();
    } catch (err: any) {
      error('Failed to delete entry', err.message);
    }
  };

  const handleFormSave = () => {
    loadEntries(); // Reload entries after save
    if (onEntryChange) onEntryChange();
  };

  const handleFormClose = () => {
    setIsFormOpen(false);
    setEditingEntry(undefined);
  };

  if (isLoading) {
    return (
      <div className={`bg-card rounded-xl shadow-sm p-6 border border-border ${className}`}>
        <div className="flex items-center justify-center h-64">
          <LoadingSpinner size="large" />
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-card rounded-xl shadow-sm p-6 border border-border ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-main">Life Log</h2>
          <p className="text-sm text-muted mt-1">Track books, movies, shows, music, and hobbies</p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="p-2 text-muted hover:text-primary transition-colors rounded-lg hover:bg-page"
            title="Toggle filters"
          >
            <Filter className="h-5 w-5" />
          </button>
          <button
            onClick={handleAddEntry}
            className="p-2 text-muted hover:text-primary transition-colors rounded-lg hover:bg-page"
            title="Add new entry"
          >
            <Plus className="h-5 w-5" />
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-6 space-y-4">
        {/* Type Tabs */}
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setSelectedType('ALL')}
            className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${selectedType === 'ALL'
              ? 'bg-primary text-primary-foreground'
              : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
              }`}
          >
            All
          </button>
          {Object.values(LifeLogType).map((type) => (
            <button
              key={type}
              onClick={() => setSelectedType(type)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${selectedType === type
                ? 'bg-primary text-primary-foreground'
                : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                }`}
            >
              {formatType(type)}
            </button>
          ))}
        </div>

        {/* Collapsible Search & Status Filters */}
        {showFilters && (
          <div className="p-4 bg-secondary/50 rounded-lg animate-in fade-in slide-in-from-top-2">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Search */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <input
                  type="text"
                  placeholder="Search entries..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 bg-background border border-border rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent placeholder-muted-foreground"
                />
              </div>

              {/* Status Filter */}
              <div className="relative">
                <select
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value as EntryStatus | 'ALL')}
                  className="w-full px-4 py-2 bg-background border border-border rounded-lg text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent appearance-none"
                >
                  <option value="ALL">All Statuses</option>
                  {Object.values(EntryStatus).map(status => (
                    <option key={status} value={status}>{formatStatus(status)}</option>
                  ))}
                </select>
                <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Entries List */}
      {entries.length > 0 ? (
        <div className={`space-y-4 ${compact ? 'max-h-[400px] overflow-y-auto pr-2 scrollbar-thin' : ''}`}>
          {entries.map((entry) => (
            <LifeLogEntryCard
              key={entry.id}
              entry={entry}
              onEdit={handleEditEntry}
              onDelete={handleDeleteEntry}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-12">
          <div className="w-16 h-16 bg-page rounded-full flex items-center justify-center mx-auto mb-4">
            <Book className="h-8 w-8 text-muted" />
          </div>
          <p className="text-muted mb-2">
            {searchQuery || selectedType !== 'ALL' || selectedStatus !== 'ALL'
              ? 'No entries match your filters.'
              : 'No Life Log entries yet.'
            }
          </p>
          <button
            onClick={handleAddEntry}
            className="text-blue-500 hover:text-blue-600 text-sm font-medium"
          >
            Add your first entry
          </button>
        </div>
      )}

      {/* Entry Count */}
      {entries.length > 0 && (
        <div className="mt-6 pt-4 border-t border-border">
          <p className="text-sm text-muted text-center">
            Showing {entries.length} {entries.length === 1 ? 'entry' : 'entries'}
          </p>
        </div>
      )}

      {/* Life Log Entry Form */}
      <LifeLogEntryForm
        isOpen={isFormOpen}
        onClose={handleFormClose}
        onSave={handleFormSave}
        entry={editingEntry}
        isNew={!editingEntry}
      />
    </div>
  );
};

export default LifeLogView;