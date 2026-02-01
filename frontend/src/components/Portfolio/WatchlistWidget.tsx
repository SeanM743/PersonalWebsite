import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import { Plus, Trash2 } from 'lucide-react';

interface WatchlistItem {
    id: number;
    symbol: string;
    companyName?: string;
    currentPrice?: number;
    dailyChange?: number;
    dailyChangePercent?: number;
    marketOpen?: boolean;
}

interface WatchlistWidgetProps {
    onSelect?: (symbol: string) => void;
    selectedSymbol?: string;
    compact?: boolean;
}

const WatchlistWidget: React.FC<WatchlistWidgetProps> = ({ onSelect, selectedSymbol, compact = false }) => {
    const [items, setItems] = useState<WatchlistItem[]>([]);
    const [newSymbol, setNewSymbol] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchWatchlist = async () => {
        try {
            const res = await apiService.getWatchlist();
            if (res.success) {
                setItems(res.data || []);
            }
        } catch (err) {
            console.error('Failed to fetch watchlist', err);
        }
    };

    useEffect(() => {
        fetchWatchlist();
        const interval = setInterval(fetchWatchlist, 30000); // Refresh every 30s
        return () => clearInterval(interval);
    }, []);

    const handleAdd = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newSymbol.trim()) return;

        setIsLoading(true);
        setError(null);
        try {
            const res = await apiService.addToWatchlist(newSymbol.toUpperCase());
            if (res.success) {
                setNewSymbol('');
                setIsOpen(false);
                fetchWatchlist();
            } else {
                setError(res.message || 'Failed to add symbol');
            }
        } catch (err: any) {
            setError(err.response?.data?.message || 'Failed to add symbol');
        } finally {
            setIsLoading(false);
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await apiService.removeFromWatchlist(id);
            setItems(prev => prev.filter(item => item.id !== id));
        } catch (err) {
            console.error('Failed to remove item', err);
        }
    };

    return (
        <div className="bg-card rounded-lg p-6 shadow-sm border border-border h-full flex flex-col">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-main">Watchlist</h3>
                <button
                    onClick={() => setIsOpen(!isOpen)}
                    className={`p-1 rounded hover:bg-page transition-colors ${isOpen ? 'text-primary bg-primary/10' : 'text-muted hover:text-main'}`}
                >
                    <Plus className={`h-5 w-5 transition-transform ${isOpen ? 'rotate-45' : ''}`} />
                </button>
            </div>

            {isOpen && (
                <form onSubmit={handleAdd} className="mb-4">
                    <div className="flex space-x-2">
                        <input
                            type="text"
                            value={newSymbol}
                            onChange={(e) => setNewSymbol(e.target.value.toUpperCase())}
                            placeholder="Symbol (e.g. AAPL)"
                            className="flex-1 bg-page border border-border rounded px-3 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-primary uppercase"
                            autoFocus
                        />
                        <button
                            type="submit"
                            disabled={isLoading || !newSymbol.trim()}
                            className="bg-primary text-white px-3 py-1.5 rounded text-sm disabled:opacity-50"
                        >
                            {isLoading ? 'Adding...' : 'Add'}
                        </button>
                    </div>
                    {error && <div className="text-red-500 text-xs mt-1">{error}</div>}
                </form>
            )}

            <div className="flex-1 overflow-y-auto space-y-1 pr-1 custom-scrollbar">
                {items.length === 0 ? (
                    <div className="text-sm text-muted text-center py-8">
                        No symbols watched.
                    </div>
                ) : (
                    items.map(item => {
                        const dailyChange = item.dailyChange || 0;
                        const dailyChangePercent = item.dailyChangePercent || 0;
                        const isUp = dailyChange >= 0;
                        const changeColor = isUp ? 'text-green-500' : 'text-red-500';
                        const isSelected = selectedSymbol === item.symbol;

                        return (
                            <div
                                key={item.id}
                                onClick={() => onSelect?.(item.symbol)}
                                className={`group flex items-center justify-between p-2 rounded cursor-pointer transition-colors ${isSelected
                                    ? 'bg-primary/10 border border-primary/30'
                                    : 'hover:bg-page border border-transparent'
                                    }`}
                            >
                                <div className="flex-1 min-w-0 pr-3">
                                    <div className="font-bold text-main text-sm">{item.symbol}</div>
                                    <div className="text-xs text-muted truncate" title={item.companyName}>
                                        {item.companyName || ''}
                                    </div>
                                </div>
                                <div className="flex items-center space-x-2">
                                    <div className="text-right">
                                        <div className="text-sm font-medium text-main">
                                            {item.currentPrice != null ? `$${Number(item.currentPrice).toFixed(2)}` : 'N/A'}
                                        </div>
                                        <div className={`text-xs ${changeColor}`}>
                                            {dailyChange !== 0 && (
                                                <span>
                                                    {isUp ? '+' : ''}{dailyChange.toFixed(2)} ({isUp ? '+' : ''}{dailyChangePercent.toFixed(2)}%)
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDelete(item.id);
                                        }}
                                        className="text-muted hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity p-1"
                                    >
                                        <Trash2 className="h-4 w-4" />
                                    </button>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
};

export default WatchlistWidget;
