import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import { Plus, Trash2, ArrowUpRight, ArrowDownRight } from 'lucide-react';

interface WatchlistItem {
    id: number;
    symbol: string;
    currentPrice?: number;
    changePercent?: number;
}

const WatchlistWidget: React.FC = () => {
    const [items, setItems] = useState<WatchlistItem[]>([]);
    const [newSymbol, setNewSymbol] = useState('');
    const [isOpen, setIsOpen] = useState(false);

    // Mock data for now since we don't have live market data for everything yet
    // In real app, we'd fetch prices.
    useEffect(() => {
        // apiService.getWatchlist().then(...)
        // checking if I added getWatchlist to apiService... I did create Controller but maybe not FE service method?
        // I need to check apiService.ts. 
        // I forgot to add getWatchlist to apiService.ts!
        // I will add it shortly.
    }, []);

    return (
        <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-main">Watchlist</h3>
                <button className="p-1 rounded hover:bg-page text-muted hover:text-main">
                    <Plus className="h-5 w-5" />
                </button>
            </div>

            <div className="space-y-3">
                <div className="text-sm text-muted text-center py-4">
                    Watchlist feature coming soon...
                </div>
            </div>
        </div>
    );
};

export default WatchlistWidget;
