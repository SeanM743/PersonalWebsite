import React from 'react';
import { X, Book, Film, Music, MapPin, Calendar, ExternalLink } from 'lucide-react';

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

interface QuickFactMetadataModalProps {
  isOpen: boolean;
  onClose: () => void;
  quickFact: QuickFact;
}

const QuickFactMetadataModal: React.FC<QuickFactMetadataModalProps> = ({
  isOpen,
  onClose,
  quickFact
}) => {
  if (!isOpen) return null;

  // Parse metadata JSON
  let parsedMetadata: Record<string, any> = {};
  try {
    if (quickFact.metadata) {
      parsedMetadata = JSON.parse(quickFact.metadata);
    }
  } catch (error) {
    console.error('Failed to parse metadata:', error);
  }

  // Get category icon
  const getCategoryIcon = (category: string) => {
    switch (category.toLowerCase()) {
      case 'reading':
      case 'book':
        return <Book className="h-5 w-5" />;
      case 'recently watched':
      case 'movie':
      case 'film':
        return <Film className="h-5 w-5" />;
      case 'listening to':
      case 'music':
        return <Music className="h-5 w-5" />;
      case 'next trip':
      case 'location':
        return <MapPin className="h-5 w-5" />;
      default:
        return <Calendar className="h-5 w-5" />;
    }
  };

  // Format metadata based on category
  const formatMetadataForCategory = (category: string, metadata: Record<string, any>) => {
    const formatted: Array<{ label: string; value: string; important?: boolean }> = [];

    switch (category.toLowerCase()) {
      case 'reading':
      case 'book':
        if (metadata.title) formatted.push({ label: 'Title', value: metadata.title, important: true });
        if (metadata.author) formatted.push({ label: 'Author', value: metadata.author, important: true });
        if (metadata.isbn) formatted.push({ label: 'ISBN', value: metadata.isbn });
        if (metadata.publishYear) formatted.push({ label: 'Published', value: metadata.publishYear.toString() });
        if (metadata.publisher) formatted.push({ label: 'Publisher', value: metadata.publisher });
        break;

      case 'recently watched':
      case 'movie':
      case 'film':
        if (metadata.title) formatted.push({ label: 'Title', value: metadata.title, important: true });
        if (metadata.director) formatted.push({ label: 'Director', value: metadata.director, important: true });
        if (metadata.releaseDate) formatted.push({ label: 'Release Date', value: metadata.releaseDate });
        if (metadata.rating) formatted.push({ label: 'Rating', value: `${metadata.rating}/10` });
        if (metadata.overview) formatted.push({ label: 'Overview', value: metadata.overview });
        break;

      case 'listening to':
      case 'music':
        if (metadata.song) formatted.push({ label: 'Song', value: metadata.song, important: true });
        if (metadata.artist) formatted.push({ label: 'Artist', value: metadata.artist, important: true });
        if (metadata.album) formatted.push({ label: 'Album', value: metadata.album });
        if (metadata.releaseYear) formatted.push({ label: 'Release Year', value: metadata.releaseYear.toString() });
        break;

      case 'next trip':
      case 'location':
        if (metadata.location) formatted.push({ label: 'Location', value: metadata.location, important: true });
        if (metadata.country) formatted.push({ label: 'Country', value: metadata.country });
        if (metadata.coordinates) formatted.push({ label: 'Coordinates', value: metadata.coordinates });
        break;

      default:
        // For general categories, display all metadata
        Object.entries(metadata).forEach(([key, value]) => {
          if (value && typeof value === 'string' || typeof value === 'number') {
            formatted.push({
              label: key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase()),
              value: value.toString()
            });
          }
        });
        break;
    }

    return formatted;
  };

  const formattedMetadata = formatMetadataForCategory(quickFact.category, parsedMetadata);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 max-h-[80vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white">
                {getCategoryIcon(quickFact.category)}
              </div>
              <div>
                <h2 className="text-lg font-semibold text-gray-900">{quickFact.key}</h2>
                <p className="text-sm text-gray-500 capitalize">{quickFact.category}</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          {/* Cover Image */}
          {quickFact.imageUrl && (
            <div className="mb-4 flex justify-center">
              <img
                src={quickFact.imageUrl}
                alt={quickFact.value}
                className="max-w-32 max-h-48 object-cover rounded-lg shadow-md"
                onError={(e) => {
                  (e.target as HTMLImageElement).style.display = 'none';
                }}
              />
            </div>
          )}

          {/* Basic Info */}
          <div className="mb-4">
            <div className="bg-gray-50 rounded-lg p-3">
              <h3 className="text-sm font-medium text-gray-700 mb-2">Current Value</h3>
              <p className="text-gray-900">{quickFact.value}</p>
            </div>
          </div>

          {/* Enriched Metadata */}
          {formattedMetadata.length > 0 && (
            <div className="mb-4">
              <h3 className="text-sm font-medium text-gray-700 mb-3">Enriched Details</h3>
              <div className="space-y-2">
                {formattedMetadata.map((item, index) => (
                  <div key={index} className="flex justify-between items-start">
                    <span className={`text-sm ${item.important ? 'font-medium text-gray-700' : 'text-gray-500'}`}>
                      {item.label}:
                    </span>
                    <span className={`text-sm text-right ml-2 ${item.important ? 'font-medium text-gray-900' : 'text-gray-700'}`}>
                      {item.value}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* External Link */}
          {quickFact.sourceUrl && (
            <div className="mb-4">
              <a
                href={quickFact.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center space-x-2 text-blue-600 hover:text-blue-800 text-sm"
              >
                <ExternalLink className="h-4 w-4" />
                <span>View on External Site</span>
              </a>
            </div>
          )}

          {/* Footer */}
          <div className="pt-4 border-t border-gray-200">
            <div className="flex items-center justify-between text-xs text-gray-500">
              <span>
                {quickFact.isEnriched ? '✓ Enhanced with external data' : '• Basic information only'}
              </span>
              <span>
                Updated {new Date(quickFact.updatedAt).toLocaleDateString()}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default QuickFactMetadataModal;