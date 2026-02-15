import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, DollarSign, TrendingUp, Clock, Pencil, Trash2 } from 'lucide-react';
import { apiService } from '../../services/apiService';
import { SandboxPortfolioDetail } from './SandboxTypes';
import LoadingSpinner from '../UI/LoadingSpinner';

interface SandboxPortfolioDetailProps {
    portfolioId?: number;
    onBack?: () => void;
}

const SandboxPortfolioDetailComponent: React.FC<SandboxPortfolioDetailProps> = ({ portfolioId, onBack }) => {
    // If portfolioId is passed via props (embedded), use it. Otherwise use params (standalone).
    const { id } = useParams<{ id: string }>();
    const effectiveId = portfolioId || Number(id);

    const navigate = useNavigate();
    const [portfolio, setPortfolio] = useState<SandboxPortfolioDetail | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isTradeModalOpen, setIsTradeModalOpen] = useState(false);
    const [tradeType, setTradeType] = useState<'BUY' | 'SELL'>('BUY');
    const [isFetchingPrice, setIsFetchingPrice] = useState(false);
    const [inputMode, setInputMode] = useState<'shares' | 'dollars'>('shares');
    const [editingTransactionId, setEditingTransactionId] = useState<number | null>(null);
    const [deleteConfirm, setDeleteConfirm] = useState<{ show: boolean; txnId: number | null }>({ show: false, txnId: null });
    const [tradeForm, setTradeForm] = useState({
        symbol: '',
        quantity: 1,
        dollarAmount: 100,
        price: 0,
        date: new Date().toISOString().split('T')[0]
    });

    useEffect(() => {
        if (effectiveId) fetchPortfolioDetails();
    }, [effectiveId]);

    const fetchPortfolioDetails = async () => {
        try {
            const data = await apiService.getSandboxPortfolioDetails(effectiveId);
            setPortfolio(data);
        } catch (error) {
            console.error('Failed to fetch portfolio details:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleBack = () => {
        if (onBack) {
            onBack();
        } else {
            navigate('/sandbox');
        }
    };

    const handleFetchPrice = async () => {
        if (!tradeForm.symbol || !tradeForm.date) return;

        setIsFetchingPrice(true);
        try {
            const price = await apiService.getHistoricalPrice(tradeForm.symbol, tradeForm.date);
            setTradeForm(prev => ({ ...prev, price }));
        } catch (error) {
            console.error('Failed to fetch price:', error);
            alert('Could not fetch price for this date. Market might be closed or symbol invalid.');
        } finally {
            setIsFetchingPrice(false);
        }
    };

    const resetTradeForm = () => {
        setTradeForm({ symbol: '', quantity: 1, dollarAmount: 100, price: 0, date: new Date().toISOString().split('T')[0] });
        setInputMode('shares');
        setEditingTransactionId(null);
    };

    const handleTradeSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const tradeData: any = {
                symbol: tradeForm.symbol.toUpperCase(),
                type: tradeType,
                price: tradeForm.price > 0 ? tradeForm.price : undefined,
                date: tradeForm.date
            };

            if (inputMode === 'dollars') {
                tradeData.dollarAmount = Number(tradeForm.dollarAmount);
            } else {
                tradeData.quantity = Number(tradeForm.quantity);
            }

            if (editingTransactionId) {
                await apiService.editSandboxTransaction(effectiveId, editingTransactionId, tradeData);
            } else {
                await apiService.executeSandboxTrade(effectiveId, tradeData);
            }

            setIsTradeModalOpen(false);
            resetTradeForm();
            await fetchPortfolioDetails();
        } catch (error) {
            console.error('Trade failed:', error);
            alert('Trade failed: ' + ((error as any).response?.data?.message || (error as any).message));
        }
    };

    const handleDeleteTransaction = async (txnId: number) => {
        try {
            await apiService.deleteSandboxTransaction(effectiveId, txnId);
            setDeleteConfirm({ show: false, txnId: null });
            await fetchPortfolioDetails();
        } catch (error) {
            console.error('Delete failed:', error);
            alert('Delete failed: ' + ((error as any).response?.data?.message || (error as any).message));
        }
    };

    const handleEditTransaction = (txn: any) => {
        setTradeForm({
            symbol: txn.symbol,
            quantity: txn.quantity,
            dollarAmount: txn.totalCost,
            price: txn.price,
            date: txn.transactionDate
        });
        setTradeType(txn.type);
        setInputMode('shares');
        setEditingTransactionId(txn.id);
        setIsTradeModalOpen(true);
    };

    if (isLoading) return <div className="flex justify-center p-8"><LoadingSpinner /></div>;
    if (!portfolio) return <div className="p-8 text-center">Portfolio not found</div>;

    const formatMoney = (val: number) => `$${val.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    const formatPercent = (val: number) => `${val > 0 ? '+' : ''}${val.toFixed(2)}%`;

    return (
        <div className="container mx-auto px-4 py-8 max-w-7xl animate-fadeIn">
            <button
                onClick={handleBack}
                className="flex items-center gap-2 text-muted hover:text-main mb-6 transition"
            >
                <ArrowLeft size={20} />
                Back to Dashboard
            </button>

            {/* Header / Overview */}
            <div className="bg-card rounded-xl shadow-sm border border-border p-6 mb-6">
                <div className="flex flex-col md:flex-row justify-between md:items-start gap-4 mb-6">
                    <div>
                        <h1 className="text-3xl font-bold text-main">{portfolio.name}</h1>
                        <p className="text-muted mt-1">{portfolio.description}</p>
                    </div>
                    <button
                        onClick={() => {
                            setTradeType('BUY');
                            setIsTradeModalOpen(true);
                        }}
                        className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                    >
                        <Plus size={20} />
                        Place Trade
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                    <div className="p-4 bg-page rounded-lg">
                        <p className="text-sm text-muted uppercase font-medium">Net Worth</p>
                        <p className="text-2xl font-bold text-main">{formatMoney(portfolio.totalValue || 0)}</p>
                    </div>
                    <div className="p-4 bg-page rounded-lg">
                        <p className="text-sm text-muted uppercase font-medium">Cash Available</p>
                        <p className="text-2xl font-bold text-main">{formatMoney(portfolio.currentBalance)}</p>
                    </div>
                    <div className="p-4 bg-page rounded-lg">
                        <p className="text-sm text-muted uppercase font-medium">Holdings Value</p>
                        <p className="text-2xl font-bold text-main">{formatMoney(portfolio.holdingsValue || 0)}</p>
                    </div>
                    <div className="p-4 bg-page rounded-lg">
                        <p className="text-sm text-muted uppercase font-medium">Total Return</p>
                        <div className={`flex items-baseline gap-2 ${(portfolio.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                            <p className="text-2xl font-bold">
                                {formatMoney(portfolio.totalGainLoss || 0)}
                            </p>
                            <span className="text-sm font-medium">
                                ({formatPercent(portfolio.totalGainLossPercentage || 0)})
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content Layout */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Left Column: Holdings & Transactions */}
                <div className="lg:col-span-2 space-y-6">

                    {/* Holdings Table */}
                    <div className="bg-card rounded-xl shadow-sm border border-border overflow-hidden">
                        <div className="p-6 border-b border-border flex justify-between items-center">
                            <h2 className="text-lg font-bold text-main">Holdings</h2>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-page text-xs text-muted uppercase">
                                    <tr>
                                        <th className="px-6 py-3 text-left font-medium">Symbol</th>
                                        <th className="px-6 py-3 text-right font-medium">Qty</th>
                                        <th className="px-6 py-3 text-right font-medium">Avg Cost</th>
                                        <th className="px-6 py-3 text-right font-medium">Price</th>
                                        <th className="px-6 py-3 text-right font-medium">Value</th>
                                        <th className="px-6 py-3 text-right font-medium">Gain/Loss</th>
                                        <th className="px-6 py-3 text-right font-medium">Actions</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-border">
                                    {portfolio.holdings.length === 0 ? (
                                        <tr>
                                            <td colSpan={7} className="px-6 py-8 text-center text-muted">
                                                No holdings yet. Place a trade to get started.
                                            </td>
                                        </tr>
                                    ) : (
                                        portfolio.holdings.map((holding) => (
                                            <tr key={holding.id} className="hover:bg-page transition">
                                                <td className="px-6 py-4 font-semibold text-main">{holding.symbol}</td>
                                                <td className="px-6 py-4 text-right text-main">{holding.quantity}</td>
                                                <td className="px-6 py-4 text-right text-muted">{formatMoney(holding.averageCost)}</td>
                                                <td className="px-6 py-4 text-right font-medium text-main">{formatMoney(holding.currentPrice)}</td>
                                                <td className="px-6 py-4 text-right font-bold text-main">{formatMoney(holding.marketValue)}</td>
                                                <td className={`px-6 py-4 text-right font-medium ${holding.totalGainLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                    {formatMoney(holding.totalGainLoss)}
                                                    <span className="text-xs ml-1 block">
                                                        ({formatPercent(holding.totalGainLossPercentage)})
                                                    </span>
                                                </td>
                                                <td className="px-6 py-4 text-right">
                                                    <button
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            setTradeForm(prev => ({ ...prev, symbol: holding.symbol }));
                                                            setTradeType('SELL');
                                                            setIsTradeModalOpen(true);
                                                        }}
                                                        className="text-xs text-blue-600 hover:text-blue-800 font-medium"
                                                    >
                                                        Sell
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Recent Transactions */}
                    <div className="bg-card rounded-xl shadow-sm border border-border overflow-hidden">
                        <div className="p-6 border-b border-border">
                            <h2 className="text-lg font-bold text-main">Recent Activity</h2>
                        </div>
                        <div className="divide-y divide-border">
                            {portfolio.recentTransactions.length === 0 ? (
                                <div className="p-6 text-center text-muted">No transactions found.</div>
                            ) : (
                                portfolio.recentTransactions.map((txn) => (
                                    <div key={txn.id} className="p-4 flex justify-between items-center hover:bg-page group">
                                        <div className="flex items-center gap-4">
                                            <div className={`p-2 rounded-full ${txn.type === 'BUY' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600'}`}>
                                                {txn.type === 'BUY' ? <TrendingUp size={16} /> : <DollarSign size={16} />}
                                            </div>
                                            <div>
                                                <p className="font-semibold text-main">
                                                    {txn.type === 'BUY' ? 'Bought' : 'Sold'} {Number(txn.quantity).toFixed(4).replace(/\.?0+$/, '')} {txn.symbol}
                                                </p>
                                                <div className="flex items-center gap-2 text-xs text-muted">
                                                    <Clock size={12} />
                                                    {txn.transactionDate}
                                                </div>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <div className="text-right">
                                                <p className="font-bold text-main">{formatMoney(txn.totalCost)}</p>
                                                <p className="text-xs text-muted">@ {formatMoney(txn.price)}</p>
                                            </div>
                                            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                                <button
                                                    onClick={() => handleEditTransaction(txn)}
                                                    className="p-1.5 text-muted hover:text-blue-600 hover:bg-blue-50 rounded transition"
                                                    title="Edit transaction"
                                                >
                                                    <Pencil size={14} />
                                                </button>
                                                <button
                                                    onClick={() => setDeleteConfirm({ show: true, txnId: txn.id })}
                                                    className="p-1.5 text-muted hover:text-red-600 hover:bg-red-50 rounded transition"
                                                    title="Delete transaction"
                                                >
                                                    <Trash2 size={14} />
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>

                {/* Right Column: Performance Graph (Placeholder for now) */}
                <div className="lg:col-span-1 space-y-6">
                    <div className="bg-card rounded-xl shadow-sm border border-border p-6">
                        <h2 className="text-lg font-bold text-main mb-4">Performance</h2>
                        <div className="h-64 flex items-center justify-center bg-page rounded-lg border border-dashed border-border text-muted">
                            <div className="text-center">
                                <TrendingUp className="mx-auto mb-2 opacity-50" size={32} />
                                <p className="text-sm">Not enough data for chart yet.</p>
                                <p className="text-xs mt-1">Check back tomorrow!</p>
                            </div>
                        </div>
                        {/* 
                            TODO: Implement real chart using historical snapshot data.
                            We need to create a daily snapshot job for sandbox data first.
                        */}
                    </div>

                    {/* Quick Stats or Tips */}
                    <div className="bg-blue-50 rounded-xl border border-blue-100 p-6">
                        <h3 className="text-blue-900 font-semibold mb-2">Sandbox Tips</h3>
                        <ul className="text-sm text-blue-800 space-y-2 list-disc list-inside">
                            <li>All trades are simulations.</li>
                            <li>Real-time market data is used.</li>
                            <li>Test risky strategies here!</li>
                        </ul>
                    </div>
                </div>
            </div>

            {/* Trade Modal */}
            {isTradeModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-card rounded-xl max-w-sm w-full p-6">
                        <h2 className="text-xl font-bold mb-4 text-main">
                            {tradeType === 'BUY' ? 'Buy Stock' : 'Sell Stock'}
                        </h2>

                        {/* Type Toggle */}
                        <div className="flex bg-page rounded-lg p-1 mb-4">
                            <button
                                className={`flex-1 py-1.5 text-sm font-medium rounded-md transition ${tradeType === 'BUY' ? 'bg-card shadow-sm text-blue-600' : 'text-muted'}`}
                                onClick={() => setTradeType('BUY')}
                            >
                                Buy
                            </button>
                            <button
                                className={`flex-1 py-1.5 text-sm font-medium rounded-md transition ${tradeType === 'SELL' ? 'bg-card shadow-sm text-green-600' : 'text-muted'}`}
                                onClick={() => setTradeType('SELL')}
                            >
                                Sell
                            </button>
                        </div>

                        <form onSubmit={handleTradeSubmit}>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Symbol</label>
                                    <input
                                        type="text"
                                        required
                                        className="w-full p-2 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none uppercase bg-page text-main"
                                        value={tradeForm.symbol}
                                        onChange={(e) => setTradeForm({ ...tradeForm, symbol: e.target.value.toUpperCase() })}
                                        placeholder="AAPL"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Date</label>
                                    <input
                                        type="date"
                                        required
                                        max={new Date().toISOString().split('T')[0]}
                                        className="w-full p-2 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                        value={tradeForm.date}
                                        onChange={(e) => setTradeForm({ ...tradeForm, date: e.target.value })}
                                    />
                                </div>

                                {/* Input Mode Toggle: Shares vs Dollar Amount */}
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Invest By</label>
                                    <div className="flex bg-page rounded-lg p-1 mb-2">
                                        <button
                                            type="button"
                                            className={`flex-1 py-1 text-xs font-medium rounded-md transition ${inputMode === 'shares' ? 'bg-card shadow-sm text-main' : 'text-muted'}`}
                                            onClick={() => setInputMode('shares')}
                                        >
                                            # Shares
                                        </button>
                                        <button
                                            type="button"
                                            className={`flex-1 py-1 text-xs font-medium rounded-md transition ${inputMode === 'dollars' ? 'bg-card shadow-sm text-main' : 'text-muted'}`}
                                            onClick={() => setInputMode('dollars')}
                                        >
                                            $ Amount
                                        </button>
                                    </div>

                                    {inputMode === 'shares' ? (
                                        <div>
                                            <input
                                                type="number"
                                                required
                                                min="0.00000001"
                                                step="any"
                                                className="w-full p-2 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                                value={tradeForm.quantity}
                                                onChange={(e) => setTradeForm({ ...tradeForm, quantity: parseFloat(e.target.value) })}
                                                placeholder="Number of shares"
                                            />
                                        </div>
                                    ) : (
                                        <div className="relative">
                                            <span className="absolute left-3 top-2 text-muted">$</span>
                                            <input
                                                type="number"
                                                required
                                                min="0.01"
                                                step="0.01"
                                                className="w-full p-2 pl-7 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                                value={tradeForm.dollarAmount}
                                                onChange={(e) => setTradeForm({ ...tradeForm, dollarAmount: parseFloat(e.target.value) })}
                                                placeholder="Dollar amount to invest"
                                            />
                                            <p className="text-xs text-muted mt-1">Fractional shares will be calculated automatically.</p>
                                        </div>
                                    )}
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Price per Share</label>
                                    <div className="flex gap-2">
                                        <div className="relative flex-1">
                                            <span className="absolute left-3 top-2 text-muted">$</span>
                                            <input
                                                type="number"
                                                step="0.01"
                                                className="w-full p-2 pl-7 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                                value={tradeForm.price || ''}
                                                onChange={(e) => setTradeForm({ ...tradeForm, price: parseFloat(e.target.value) })}
                                                placeholder="Market Price"
                                            />
                                        </div>
                                        <button
                                            type="button"
                                            onClick={handleFetchPrice}
                                            disabled={isFetchingPrice || !tradeForm.symbol}
                                            className="px-3 py-2 bg-page text-muted rounded-lg hover:bg-muted/10 transition text-sm font-medium disabled:opacity-50 border border-border"
                                        >
                                            {isFetchingPrice ? <LoadingSpinner size="small" /> : 'Get Price'}
                                        </button>
                                    </div>
                                    <p className="text-xs text-muted mt-1">
                                        Leave empty to use current market price, or click "Get Price" to fetch historical close.
                                    </p>
                                </div>
                            </div>

                            <div className="flex justify-end gap-3 mt-6">
                                <button
                                    type="button"
                                    onClick={() => { setIsTradeModalOpen(false); resetTradeForm(); }}
                                    className="px-4 py-2 text-muted hover:bg-page rounded-lg transition"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className={`px-4 py-2 text-white rounded-lg transition ${tradeType === 'BUY' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-green-600 hover:bg-green-700'}`}
                                >
                                    {editingTransactionId
                                        ? 'Save Changes'
                                        : tradeType === 'BUY' ? 'Place Buy Order' : 'Place Sell Order'
                                    }
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Dialog */}
            {deleteConfirm.show && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-card rounded-xl max-w-sm w-full p-6">
                        <h2 className="text-lg font-bold text-main mb-2">Delete Transaction</h2>
                        <p className="text-muted text-sm mb-4">
                            Are you sure? This will reverse the transaction's effect on your portfolio cash balance and holdings.
                        </p>
                        <div className="flex justify-end gap-3">
                            <button
                                onClick={() => setDeleteConfirm({ show: false, txnId: null })}
                                className="px-4 py-2 text-muted hover:bg-page rounded-lg transition"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={() => deleteConfirm.txnId && handleDeleteTransaction(deleteConfirm.txnId)}
                                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default SandboxPortfolioDetailComponent;


