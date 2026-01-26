import React from 'react';
import { X, ExternalLink, Calendar, User, BookOpen, ShoppingBag } from 'lucide-react';

interface BookMetadata {
    title: string;
    author_name?: string[];
    isbn?: string | string[];
    cover_i?: string | number;
    coverUrl?: string; // Unified URL from backend
    publisher?: string | string[];
    first_publish_year?: number;
    description?: string | { value: string }; // OpenLibrary sometimes returns object
    subject?: string[];
    number_of_pages_median?: number;
}

interface BookMetadataModalProps {
    isOpen: boolean;
    onClose: () => void;
    metadataStr: string;
}

const BookMetadataModal: React.FC<BookMetadataModalProps> = ({ isOpen, onClose, metadataStr }) => {
    if (!isOpen) return null;

    let metadata: BookMetadata | null = null;
    try {
        metadata = JSON.parse(metadataStr);
    } catch (e) {
        console.error("Failed to parse book metadata", e);
        return null;
    }

    if (!metadata) return null;

    // Helper to extract description text
    const getDescription = () => {
        if (!metadata?.description) return null;
        if (typeof metadata.description === 'string') return metadata.description;
        return metadata.description.value || null;
    };

    // Helper to get Amazon link
    const getAmazonLink = () => {
        const isbn = Array.isArray(metadata?.isbn) ? metadata?.isbn[0] : metadata?.isbn;
        if (isbn) {
            return `https://www.amazon.com/s?k=${isbn}`;
        }
        return `https://www.amazon.com/s?k=${encodeURIComponent(metadata?.title || '')}`;
    };

    const authors = metadata.author_name?.join(', ') || 'Unknown Author';
    const description = getDescription();
    const amazonLink = getAmazonLink();

    // Use the coverUrl from our backend if available (it handles logic), or construct it
    const coverUrl = metadata.coverUrl ||
        (metadata.cover_i ? `https://covers.openlibrary.org/b/id/${metadata.cover_i}-L.jpg` : null);

    const publisher = Array.isArray(metadata.publisher) ? metadata.publisher[0] : metadata.publisher;
    const isbn = Array.isArray(metadata.isbn) ? metadata.isbn[0] : metadata.isbn;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 animate-fadeIn">
            <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
                {/* Header with Close Button */}
                <div className="flex justify-between items-start p-4 border-b border-gray-100">
                    <h2 className="text-xl font-bold text-gray-900 line-clamp-1 pr-4" title={metadata.title}>
                        {metadata.title}
                    </h2>
                    <button
                        onClick={onClose}
                        className="p-1 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors"
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                {/* content - Scrollable */}
                <div className="overflow-y-auto p-6 flex-1">
                    <div className="flex flex-col md:flex-row gap-6">
                        {/* Left Column: Cover Image */}
                        <div className="flex-shrink-0 mx-auto md:mx-0">
                            {coverUrl ? (
                                <div className="w-48 h-72 shadow-lg rounded-lg overflow-hidden bg-gray-100">
                                    <img
                                        src={coverUrl}
                                        alt={`Cover of ${metadata.title}`}
                                        className="w-full h-full object-cover"
                                        onError={(e) => {
                                            (e.target as HTMLImageElement).src = 'https://placehold.co/192x288?text=No+Cover';
                                        }}
                                    />
                                </div>
                            ) : (
                                <div className="w-48 h-72 shadow-lg rounded-lg bg-gray-200 flex items-center justify-center flex-col text-gray-400 p-4 text-center">
                                    <BookOpen className="h-12 w-12 mb-2" />
                                    <span className="text-sm">No Cover Available</span>
                                </div>
                            )}
                        </div>

                        {/* Right Column: Details */}
                        <div className="flex-1 space-y-4">
                            {/* Key Details Grid */}
                            <div className="grid grid-cols-1 gap-3">
                                <div className="flex items-start">
                                    <User className="h-5 w-5 text-gray-400 mt-0.5 mr-3 flex-shrink-0" />
                                    <div>
                                        <p className="text-sm font-medium text-gray-500">Author</p>
                                        <p className="text-gray-900 font-medium">{authors}</p>
                                    </div>
                                </div>

                                {metadata.first_publish_year && (
                                    <div className="flex items-start">
                                        <Calendar className="h-5 w-5 text-gray-400 mt-0.5 mr-3 flex-shrink-0" />
                                        <div>
                                            <p className="text-sm font-medium text-gray-500">First Published</p>
                                            <p className="text-gray-900">{metadata.first_publish_year}</p>
                                        </div>
                                    </div>
                                )}

                                {publisher && (
                                    <div className="flex items-start">
                                        <BookOpen className="h-5 w-5 text-gray-400 mt-0.5 mr-3 flex-shrink-0" />
                                        <div>
                                            <p className="text-sm font-medium text-gray-500">Publisher</p>
                                            <p className="text-gray-900">{publisher}</p>
                                        </div>
                                    </div>
                                )}

                                {metadata.number_of_pages_median && (
                                    <div className="flex items-start">
                                        <BookOpen className="h-5 w-5 text-gray-400 mt-0.5 mr-3 flex-shrink-0" />
                                        <div>
                                            <p className="text-sm font-medium text-gray-500">Pages</p>
                                            <p className="text-gray-900">{metadata.number_of_pages_median}</p>
                                        </div>
                                    </div>
                                )}

                                {isbn && (
                                    <div className="flex items-start">
                                        <div className="h-5 w-5 flex items-center justify-center mr-3 mt-0.5 text-gray-400 border border-gray-400 rounded-sm text-[10px] font-bold">#</div>
                                        <div>
                                            <p className="text-sm font-medium text-gray-500">ISBN</p>
                                            <p className="text-gray-900 font-mono text-sm">{isbn}</p>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Subjects */}
                            {metadata.subject && metadata.subject.length > 0 && (
                                <div className="mt-2 pt-2 border-t border-gray-100">
                                    <p className="text-xs font-medium text-gray-500 mb-2">Subjects</p>
                                    <div className="flex flex-wrap gap-1.5">
                                        {metadata.subject.slice(0, 8).map((subject, index) => (
                                            <span
                                                key={index}
                                                className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700"
                                            >
                                                {subject}
                                            </span>
                                        ))}
                                        {metadata.subject.length > 8 && (
                                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600">
                                                +{metadata.subject.length - 8} more
                                            </span>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Description */}
                            {description && (
                                <div className="mt-4 pt-4 border-t border-gray-100">
                                    <h3 className="text-sm font-bold text-gray-900 mb-2">Description</h3>
                                    <p className="text-gray-600 text-sm leading-relaxed whitespace-pre-line">
                                        {description}
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Footer Actions */}
                <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end">
                    <a
                        href={amazonLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center space-x-2 px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-lg hover:from-amber-600 hover:to-orange-600 transition-colors shadow-sm font-medium"
                    >
                        <ShoppingBag className="h-4 w-4" />
                        <span>View on Amazon</span>
                        <ExternalLink className="h-3 w-3 ml-1 opacity-70" />
                    </a>
                </div>
            </div>
        </div>
    );
};

export default BookMetadataModal;
