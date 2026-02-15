import React, { useState, useEffect, useMemo } from 'react';
import { useNotification } from '../contexts/NotificationContext';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { RefreshCw, ChevronDown, ChevronUp, Newspaper, X, Settings } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface NewsArticle {
    id: number;
    title: string;
    summary: string;
    content: string;
    url: string;
    source: string;
    imageUrl: string;
    publishedAt: string;
    relevanceScore: number;
}

interface DisplayArticle extends NewsArticle {
    topic: string;
    tab: string;     // Added tab to article for filtering
}

interface NewsCategory {
    id: number;
    topic: string;
    tab?: string;    // Tab assigned to category
    lastFetchedAt: string;
    articles: NewsArticle[];
}

// Fixed set of tabs as per requirement, but populated by user topics
const STATIC_TABS = ["Financial", "Sports", "Politics", "Entertainment", "Science", "Misc"];

const NewsPage: React.FC = () => {
    const navigate = useNavigate();
    const [categories, setCategories] = useState<NewsCategory[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [selectedTab, setSelectedTab] = useState("Financial");
    const { success, error } = useNotification();
    const [expandedArticleId, setExpandedArticleId] = useState<number | null>(null);

    useEffect(() => {
        loadNews(true);
        const interval = setInterval(() => {
            setCategories(prev => {
                if (prev.some(c => !c.articles || c.articles.length === 0)) {
                    loadNews(false);
                }
                return prev;
            });
        }, 30000); // 30s check
        return () => clearInterval(interval);
    }, []);

    const loadNews = async (initialLoad: boolean = false) => {
        try {
            if (initialLoad) setIsLoading(true);
            const data = await apiService.getNews();
            if (data && data.categories) {
                setCategories(data.categories || []);
            }
        } catch (err: any) {
            if (initialLoad && !err.message?.includes('404')) error('Failed to load news', err.message);
        } finally {
            if (initialLoad) setIsLoading(false);
        }
    };

    const handleRefresh = async () => {
        try {
            setIsRefreshing(true);
            await apiService.refreshNews(true);
            await loadNews(false);
            success('News refreshed');
        } catch (err: any) {
            error('Failed to refresh news', err.message);
        } finally {
            setIsRefreshing(false);
        }
    };

    const handleDeleteArticle = async (articleId: number, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!window.confirm("Remove this article?")) return;
        try {
            await apiService.deleteNewsArticle(articleId);
            setCategories(prev => prev.map(cat => ({
                ...cat,
                articles: cat.articles?.filter(a => a.id !== articleId)
            })));
            success("Article removed");
        } catch (err: any) {
            error("Failed to delete article", err.message);
        }
    };

    const toggleArticle = (id: number) => {
        setExpandedArticleId(expandedArticleId === id ? null : id);
    };

    // Flatten all articles with metadata
    const allArticles = useMemo(() => {
        return categories.flatMap(c =>
            (c.articles || []).map(a => ({
                ...a,
                topic: c.topic,
                tab: c.tab || 'Misc'
            }))
        );
    }, [categories]);

    // Filter by Tab and Sort by Relevance Score
    const displayedArticles = useMemo(() => {
        // 1. Filter by Tab
        const filtered = allArticles.filter(a => a.tab === selectedTab);

        // 2. Sort by relevance score (descending), then by date (descending)
        const sorted = [...filtered].sort((a, b) => {
            // Primary: relevance score (higher first)
            const scoreDiff = (b.relevanceScore || 5) - (a.relevanceScore || 5);
            if (scoreDiff !== 0) return scoreDiff;

            // Secondary: date (newer first)
            return new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime();
        });

        // 3. Deduplicate
        const unique = new Map();
        const result: DisplayArticle[] = [];
        sorted.forEach(a => {
            if (!unique.has(a.id)) {
                unique.set(a.id, true);
                result.push(a);
            }
        });

        return result;
    }, [allArticles, selectedTab]);

    if (isLoading) {
        return (
            <div className="flex justify-center items-center h-screen bg-page">
                <LoadingSpinner size="large" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-page text-foreground p-4 md:p-8 animate-fade-in font-sans">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <header className="mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-card p-4 rounded-xl border border-border shadow-sm">
                    <div>
                        <h1 className="text-3xl font-serif font-bold text-foreground mb-1 flex items-center gap-2">
                            <Newspaper className="h-8 w-8 text-primary" />
                            Daily Briefing
                        </h1>
                        <p className="text-muted text-sm">Curated AI summaries for your interests.</p>
                    </div>
                    <div className="flex items-center gap-4 w-full md:w-auto">
                        <div className="flex bg-page p-1 rounded-lg border border-border flex-1 md:flex-none overflow-x-auto scrollbar-hide">
                            {STATIC_TABS.map(tab => (
                                <button
                                    key={tab}
                                    onClick={() => setSelectedTab(tab)}
                                    className={`px-4 py-2 rounded-md text-sm font-medium transition-all whitespace-nowrap ${selectedTab === tab
                                        ? 'bg-primary text-white shadow-sm'
                                        : 'text-muted hover:text-foreground hover:bg-card'
                                        }`}
                                >
                                    {tab}
                                </button>
                            ))}
                        </div>
                        <div className="flex items-center gap-2 border-l border-border pl-4">
                            <button
                                onClick={handleRefresh}
                                className={`p-2 rounded-full bg-page border border-border hover:text-primary transition-colors shrink-0 ${isRefreshing ? 'animate-spin' : ''}`}
                                title="Refresh News"
                            >
                                <RefreshCw size={20} />
                            </button>
                            <button
                                onClick={() => navigate('/news/settings')}
                                className="p-2 rounded-full bg-page border border-border hover:text-primary transition-colors shrink-0"
                                title="Manage Topics"
                            >
                                <Settings size={20} />
                            </button>
                        </div>
                    </div>
                </header>

                {/* Main Grid */}
                {displayedArticles.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {displayedArticles.map((article) => (
                            <article
                                key={article.id}
                                className="group bg-card rounded-2xl overflow-hidden border border-border shadow-sm hover:shadow-md transition-all duration-300 flex flex-col h-full"
                            >
                                {/* Image & Topic Badge */}
                                <div className="relative h-48 overflow-hidden shrink-0">
                                    {article.imageUrl ? (
                                        <img
                                            src={article.imageUrl}
                                            alt={article.title}
                                            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105"
                                            onError={(e) => { (e.target as HTMLImageElement).src = `https://source.unsplash.com/random/800x600?${article.topic}`; }}
                                        />
                                    ) : (
                                        <div className="w-full h-full bg-gray-200 flex items-center justify-center">
                                            <Newspaper className="text-gray-400" size={48} />
                                        </div>
                                    )}

                                    <div className="absolute top-2 left-2 px-2 py-1 bg-black/60 backdrop-blur-md rounded text-[10px] font-bold text-white uppercase tracking-wider">
                                        {article.topic}
                                    </div>

                                    <button
                                        onClick={(e) => handleDeleteArticle(article.id, e)}
                                        className="absolute top-2 right-2 p-1.5 bg-black/50 text-white rounded-full opacity-0 group-hover:opacity-100 hover:bg-red-500 transition-all backdrop-blur-sm"
                                        title="Remove article"
                                    >
                                        <X size={14} />
                                    </button>
                                </div>

                                <div className="p-5 flex flex-col flex-1">
                                    <div className="flex justify-between items-center mb-2">
                                        <span className="text-xs font-medium text-primary uppercase line-clamp-1">{article.source || "Unknown"}</span>
                                        <span className="text-xs text-muted whitespace-nowrap ml-2">{new Date(article.publishedAt).toLocaleDateString()}</span>
                                    </div>

                                    <h3 className="font-serif font-bold text-lg mb-2 line-clamp-2 leading-tight">
                                        <a href={article.url} target="_blank" rel="noreferrer" className="hover:text-primary transition-colors">{article.title}</a>
                                    </h3>

                                    <p className="text-muted text-sm line-clamp-3 mb-4">{article.summary}</p>

                                    <div className="mt-auto pt-4 border-t border-border flex justify-between items-center">
                                        <button
                                            onClick={() => toggleArticle(article.id)}
                                            className="text-xs font-bold text-primary hover:text-primary-dark uppercase tracking-wider flex items-center gap-1"
                                        >
                                            {expandedArticleId === article.id ? 'Close' : 'Analysis'}
                                            {expandedArticleId === article.id ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                                        </button>
                                    </div>

                                    <div className={`grid transition-[grid-template-rows] duration-500 ease-out ${expandedArticleId === article.id ? 'grid-rows-[1fr] mt-4' : 'grid-rows-[0fr]'}`}>
                                        <div className="overflow-hidden">
                                            <div className="p-3 bg-page rounded-lg text-sm text-foreground/90 leading-relaxed font-sans">
                                                {article.content}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </article>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-20 bg-card rounded-2xl border border-border border-dashed">
                        {categories.filter(c => c.tab === selectedTab).length === 0 ? (
                            <div className="space-y-4">
                                <div className="mx-auto w-16 h-16 bg-muted/20 rounded-full flex items-center justify-center text-muted">
                                    <Settings size={32} />
                                </div>
                                <h3 className="text-xl font-serif text-muted">Empty Section</h3>
                                <p className="text-muted/60 max-w-md mx-auto">
                                    You haven't added any topics to <strong>{selectedTab}</strong> yet.
                                    Click the gear icon to customize your feed.
                                </p>
                                <button
                                    onClick={() => navigate('/news/settings')}
                                    className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors inline-flex items-center gap-2"
                                >
                                    <PlusIcon size={18} />
                                    Add {selectedTab} Topic
                                </button>
                            </div>
                        ) : (
                            <div className="space-y-4">
                                <h3 className="text-xl font-serif text-muted">No news available</h3>
                                <p className="text-muted/60">We couldn't find fresh articles for your topics.</p>
                                <button onClick={handleRefresh} className="text-primary hover:underline">Try Refreshing</button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

// Helper for empty state button
const PlusIcon = ({ size }: { size: number }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
    </svg>
);

export default NewsPage;
