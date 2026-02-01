import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import { usePortfolio } from '../../contexts/PortfolioContext';
import { TrendingUp, History, Edit2, Wallet, Plus } from 'lucide-react';

import LoadingSpinner from '../UI/LoadingSpinner';
import WatchlistWidget from './WatchlistWidget';
import TimeRangeSelector from './TimeRangeSelector';
import MarketIndices from './MarketIndices';
import StockPerformanceChart from './StockPerformanceChart';

import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, ReferenceLine } from 'recharts';

interface Account {
    id: number;
    name: string;
    type: string;
    balance: number;
    isManual: boolean;
    updatedAt: string;
}

interface Transaction {
    id: number;
    symbol: string;
    type: string;
    quantity: number;
    pricePerShare: number;
    totalCost: number;
    transactionDate: string;
}

interface Holding {
    symbol: string;
    quantity: number;
    currentPrice: number;
    currentValue: number;
    purchasePrice: number;
    totalGainLoss: number;
    totalGainLossPercentage: number;
}

interface AccountDetailsProps {
    account: Account;
    onUpdate: () => void;
}

const AccountDetails: React.FC<AccountDetailsProps> = ({ account, onUpdate }) => {
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [holdings, setHoldings] = useState<Holding[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [editBalance, setEditBalance] = useState(account.balance);
    const [activeTab, setActiveTab] = useState<'holdings' | 'transactions' | 'sandbox' | 'performance'>('holdings');
    const [isAddTxnOpen, setIsAddTxnOpen] = useState(false);

    // Use Portfolio Context for performance graph data
    const { historyData, isHistoryLoading, loadHistory } = usePortfolio();
    const [historyPeriod, setHistoryPeriod] = useState('1M');

    // Transaction Form State
    const [txnSymbol, setTxnSymbol] = useState('');
    const [portfolioSummary, setPortfolioSummary] = useState<any>(null);

    // Track active value for horizontal crosshair line
    const [activeValue, setActiveValue] = useState<number | null>(null);
    const [txnType, setTxnType] = useState('BUY');
    const [txnQty, setTxnQty] = useState('');
    const [txnPrice, setTxnPrice] = useState('');
    const [txnDate, setTxnDate] = useState(new Date().toISOString().split('T')[0]);

    // Selected symbol for chart (from watchlist clicks)
    const [selectedChartSymbol, setSelectedChartSymbol] = useState<string | undefined>(undefined);

    const isStockAccount = account.type === 'STOCK_PORTFOLIO';

    // Helper function to get last trading date formatted
    const getLastTradingDateFormatted = () => {
        const now = new Date();
        const day = now.getDay(); // 0 = Sunday, 6 = Saturday
        const hour = now.getHours();

        let lastTradingDate = new Date();

        if (day === 0) {
            // Sunday - last trading day was Friday
            lastTradingDate.setDate(now.getDate() - 2);
        } else if (day === 6) {
            // Saturday - last trading day was Friday
            lastTradingDate.setDate(now.getDate() - 1);
        } else if (day === 1 && hour < 9) {
            // Monday before market open - last trading day was Friday
            lastTradingDate.setDate(now.getDate() - 3);
        } else if (hour < 9) {
            // Weekday before market open - last trading day was yesterday
            lastTradingDate.setDate(now.getDate() - 1);
        } else if (hour >= 16) {
            // After market close - today is last trading day
            // lastTradingDate is already today
        } else {
            // During market hours - use yesterday as the last confirmed close
            lastTradingDate.setDate(now.getDate() - 1);
        }

        return lastTradingDate.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    };

    useEffect(() => {
        if (isStockAccount) {
            loadStockData();
        }
    }, [account.id]);

    useEffect(() => {
        if (isStockAccount && activeTab === 'performance') {
            loadHistory(historyPeriod);
        }
    }, [activeTab, historyPeriod, isStockAccount, loadHistory]);

    const loadStockData = async () => {
        setIsLoading(true);
        try {
            const [txnsRes, holdingsRes, portfolioRes] = await Promise.all([
                apiService.getTransactions(),
                apiService.getHoldings(),
                apiService.getPortfolio(false)
            ]);

            if (txnsRes && (txnsRes as any).success) setTransactions((txnsRes as any).data || []);
            if (holdingsRes && (holdingsRes as any).success) setHoldings((holdingsRes as any).data || []);
            if (portfolioRes && (portfolioRes as any).success) setPortfolioSummary((portfolioRes as any).data || {});

        } catch (e) {
            console.error("Failed to load stock data", e);
        } finally {
            setIsLoading(false);
        }
    };

    // ... (rest of methods)

    // Render part needs to use portfolioSummary
    // Finding the render part in file...
    // It's inside return (...)
    // I need to target the block with mismatched div

    // I will execute this replace in two chunks if needed, but defining useState and loadStockData is chunk 1.
    // Chunk 2 is the render block.


    const handleUpdateBalance = async () => {
        try {
            await apiService.updateAccountBalance(account.id, editBalance);
            setIsEditing(false);
            onUpdate();
        } catch (e) {
            console.error("Failed to update balance", e);
        }
    };

    const handleAddTransactionSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await apiService.addTransaction({
                symbol: txnSymbol.toUpperCase(),
                type: txnType,
                quantity: parseFloat(txnQty),
                pricePerShare: parseFloat(txnPrice),
                transactionDate: txnDate,
                totalCost: parseFloat(txnQty) * parseFloat(txnPrice)
            });
            setIsAddTxnOpen(false);
            // Reset form
            setTxnSymbol('');
            setTxnQty('');
            setTxnPrice('');
            // Reload data
            loadStockData();
            onUpdate(); // refresh balance
        } catch (e) {
            console.error("Failed to add transaction", e);
            alert("Failed to add transaction. Check console.");
        }
    };

    // Calculate stats
    const totalGainLoss = isStockAccount ? holdings.reduce((sum, h) => sum + (h.totalGainLoss || 0), 0) : 0;

    return (
        <div className="p-6 space-y-6 animate-fadeIn">
            {/* Account Header */}
            <div className="bg-card rounded-lg p-6 shadow-sm border border-border flex justify-between items-center">
                <div>
                    <div className="flex items-center space-x-2 text-muted text-sm mb-1">
                        <span>{account.type.replace('_', ' ')}</span>
                        {account.isManual && (
                            <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">Manual</span>
                        )}
                    </div>
                    <h1 className="text-3xl font-bold text-main">{account.name}</h1>
                    <div className="text-sm text-muted mt-1">
                        {isStockAccount ? (
                            `Updated as of ${getLastTradingDateFormatted()} (Market Close)`
                        ) : (
                            `Last updated: ${account.updatedAt ? new Date(account.updatedAt).toLocaleDateString() : 'Never'}`
                        )}
                    </div>
                </div>

                <div className="text-right">
                    <div className="text-sm text-muted mb-1">Current Balance</div>
                    {isEditing ? (
                        <div className="flex items-center space-x-2 justify-end">
                            <span className="text-2xl font-bold text-main">$</span>
                            <input
                                type="number"
                                value={editBalance}
                                onChange={(e) => setEditBalance(parseFloat(e.target.value))}
                                className="text-2xl font-bold bg-page border border-border rounded px-2 w-48 text-main text-right"
                            />
                            <div className="flex flex-col space-y-1">
                                <button onClick={handleUpdateBalance} className="bg-primary text-white px-2 py-1 rounded text-xs">Save</button>
                                <button onClick={() => setIsEditing(false)} className="text-muted text-xs hover:text-main">Cancel</button>
                            </div>
                        </div>
                    ) : (
                        <div className="group">
                            <div className="flex items-center justify-end">
                                <div className="text-4xl font-bold text-main">
                                    ${(isStockAccount ? holdings.reduce((sum, h) => sum + (h.currentValue || 0), 0) : account.balance)
                                        .toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </div>
                                {account.isManual && (
                                    <button onClick={() => { setEditBalance(account.balance); setIsEditing(true); }} className="ml-3 p-2 hover:bg-page rounded-full text-muted hover:text-primary transition-colors">
                                        <Edit2 className="h-4 w-4" />
                                    </button>
                                )}
                            </div>
                            {isStockAccount && portfolioSummary?.totalGainLossYTD !== undefined && (
                                <div className={`text-sm mt-1 float-right ${portfolioSummary.totalGainLossYTD >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                    {portfolioSummary.totalGainLossYTD >= 0 ? '+' : ''}${Math.abs(portfolioSummary.totalGainLossYTD).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })} YTD
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {
                isStockAccount ? (
                    <>
                        {/* Main Layout Grid - center content takes more space */}
                        <div className="grid grid-cols-1 lg:grid-cols-[65fr_35fr] gap-6">

                            {/* Left Column: Holdings & Transactions */}
                            <div className="space-y-6">

                                {/* Tabs */}
                                <div className="flex space-x-1 bg-card border border-border rounded-lg p-1 w-fit">
                                    <button
                                        onClick={() => setActiveTab('holdings')}
                                        className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${activeTab === 'holdings' ? 'bg-primary text-white' : 'text-muted hover:text-main'}`}
                                    >
                                        Holdings
                                    </button>
                                    <button
                                        onClick={() => setActiveTab('transactions')}
                                        className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${activeTab === 'transactions' ? 'bg-primary text-white' : 'text-muted hover:text-main'}`}
                                    >
                                        Transactions
                                    </button>
                                    <button
                                        onClick={() => setActiveTab('performance')}
                                        className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${activeTab === 'performance' ? 'bg-primary text-white' : 'text-muted hover:text-main'}`}
                                    >
                                        Performance
                                    </button>
                                    <button
                                        onClick={() => setActiveTab('sandbox')}
                                        className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${activeTab === 'sandbox' ? 'bg-primary text-white' : 'text-muted hover:text-main'}`}
                                    >
                                        Sandbox
                                    </button>
                                </div>

                                {isLoading ? (
                                    <LoadingSpinner />
                                ) : (
                                    <>
                                        {activeTab === 'holdings' && (
                                            <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                                                {/* Holdings Table */}
                                                <div className="bg-card rounded-lg shadow-sm border border-border overflow-hidden">
                                                    <table className="w-full text-left">
                                                        <thead className="bg-muted/5">
                                                            <tr className="border-b border-border text-xs uppercase text-muted">
                                                                <th className="py-3 px-4">Symbol</th>
                                                                <th className="py-3 px-4 text-right">Qty</th>
                                                                <th className="py-3 px-4 text-right">Price</th>
                                                                <th className="py-3 px-4 text-right">Value</th>
                                                                <th className="py-3 px-4 text-right">Gain/Loss</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody className="divide-y divide-border">
                                                            {holdings.map((h) => (
                                                                <tr
                                                                    key={h.symbol}
                                                                    onClick={() => setSelectedChartSymbol(h.symbol)}
                                                                    className={`hover:bg-page/50 text-sm cursor-pointer transition-colors ${selectedChartSymbol === h.symbol ? 'bg-primary/10' : ''
                                                                        }`}
                                                                >
                                                                    <td className="py-3 px-4 font-medium text-main">{h.symbol}</td>
                                                                    <td className="py-3 px-4 text-right text-muted">{h.quantity.toLocaleString()}</td>
                                                                    <td className="py-3 px-4 text-right text-main">${(h.currentPrice || 0).toFixed(2)}</td>
                                                                    <td className="py-3 px-4 text-right font-medium text-main">${(h.currentValue || 0).toLocaleString()}</td>
                                                                    <td className={`py-3 px-4 text-right ${(h.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                                        {(h.totalGainLoss || 0) >= 0 ? '+' : ''}{(h.totalGainLossPercentage || 0).toFixed(2)}%
                                                                    </td>
                                                                </tr>
                                                            ))}
                                                        </tbody>
                                                    </table>
                                                </div>

                                                {/* Stock Performance Chart - next to holdings */}
                                                {holdings.length > 0 && (
                                                    <StockPerformanceChart holdings={holdings} externalSymbol={selectedChartSymbol} />
                                                )}
                                            </div>
                                        )}

                                        {activeTab === 'transactions' && (
                                            <div className="bg-card rounded-lg shadow-sm border border-border overflow-hidden">
                                                <div className="p-4 border-b border-border flex justify-between items-center bg-muted/5">
                                                    <h3 className="text-sm font-semibold text-main">Transaction History</h3>
                                                    <button
                                                        onClick={() => setIsAddTxnOpen(true)}
                                                        className="flex items-center space-x-1 text-xs bg-primary text-white px-3 py-1.5 rounded hover:bg-primary/90 transition-colors"
                                                    >
                                                        <Plus className="h-3 w-3" />
                                                        <span>Add Transaction</span>
                                                    </button>
                                                </div>
                                                <table className="w-full text-left">
                                                    <thead className="bg-muted/5">
                                                        <tr className="border-b border-border text-xs uppercase text-muted">
                                                            <th className="py-3 px-4">Date</th>
                                                            <th className="py-3 px-4">Symbol</th>
                                                            <th className="py-3 px-4">Type</th>
                                                            <th className="py-3 px-4 text-right">Summary</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody className="divide-y divide-border">
                                                        {transactions.map((txn) => (
                                                            <tr key={txn.id} className="hover:bg-page/50 text-sm">
                                                                <td className="py-3 px-4 text-muted">{new Date(txn.transactionDate).toLocaleDateString()}</td>
                                                                <td className="py-3 px-4 font-medium text-main">{txn.symbol}</td>
                                                                <td className="py-3 px-4">
                                                                    <span className={`text-xs font-bold px-2 py-1 rounded ${txn.type === 'BUY' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                                                                        }`}>
                                                                        {txn.type}
                                                                    </span>
                                                                </td>
                                                                <td className="py-3 px-4 text-right text-main">
                                                                    {txn.type === 'BUY' ? 'Bought' : 'Sold'} {txn.quantity.toLocaleString()} @ ${txn.pricePerShare.toLocaleString()}
                                                                </td>
                                                            </tr>
                                                        ))}
                                                    </tbody>
                                                </table>
                                            </div>
                                        )}

                                        {activeTab === 'performance' && (
                                            <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                                                <div className="flex items-center justify-between mb-6">
                                                    <h3 className="text-lg font-semibold text-main">Portfolio Value Over Time</h3>
                                                    <TimeRangeSelector
                                                        selectedPeriod={historyPeriod}
                                                        onSelectPeriod={setHistoryPeriod}
                                                    />
                                                </div>

                                                <div className="h-[400px] w-full">
                                                    <ResponsiveContainer width="100%" height="100%">
                                                        <AreaChart
                                                            data={historyData.filter(d => d.value > 0)}
                                                            margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                                                            onMouseMove={(e: any) => {
                                                                if (e.activePayload && e.activePayload[0]) {
                                                                    setActiveValue(e.activePayload[0].payload.value);
                                                                }
                                                            }}
                                                            onMouseLeave={() => setActiveValue(null)}
                                                        >
                                                            <defs>
                                                                <linearGradient id="colorVal" x1="0" y1="0" x2="0" y2="1">
                                                                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                                                                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                                                                </linearGradient>
                                                            </defs>
                                                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#374151" opacity={0.1} />
                                                            <XAxis
                                                                dataKey="date"
                                                                tickFormatter={(str) => {
                                                                    const [year, month, day] = str.split('-').map(Number);
                                                                    const date = new Date(year, month - 1, day);
                                                                    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: historyPeriod === 'ALL' || historyPeriod === '5Y' ? '2-digit' : undefined });
                                                                }}
                                                                stroke="#9ca3af"
                                                                fontSize={12}
                                                                tickMargin={10}
                                                                minTickGap={30}
                                                            />
                                                            <YAxis
                                                                tickFormatter={(value) => `$${value.toLocaleString('en-US', { notation: 'compact', maximumFractionDigits: 2 })}`}
                                                                stroke="#9ca3af"
                                                                fontSize={12}
                                                                domain={['dataMin', 'dataMax']}
                                                            />
                                                            <Tooltip
                                                                contentStyle={{ backgroundColor: '#1f2937', borderColor: '#374151', borderRadius: '0.5rem', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
                                                                itemStyle={{ color: '#e5e7eb' }}
                                                                labelStyle={{ color: '#9ca3af', marginBottom: '0.25rem' }}
                                                                formatter={(value: number) => [`$${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, 'Portfolio Value']}
                                                                labelFormatter={(label) => {
                                                                    const [year, month, day] = label.split('-').map(Number);
                                                                    return new Date(year, month - 1, day).toLocaleDateString('en-US', { dateStyle: 'full' });
                                                                }}
                                                            />
                                                            <Area
                                                                type="monotone"
                                                                dataKey="value"
                                                                stroke="#3b82f6"
                                                                strokeWidth={2}
                                                                fillOpacity={1}
                                                                fill="url(#colorVal)"
                                                                activeDot={{ r: 6, strokeWidth: 0 }}
                                                            />
                                                            {activeValue !== null && (
                                                                <ReferenceLine
                                                                    y={activeValue}
                                                                    stroke="#9ca3af"
                                                                    strokeDasharray="3 3"
                                                                    opacity={0.5}
                                                                />
                                                            )}
                                                        </AreaChart>
                                                    </ResponsiveContainer>
                                                </div>
                                            </div>
                                        )}

                                        {activeTab === 'sandbox' && (
                                            <div className="bg-card rounded-lg p-8 text-center border border-border border-dashed">
                                                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-blue-100 text-blue-600 mb-4">
                                                    <Wallet className="w-6 h-6" />
                                                </div>
                                                <h3 className="text-lg font-medium text-main">Paper Trading Sandbox</h3>
                                                <p className="text-muted mt-2 max-w-sm mx-auto">
                                                    Simulate trades and test "what-if" scenarios without affecting your actual portfolio.
                                                    <br /><span className="text-xs uppercase font-bold text-blue-500 mt-2 block">Coming Soon</span>
                                                </p>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>

                            {/* Right Column: Market Indices, Watchlist & Quick Stats */}
                            <div className="space-y-6">
                                <MarketIndices />
                                <WatchlistWidget />

                                {/* Simplified Performance Card */}
                                <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                                    <h3 className="font-semibold text-main mb-4 flex items-center">
                                        <TrendingUp className="h-4 w-4 mr-2" />
                                        Performance
                                    </h3>
                                    <div className="space-y-4">
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="text-muted">Day Change</span>
                                            <span className="text-green-600 font-medium">+$0.00 (0.00%)</span>
                                        </div>
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="text-muted">Total Return</span>
                                            <span className={`font-medium ${totalGainLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                {totalGainLoss >= 0 ? '+' : ''}${totalGainLoss.toLocaleString()}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </div>
                    </>
                ) : (
                    /* Default/Manual Account View */
                    <div className="bg-card rounded-lg p-6 shadow-sm border border-border text-center py-12">
                        <History className="h-12 w-12 text-muted mx-auto mb-4" />
                        <h3 className="text-lg font-medium text-main">History Tracking</h3>
                        <p className="text-muted mt-2">
                            Historical balance tracking for {account.name} will appear here.
                        </p>
                    </div>
                )
            }
            {/* Add Transaction Modal */}
            {
                isAddTxnOpen && (
                    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                        <div className="bg-card rounded-lg border border-border shadow-lg p-6 w-full max-w-md animate-in fade-in zoom-in duration-200">
                            <h3 className="text-lg font-bold text-main mb-4">Add Real Transaction</h3>
                            <form onSubmit={handleAddTransactionSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Date</label>
                                    <input
                                        type="date"
                                        required
                                        className="w-full bg-page border border-border rounded px-3 py-2 text-main"
                                        value={txnDate}
                                        onChange={e => setTxnDate(e.target.value)}
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-muted mb-1">Symbol</label>
                                        <input
                                            type="text"
                                            required
                                            className="w-full bg-page border border-border rounded px-3 py-2 text-main uppercase"
                                            placeholder="AAPL"
                                            value={txnSymbol}
                                            onChange={e => setTxnSymbol(e.target.value)}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-muted mb-1">Type</label>
                                        <select
                                            className="w-full bg-page border border-border rounded px-3 py-2 text-main"
                                            value={txnType}
                                            onChange={e => setTxnType(e.target.value)}
                                        >
                                            <option value="BUY">Buy</option>
                                            <option value="SELL">Sell</option>
                                        </select>
                                    </div>
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-muted mb-1">Quantity</label>
                                        <input
                                            type="number"
                                            step="any"
                                            required
                                            className="w-full bg-page border border-border rounded px-3 py-2 text-main"
                                            placeholder="0"
                                            value={txnQty}
                                            onChange={e => setTxnQty(e.target.value)}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-muted mb-1">Price ($)</label>
                                        <input
                                            type="number"
                                            step="any"
                                            required
                                            className="w-full bg-page border border-border rounded px-3 py-2 text-main"
                                            placeholder="0.00"
                                            value={txnPrice}
                                            onChange={e => setTxnPrice(e.target.value)}
                                        />
                                    </div>
                                </div>

                                <div className="bg-muted/10 p-3 rounded text-sm text-center text-muted">
                                    Total {txnType === 'BUY' ? 'Cost' : 'Proceeds'}: <strong>${((parseFloat(txnQty) || 0) * (parseFloat(txnPrice) || 0)).toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
                                </div>

                                <div className="flex justify-end space-x-3 pt-2">
                                    <button
                                        type="button"
                                        onClick={() => setIsAddTxnOpen(false)}
                                        className="px-4 py-2 text-sm text-muted hover:text-main"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        className="px-4 py-2 bg-primary text-white text-sm font-medium rounded hover:bg-primary/90"
                                    >
                                        Confirm Transaction
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )
            }
        </div >
    );
};

export default AccountDetails;
