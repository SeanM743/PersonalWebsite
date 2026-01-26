import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import { Wallet, RotateCcw, Plus, Calculator } from 'lucide-react';
import LoadingSpinner from '../UI/LoadingSpinner';

interface PaperTransaction {
    id: number;
    symbol: string;
    type: 'BUY' | 'SELL';
    quantity: number;
    pricePerShare: number;
    transactionDate: string;
}

const SandboxTab: React.FC = () => {
    const [transactions, setTransactions] = useState<PaperTransaction[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    // Form State
    const [symbol, setSymbol] = useState('');
    const [type, setType] = useState<'BUY' | 'SELL'>('BUY');
    const [quantity, setQuantity] = useState('');
    const [price, setPrice] = useState('');

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        setIsLoading(true);
        try {
            const res = await apiService.getPaperTransactions();
            if (res.success) setTransactions(res.data);
        } catch (e) {
            console.error(e);
        } finally {
            setIsLoading(false);
        }
    };

    const handleTrade = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const res = await apiService.addPaperTransaction({
                symbol: symbol.toUpperCase(),
                type,
                quantity: parseFloat(quantity),
                pricePerShare: parseFloat(price)
            });
            if (res.success) {
                setTransactions([res.data, ...transactions]);
                setSymbol('');
                setQuantity('');
                setPrice('');
            }
        } catch (e) {
            console.error(e);
        }
    };

    const handleReset = async () => {
        if (!confirm('Clear all paper trades?')) return;
        await apiService.resetPaperPortfolio();
        setTransactions([]);
    };

    // Calculate Fake Portfolio Value
    const portfolioValue = transactions.reduce((acc, t) => {
        const val = t.quantity * t.pricePerShare;
        return t.type === 'BUY' ? acc + val : acc - val; // Crude cost basis calc
    }, 0);

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between bg-card p-4 rounded-lg border border-border border-dashed">
                <div className="flex items-center space-x-4">
                    <div className="p-3 bg-purple-100 text-purple-600 rounded-lg">
                        <Wallet className="h-6 w-6" />
                    </div>
                    <div>
                        <h3 className="font-semibold text-main">Paper Portfolio Value</h3>
                        <div className="text-sm text-muted">Estimated Cost Basis</div>
                    </div>
                </div>
                <div className="text-right">
                    <div className="text-2xl font-bold text-main">${portfolioValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                    <button onClick={handleReset} className="text-xs text-red-500 hover:text-red-600 flex items-center mt-1">
                        <RotateCcw className="h-3 w-3 mr-1" /> Reset
                    </button>
                </div>
            </div>

            {/* Trade Form */}
            <div className="bg-card p-4 rounded-lg border border-border">
                <h4 className="text-sm font-semibold text-main mb-3 flex items-center">
                    <Calculator className="h-4 w-4 mr-2" /> Simulate Trade
                </h4>
                <form onSubmit={handleTrade} className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
                    <div>
                        <label className="text-xs text-muted block mb-1">Symbol</label>
                        <input
                            required
                            className="w-full bg-page border border-border rounded px-3 py-2 text-sm uppercase"
                            value={symbol}
                            onChange={e => setSymbol(e.target.value.toUpperCase())}
                            placeholder="AAPL"
                        />
                    </div>
                    <div>
                        <label className="text-xs text-muted block mb-1">Action</label>
                        <select
                            className="w-full bg-page border border-border rounded px-3 py-2 text-sm"
                            value={type}
                            onChange={e => setType(e.target.value as any)}
                        >
                            <option value="BUY">Buy</option>
                            <option value="SELL">Sell</option>
                        </select>
                    </div>
                    <div>
                        <label className="text-xs text-muted block mb-1">Quantity</label>
                        <input
                            required
                            type="number"
                            step="any"
                            className="w-full bg-page border border-border rounded px-3 py-2 text-sm"
                            value={quantity}
                            onChange={e => setQuantity(e.target.value)}
                            placeholder="0"
                        />
                    </div>
                    <div>
                        <label className="text-xs text-muted block mb-1">Price ($)</label>
                        <input
                            required
                            type="number"
                            step="any"
                            className="w-full bg-page border border-border rounded px-3 py-2 text-sm"
                            value={price}
                            onChange={e => setPrice(e.target.value)}
                            placeholder="0.00"
                        />
                    </div>
                    <button type="submit" className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded text-sm font-medium transition-colors">
                        Execute
                    </button>
                </form>
            </div>

            {/* History List */}
            <div className="bg-card rounded-lg shadow-sm border border-border overflow-hidden">
                <table className="w-full text-left">
                    <thead className="bg-muted/5">
                        <tr className="border-b border-border text-xs uppercase text-muted">
                            <th className="py-3 px-4">Date</th>
                            <th className="py-3 px-4">Action</th>
                            <th className="py-3 px-4">Symbol</th>
                            <th className="py-3 px-4 text-right">Details</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                        {transactions.map(t => (
                            <tr key={t.id} className="text-sm hover:bg-page/50">
                                <td className="py-3 px-4 text-muted">{new Date(t.transactionDate).toLocaleDateString()}</td>
                                <td className="py-3 px-4">
                                    <span className={`text-xs font-bold px-2 py-1 rounded ${t.type === 'BUY' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                                        }`}>
                                        {t.type}
                                    </span>
                                </td>
                                <td className="py-3 px-4 font-medium text-main">{t.symbol}</td>
                                <td className="py-3 px-4 text-right text-muted">
                                    {t.quantity} @ ${t.pricePerShare.toLocaleString()}
                                </td>
                            </tr>
                        ))}
                        {transactions.length === 0 && (
                            <tr>
                                <td colSpan={4} className="py-8 text-center text-muted">No simulated trades yet.</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default SandboxTab;
