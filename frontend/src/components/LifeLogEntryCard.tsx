import React from 'react';
import {
  Book,
  Film,
  Tv,
  Music,
  Gamepad2,
  Star,
  Calendar,
  Clock,
  Edit,
  Trash2,
  Zap,
  Eye,
  Info
} from 'lucide-react';
import BookMetadataModal from './BookMetadataModal';
import { LifeLogEntry, LifeLogType, EntryStatus } from './LifeLogView';

interface LifeLogEntryCardProps {
  entry: LifeLogEntry;
  onEdit: (entry: LifeLogEntry) => void;
  onDelete: (entry: LifeLogEntry) => void;
  onView?: (entry: LifeLogEntry) => void;
  compact?: boolean;
  className?: string;
}

const LifeLogEntryCard: React.FC<LifeLogEntryCardProps> = ({
  entry,
  onEdit,
  onDelete,
  onView,
  compact = false,
  className = ""
}) => {
  const [isModalOpen, setIsModalOpen] = React.useState(false);

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

  // Status color mapping
  const getStatusColor = (status: EntryStatus) => {
    switch (status) {
      case EntryStatus.COMPLETED:
        return 'bg-green-100 text-green-800 border-green-200';
      case EntryStatus.IN_PROGRESS:
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case EntryStatus.PLANNED:
        return 'bg-gray-100 text-gray-800 border-gray-200';
      case EntryStatus.PAUSED:
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case EntryStatus.DROPPED:
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  // Format status for display
  const formatStatus = (status: EntryStatus) => {
    return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  // Format type for display
  const formatType = (type: LifeLogType) => {
    return type.toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  };

  // Format date for display
  const formatDate = (dateString?: string) => {
    if (!dateString) return null;
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  // Calculate duration if both dates are available
  const calculateDuration = () => {
    if (!entry.startDate || !entry.endDate) return null;

    const start = new Date(entry.startDate);
    const end = new Date(entry.endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) return '1 day';
    if (diffDays < 30) return `${diffDays} days`;
    if (diffDays < 365) {
      const months = Math.round(diffDays / 30);
      return months === 1 ? '1 month' : `${months} months`;
    }
    const years = Math.round(diffDays / 365);
    return years === 1 ? '1 year' : `${years} years`;
  };

  // Render star rating
  const renderRating = (rating?: number) => {
    if (!rating) return null;

    return (
      <div className="flex items-center space-x-1">
        <div className="flex items-center">
          {[1, 2, 3, 4, 5].map((star) => (
            <Star
              key={star}
              className={`h-4 w-4 ${star <= rating
                ? 'text-yellow-400 fill-current'
                : 'text-muted/30'
                }`}
            />
          ))}
        </div>
        <span className="text-sm text-gray-600">({rating}/5)</span>
      </div>
    );
  };

  // Render intensity indicator
  const renderIntensity = (intensity?: number) => {
    if (!intensity || entry.type !== LifeLogType.HOBBY) return null;

    return (
      <div className="flex items-center space-x-2">
        <Zap className="h-4 w-4 text-orange-500" />
        <div className="flex items-center space-x-1">
          {[1, 2, 3, 4, 5].map((level) => (
            <div
              key={level}
              className={`w-2 h-2 rounded-full ${level <= intensity
                ? 'bg-orange-500'
                : 'bg-muted/30'
                }`}
            />
          ))}
        </div>
        <span className="text-sm text-gray-600">({intensity}/5)</span>
      </div>
    );
  };

  const duration = calculateDuration();

  return (
    <div className={`border border-border rounded-xl p-4 hover:shadow-lg transition-all duration-200 hover:border-primary/30 bg-card ${className}`}>
      <div className="flex items-start space-x-4">
        {/* Type Icon */}
        <div className={`w-12 h-12 ${getColorForType(entry.type)} rounded-xl flex items-center justify-center text-white shadow-sm flex-shrink-0`}>
          {getIconForType(entry.type)}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              {/* Title and Type */}
              <div className="flex items-start justify-between mb-2">
                <h3 className={`font-semibold text-main ${compact ? 'text-base' : 'text-lg'} leading-tight`}>
                  {entry.title}
                </h3>
                {onView && (
                  <button
                    onClick={() => onView(entry)}
                    className="ml-2 p-1 text-muted hover:text-primary transition-colors rounded"
                    title="View details"
                  >
                    <Eye className="h-4 w-4" />
                  </button>
                )}
              </div>

              {/* Status and Type Badges */}
              <div className="flex items-center space-x-2 mb-3">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getStatusColor(entry.status)}`}>
                  {formatStatus(entry.status)}
                </span>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-page text-muted border border-border">
                  {formatType(entry.type)}
                </span>
              </div>

              {/* Dates and Duration */}
              {!compact && (
                <div className="space-y-2 mb-3">
                  <div className="flex items-center space-x-4 text-sm text-muted">
                    {entry.startDate && (
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-4 w-4" />
                        <span>Started {formatDate(entry.startDate)}</span>
                      </div>
                    )}
                    {entry.endDate && (
                      <div className="flex items-center space-x-1">
                        <Clock className="h-4 w-4" />
                        <span>Finished {formatDate(entry.endDate)}</span>
                      </div>
                    )}
                  </div>
                  {duration && (
                    <div className="text-sm text-muted">
                      Duration: {duration}
                    </div>
                  )}
                </div>
              )}

              {/* Rating and Intensity */}
              <div className="space-y-2 mb-3">
                {entry.rating && (
                  <div>
                    <div className="text-xs text-muted mb-1">Mahoney Rating</div>
                    {renderRating(entry.rating)}
                  </div>
                )}
                {entry.intensity && entry.type === LifeLogType.HOBBY && (
                  <div>
                    <div className="text-xs text-muted mb-1">Intensity</div>
                    {renderIntensity(entry.intensity)}
                  </div>
                )}
              </div>

              {/* Key Takeaway */}
              {entry.keyTakeaway && !compact && (
                <div className="bg-blue-50 border-l-4 border-blue-400 p-3 rounded-r-lg mb-3">
                  <p className="text-xs text-blue-600 font-medium mb-1">Key Takeaway:</p>
                  <p className="text-sm text-blue-800 leading-relaxed">{entry.keyTakeaway}</p>
                </div>
              )}

              {/* Compact Key Takeaway */}
              {entry.keyTakeaway && compact && (
                <div className="mb-3">
                  <p className="text-xs text-muted mb-1">Key Takeaway:</p>
                  <p className="text-sm text-main line-clamp-2">{entry.keyTakeaway}</p>
                </div>
              )}
            </div>

            {/* Entry Image */}
            {entry.imageUrl && (
              <div className="flex-shrink-0 ml-4">
                <img
                  src={entry.imageUrl}
                  alt={entry.title}
                  className={`object-cover rounded-lg shadow-sm ${compact ? 'w-12 h-16' : 'w-16 h-20'
                    }`}
                  onError={(e) => {
                    (e.target as HTMLImageElement).style.display = 'none';
                  }}
                />
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-between pt-3 border-t border-border">
            <div className="text-xs text-muted">
              {entry.updatedAt && (
                <>Updated {formatDate(entry.updatedAt)}</>
              )}
            </div>
            <div className="flex items-center space-x-1">
              {entry.type === LifeLogType.BOOK && entry.metadata && (
                <button
                  onClick={() => setIsModalOpen(true)}
                  className="p-1 px-2 text-muted hover:text-primary transition-colors rounded-md hover:bg-page flex items-center space-x-1"
                  title="View Book Details"
                >
                  <Info className="h-3 w-3" />
                  <span className="text-xs">Info</span>
                </button>
              )}
              <button
                onClick={() => onEdit(entry)}
                className="flex items-center space-x-1 px-2 py-1 text-xs text-primary hover:text-primary/80 hover:bg-page rounded-md transition-colors"
              >
                <Edit className="h-3 w-3" />
                <span>Edit</span>
              </button>
              <button
                onClick={() => onDelete(entry)}
                className="flex items-center space-x-1 px-2 py-1 text-xs text-red-600 hover:text-red-700 hover:bg-red-50/10 rounded-md transition-colors"
              >
                <Trash2 className="h-3 w-3" />
                <span>Delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {entry.metadata && (
        <BookMetadataModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          metadataStr={entry.metadata}
        />
      )}
    </div>
  );
};

export default LifeLogEntryCard;