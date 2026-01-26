import React, { useState } from 'react';
import { X } from 'lucide-react';

interface NewUpdateModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: () => void;
}

const NewUpdateModal: React.FC<NewUpdateModalProps> = ({ isOpen, onClose, onSave }) => {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [platform, setPlatform] = useState('Personal');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!title.trim() || !content.trim()) {
            alert('Please fill in all fields');
            return;
        }

        setIsSubmitting(true);
        try {
            const { apiService } = await import('../services/apiService');
            await apiService.createPost({
                title: title.trim(),
                content: content.trim(),
                platform: platform
            });

            // Reset form
            setTitle('');
            setContent('');
            setPlatform('Personal');

            // Notify parent and close
            onSave();
            onClose();
        } catch (error) {
            console.error('Failed to create update:', error);
            alert('Failed to create update. Please try again.');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleClose = () => {
        if (!isSubmitting) {
            setTitle('');
            setContent('');
            setPlatform('Personal');
            onClose();
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-card rounded-lg w-full max-w-2xl shadow-xl max-h-[90vh] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b border-border sticky top-0 bg-card">
                    <h2 className="text-xl font-semibold text-main">New Update</h2>
                    <button
                        onClick={handleClose}
                        disabled={isSubmitting}
                        className="text-muted hover:text-main transition-colors disabled:opacity-50"
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-6 space-y-6">
                    {/* Platform Selection */}
                    <div>
                        <label className="block text-sm font-medium text-main mb-2">
                            Platform
                        </label>
                        <select
                            value={platform}
                            onChange={(e) => setPlatform(e.target.value)}
                            disabled={isSubmitting}
                            className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary bg-page text-main disabled:opacity-50"
                        >
                            <option value="Personal">Personal</option>
                            <option value="Twitter">Twitter</option>
                            <option value="LinkedIn">LinkedIn</option>
                            <option value="Instagram">Instagram</option>
                            <option value="Facebook">Facebook</option>
                        </select>
                    </div>

                    {/* Title */}
                    <div>
                        <label className="block text-sm font-medium text-main mb-2">
                            Title <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            disabled={isSubmitting}
                            placeholder="Enter update title..."
                            className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary bg-page text-main placeholder-muted disabled:opacity-50"
                            maxLength={200}
                        />
                        <div className="text-xs text-muted mt-1 text-right">
                            {title.length}/200
                        </div>
                    </div>

                    {/* Content */}
                    <div>
                        <label className="block text-sm font-medium text-main mb-2">
                            Content <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            disabled={isSubmitting}
                            placeholder="What's on your mind?"
                            rows={8}
                            className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary bg-page text-main placeholder-muted resize-none disabled:opacity-50"
                            maxLength={5000}
                        />
                        <div className="text-xs text-muted mt-1 text-right">
                            {content.length}/5000
                        </div>
                    </div>

                    {/* Actions */}
                    <div className="flex justify-end space-x-3 pt-4 border-t border-border">
                        <button
                            type="button"
                            onClick={handleClose}
                            disabled={isSubmitting}
                            className="px-6 py-2 text-muted hover:text-main transition-colors disabled:opacity-50"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting || !title.trim() || !content.trim()}
                            className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
                        >
                            {isSubmitting ? (
                                <>
                                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                    <span>Creating...</span>
                                </>
                            ) : (
                                <span>Create Update</span>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default NewUpdateModal;
