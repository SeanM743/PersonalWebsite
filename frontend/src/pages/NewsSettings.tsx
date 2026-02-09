import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, Trash2, Search, Pencil, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useNotification } from '../contexts/NotificationContext';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/UI/LoadingSpinner';

interface NewsCategory {
    id: number;
    topic: string;
    tab?: string;
    searchQuery?: string;
}

const TABS = ["Financial", "Sports", "Politics", "Entertainment", "Science", "Misc"];

const NewsSettings: React.FC = () => {
    const navigate = useNavigate();
    const { success, error } = useNotification();
    const [categories, setCategories] = useState<NewsCategory[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // Form State
    const [newTopic, setNewTopic] = useState('');
    const [newQuery, setNewQuery] = useState('');
    const [selectedTab, setSelectedTab] = useState('Misc');
    const [isAdding, setIsAdding] = useState(false);

    // Edit Modal State
    const [editingCategory, setEditingCategory] = useState<NewsCategory | null>(null);
    const [editTopic, setEditTopic] = useState('');
    const [editQuery, setEditQuery] = useState('');
    const [editTab, setEditTab] = useState('Misc');
    const [isEditing, setIsEditing] = useState(false);

    useEffect(() => {
        loadCategories();
    }, []);

    const loadCategories = async () => {
        try {
            setIsLoading(true);
            const data = await apiService.getCategories();
            if (Array.isArray(data)) {
                setCategories(data);
            } else if (data && data.categories) {
                // Fallback if structure is different
                setCategories(data.categories);
            }
        } catch (err: any) {
            error('Failed to load categories', err.message);
        } finally {
            setIsLoading(false);
        }
    };

    const handleAddCategory = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newTopic.trim()) return;

        try {
            setIsAdding(true);
            // Assuming apiService.addNewsCategory handles generic payload or updated args
            // We need to verify apiService.addNewsCategory signature or implementation
            // Since we can't easily change the type definition in a compiled file without seeing it,
            // we will assume we updated it or pass it as a generic post.
            // Actually, we should update apiService.ts to accept 'tab' argument.

            await apiService.addNewsCategory(newTopic, newQuery, selectedTab);

            success(`Added "${newTopic}" to ${selectedTab}`);
            setNewTopic('');
            setNewQuery('');
            setSelectedTab('Misc');
            await loadCategories();
        } catch (err: any) {
            error('Failed to add topic', err.message);
        } finally {
            setIsAdding(false);
        }
    };

    const handleDeleteCategory = async (id: number) => {
        if (!window.confirm('Are you sure you want to delete this topic?')) return;
        try {
            await apiService.deleteNewsCategory(id);
            setCategories(prev => prev.filter(c => c.id !== id));
            success('Topic deleted');
        } catch (err: any) {
            error('Failed to delete topic', err.message);
        }
    };

    const openEditModal = (category: NewsCategory) => {
        setEditingCategory(category);
        setEditTopic(category.topic);
        setEditQuery(category.searchQuery || '');
        setEditTab(category.tab || 'Misc');
    };

    const closeEditModal = () => {
        setEditingCategory(null);
        setEditTopic('');
        setEditQuery('');
        setEditTab('Misc');
    };

    const handleSaveEdit = async () => {
        if (!editingCategory || !editTopic.trim()) return;

        try {
            setIsEditing(true);
            await apiService.updateNewsCategory(
                editingCategory.id,
                editTopic.trim(),
                editQuery.trim() || undefined,
                editTab
            );
            success(`Updated "${editTopic}"`);
            closeEditModal();
            await loadCategories();
        } catch (err: any) {
            error('Failed to update topic', err.message);
        } finally {
            setIsEditing(false);
        }
    };

    // Sort categories by tab for consistent display
    const sortedCategories = [...categories].sort((a, b) => {
        const tabOrder = ['Financial', 'Sports', 'Politics', 'Misc'];
        const aOrder = tabOrder.indexOf(a.tab || 'Misc');
        const bOrder = tabOrder.indexOf(b.tab || 'Misc');
        if (aOrder !== bOrder) return aOrder - bOrder;
        return a.topic.localeCompare(b.topic);
    });

    return (
        <div className="min-h-screen bg-page text-foreground p-4 md:p-8 animate-fade-in font-sans">
            <div className="max-w-3xl mx-auto">
                {/* Header */}
                <div className="flex items-center gap-4 mb-8">
                    <button
                        onClick={() => navigate('/news')}
                        className="p-2 hover:bg-card rounded-full transition-colors"
                    >
                        <ArrowLeft size={24} />
                    </button>
                    <div>
                        <h1 className="text-3xl font-serif font-bold">Manage Interests</h1>
                        <p className="text-muted">Customize your news feed topics.</p>
                    </div>
                </div>

                {/* Add Topic Form */}
                <div className="bg-card rounded-xl border border-border p-6 shadow-sm mb-8">
                    <h2 className="text-xl font-bold mb-4 flex items-center gap-2">
                        <Plus size={20} className="text-primary" />
                        Add New Topic
                    </h2>
                    <form onSubmit={handleAddCategory} className="space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-muted mb-1">Topic Name</label>
                                <input
                                    type="text"
                                    value={newTopic}
                                    onChange={(e) => setNewTopic(e.target.value)}
                                    placeholder="e.g. NVIDIA, Manchester United"
                                    className="w-full px-4 py-2 bg-page border border-border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all"
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-muted mb-1">Category Tab</label>
                                <select
                                    value={selectedTab}
                                    onChange={(e) => setSelectedTab(e.target.value)}
                                    className="w-full px-4 py-2 bg-page border border-border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all appearance-none"
                                >
                                    {TABS.map(tab => (
                                        <option key={tab} value={tab}>{tab}</option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-muted mb-1">
                                Search Query <span className="text-xs text-muted/60">(Optional - for advanced refinement)</span>
                            </label>
                            <div className="relative">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted h-4 w-4" />
                                <input
                                    type="text"
                                    value={newQuery}
                                    onChange={(e) => setNewQuery(e.target.value)}
                                    placeholder="e.g. (NVDA OR 'Jensen Huang') AND earnings"
                                    className="w-full pl-10 pr-4 py-2 bg-page border border-border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all font-mono text-sm"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end">
                            <button
                                type="submit"
                                disabled={isAdding || !newTopic.trim()}
                                className="px-6 py-2 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {isAdding ? <LoadingSpinner size="small" /> : <Plus size={18} />}
                                Add Topic
                            </button>
                        </div>
                    </form>
                </div>

                {/* Categories List */}
                <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
                    <div className="p-6 border-b border-border">
                        <h2 className="text-xl font-bold">Your Topics</h2>
                    </div>
                    {isLoading ? (
                        <div className="p-8 flex justify-center">
                            <LoadingSpinner />
                        </div>
                    ) : sortedCategories.length === 0 ? (
                        <div className="p-8 text-center text-muted">
                            No topics added yet. Add one above to get started!
                        </div>
                    ) : (
                        <div className="divide-y divide-border">
                            {sortedCategories.map(category => (
                                <div key={category.id} className="p-4 flex items-center justify-between hover:bg-page/50 transition-colors">
                                    <div className="flex items-center gap-4">
                                        <span className={`px-2 py-1 rounded text-xs font-bold uppercase tracking-wider border
                                            ${category.tab === 'Financial' ? 'bg-green-100/10 text-green-600 border-green-200/20' :
                                                category.tab === 'Sports' ? 'bg-orange-100/10 text-orange-600 border-orange-200/20' :
                                                    category.tab === 'Politics' ? 'bg-blue-100/10 text-blue-600 border-blue-200/20' :
                                                        category.tab === 'Entertainment' ? 'bg-purple-100/10 text-purple-600 border-purple-200/20' :
                                                            category.tab === 'Science' ? 'bg-cyan-100/10 text-cyan-600 border-cyan-200/20' :
                                                                'bg-gray-100/10 text-gray-600 border-gray-200/20'}`}
                                        >
                                            {category.tab || 'Misc'}
                                        </span>
                                        <div>
                                            <h3 className="font-semibold">{category.topic}</h3>
                                            {category.searchQuery && category.searchQuery !== category.topic && (
                                                <p className="text-xs text-muted font-mono mt-0.5 max-w-md truncate">
                                                    Query: {category.searchQuery}
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <button
                                            onClick={() => openEditModal(category)}
                                            className="p-2 text-muted hover:text-primary hover:bg-primary/10 rounded-lg transition-colors"
                                            title="Edit Topic"
                                        >
                                            <Pencil size={18} />
                                        </button>
                                        <button
                                            onClick={() => handleDeleteCategory(category.id)}
                                            className="p-2 text-muted hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                            title="Delete Topic"
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Edit Modal */}
            {editingCategory && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in">
                    <div className="bg-card rounded-xl border border-border shadow-xl w-full max-w-md mx-4 p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="text-xl font-bold">Edit Topic</h2>
                            <button onClick={closeEditModal} className="p-1 hover:bg-page rounded-lg transition-colors">
                                <X size={20} />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-muted mb-1">Topic Name</label>
                                <input
                                    type="text"
                                    value={editTopic}
                                    onChange={(e) => setEditTopic(e.target.value)}
                                    className="w-full px-4 py-2 bg-page border border-border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-muted mb-1">Category Tab</label>
                                <select
                                    value={editTab}
                                    onChange={(e) => setEditTab(e.target.value)}
                                    className="w-full px-4 py-2 bg-page border border-border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all appearance-none"
                                >
                                    {TABS.map(tab => (
                                        <option key={tab} value={tab}>{tab}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-muted mb-1">Search Query</label>
                                <input
                                    type="text"
                                    value={editQuery}
                                    onChange={(e) => setEditQuery(e.target.value)}
                                    placeholder="Custom NewsAPI query (optional)"
                                    className="w-full px-4 py-2 bg-page border border-border rounded-lg focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all font-mono text-sm"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end gap-3 mt-6">
                            <button
                                onClick={closeEditModal}
                                className="px-4 py-2 text-muted hover:bg-page rounded-lg transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleSaveEdit}
                                disabled={isEditing || !editTopic.trim()}
                                className="px-4 py-2 bg-primary text-white rounded-lg font-medium hover:bg-primary-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {isEditing ? <LoadingSpinner size="small" /> : null}
                                Save Changes
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default NewsSettings;
