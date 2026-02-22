import React from 'react';
import { 
  Sprout,
  Leaf,
  TreePine,
  Link,
  Edit,
  Trash2,
  ExternalLink,
  Calendar,
  Clock
} from 'lucide-react';
import { GrowthStage, GardenNote, LinkedEntry } from './DigitalGardenView';

interface GardenNoteCardProps {
  note: GardenNote;
  onEdit: (note: GardenNote) => void;
  onDelete: (noteId: number) => void;
  onLinkedEntryClick?: (entry: LinkedEntry) => void;
  className?: string;
}

const GardenNoteCard: React.FC<GardenNoteCardProps> = ({
  note,
  onEdit,
  onDelete,
  onLinkedEntryClick,
  className = ""
}) => {
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
          gradient: 'from-green-50 to-green-100',
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
          gradient: 'from-emerald-50 to-emerald-100',
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
          gradient: 'from-teal-50 to-teal-100',
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
          gradient: 'from-gray-50 to-gray-100',
          label: 'Unknown',
          description: 'Unknown growth stage'
        };
    }
  };

  // Format date for display
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  // Format relative time
  const formatRelativeTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));
    
    if (diffInDays === 0) {
      return 'Today';
    } else if (diffInDays === 1) {
      return 'Yesterday';
    } else if (diffInDays < 7) {
      return `${diffInDays} days ago`;
    } else if (diffInDays < 30) {
      const weeks = Math.floor(diffInDays / 7);
      return `${weeks} week${weeks > 1 ? 's' : ''} ago`;
    } else if (diffInDays < 365) {
      const months = Math.floor(diffInDays / 30);
      return `${months} month${months > 1 ? 's' : ''} ago`;
    } else {
      const years = Math.floor(diffInDays / 365);
      return `${years} year${years > 1 ? 's' : ''} ago`;
    }
  };

  // Truncate content for preview
  const truncateContent = (content: string, maxLength: number = 150) => {
    if (content.length <= maxLength) return content;
    return content.substring(0, maxLength).trim() + '...';
  };

  // Handle delete with confirmation
  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this note? This action cannot be undone.')) {
      onDelete(note.id);
    }
  };

  const config = getStageConfig(note.growthStage);

  return (
    <div className={`group relative bg-gradient-to-br ${config.gradient} ${config.border} border-2 rounded-xl p-6 hover:shadow-lg transition-all duration-300 hover:scale-105 ${className}`}>
      {/* Growth Stage Badge */}
      <div className={`absolute top-4 right-4 flex items-center space-x-1 px-2 py-1 ${config.bg} ${config.border} border rounded-full`}>
        <span className="text-sm">{config.emoji}</span>
        <span className={`text-xs font-medium ${config.color}`}>
          {config.label}
        </span>
      </div>

      {/* Note Header */}
      <div className="mb-4 pr-20">
        <h3 className={`text-lg font-bold ${config.color} mb-2 line-clamp-2`}>
          {note.title}
        </h3>
        
        {/* Timestamps */}
        <div className="flex items-center space-x-4 text-xs text-gray-500">
          <div className="flex items-center space-x-1">
            <Calendar className="h-3 w-3" />
            <span>{formatDate(note.createdAt)}</span>
          </div>
          {note.updatedAt !== note.createdAt && (
            <div className="flex items-center space-x-1">
              <Clock className="h-3 w-3" />
              <span>Updated {formatRelativeTime(note.updatedAt)}</span>
            </div>
          )}
        </div>
      </div>

      {/* Note Content Preview */}
      <div className={`${config.color} text-sm mb-4 leading-relaxed`}>
        <p className="line-clamp-4">
          {truncateContent(note.content)}
        </p>
      </div>

      {/* Linked Entries */}
      {note.linkedEntries && note.linkedEntries.length > 0 && (
        <div className="mb-4">
          <div className={`flex items-center space-x-1 mb-2 ${config.color}`}>
            <Link className="h-3 w-3" />
            <span className="text-xs font-medium">
              Linked ({note.linkedEntries.length})
            </span>
          </div>
          <div className="flex flex-wrap gap-1">
            {note.linkedEntries.slice(0, 3).map(entry => (
              <button
                key={entry.id}
                onClick={() => onLinkedEntryClick?.(entry)}
                className="inline-flex items-center space-x-1 px-2 py-1 bg-white bg-opacity-70 hover:bg-opacity-90 rounded-full text-xs transition-all duration-200 hover:scale-105"
                title={`${entry.type}: ${entry.title}`}
              >
                <span className={config.color}>{entry.title}</span>
                <ExternalLink className="h-2 w-2" />
              </button>
            ))}
            {note.linkedEntries.length > 3 && (
              <span className="inline-flex items-center px-2 py-1 bg-white bg-opacity-50 rounded-full text-xs">
                +{note.linkedEntries.length - 3} more
              </span>
            )}
          </div>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex items-center justify-between">
        {/* Growth Progress Indicator */}
        <div className="flex items-center space-x-1">
          <div className="flex space-x-1">
            {Object.values(GrowthStage).map((stage) => {
              const stageConfig = getStageConfig(stage);
              const isCurrentStage = stage === note.growthStage;
              const isPastStage = Object.values(GrowthStage).indexOf(stage) <= 
                                 Object.values(GrowthStage).indexOf(note.growthStage);
              
              return (
                <div
                  key={stage}
                  className={`w-2 h-2 rounded-full transition-all duration-200 ${
                    isCurrentStage 
                      ? `${stageConfig.bg.replace('bg-', 'bg-')} ring-2 ring-white ring-opacity-50` 
                      : isPastStage 
                        ? `${stageConfig.bg.replace('bg-', 'bg-')} opacity-60`
                        : 'bg-gray-200'
                  }`}
                  title={stageConfig.label}
                />
              );
            })}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
          <button
            onClick={() => onEdit(note)}
            className={`p-2 ${config.color} hover:bg-white hover:bg-opacity-50 rounded-lg transition-all duration-200 hover:scale-110`}
            title="Edit note"
          >
            <Edit className="h-4 w-4" />
          </button>
          <button
            onClick={handleDelete}
            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-all duration-200 hover:scale-110"
            title="Delete note"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Hover Overlay */}
      <div className={`absolute inset-0 bg-white bg-opacity-10 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none`} />

      {/* Content Length Indicator */}
      <div className="absolute bottom-2 left-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
        <div className={`text-xs ${config.color} opacity-75`}>
          {note.content.length} chars
        </div>
      </div>

      {/* Quick Actions on Hover */}
      <div className="absolute top-2 left-2 opacity-0 group-hover:opacity-100 transition-all duration-200 transform -translate-y-1 group-hover:translate-y-0">
        <div className={`text-xs ${config.color} font-medium px-2 py-1 bg-white bg-opacity-80 rounded-full`}>
          {config.description}
        </div>
      </div>
    </div>
  );
};

export default GardenNoteCard;