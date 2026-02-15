import React, { useState, useEffect } from 'react';

import { apiService } from '../../services/apiService';
import { usePortfolio } from '../../contexts/PortfolioContext';
import { TrendingUp, History, Edit2, Plus } from 'lucide-react';


// Import Sandbox Components
import SandboxDashboard from '../Sandbox/SandboxDashboard';
import SandboxPortfolioDetailComponent from '../Sandbox/SandboxPortfolioDetail';

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
    dailyChange?: number;
    dailyChangePercentage?: number;
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

    // Sandbox Internal Navigation State
    const [activeSandboxPortfolioId, setActiveSandboxPortfolioId] = useState<number | null>(null);

    // Use Portfolio Context for performance graph data
    const { historyData, loadHistory } = usePortfolio();
    const [historyPeriod, setHistoryPeriod] = useState('1M');
    // ... rest of the file ... (lines 64-543 unchanged)
    // ...


    // Transaction Form State
    const [txnSymbol, setTxnSymbol] = useState('');
    const [portfolioSummary, setPortfolioSummary] = useState<any>(null);

    // Track active value for horizontal crosshair line
    const [activeValue, setActiveValue] = useState<number | null>(null);
    const [txnType, setTxnType] = useState('BUY');
    const [txnQty, setTxnQty] = useState('');
    const [txnPrice, setTxnPrice] = useState('');
    const [txnDate, setTxnDate] = useState(new Date().toISOString().split('T')[0]);

    // Account selection for transactions
    const [allAccounts, setAllAccounts] = useState<any[]>([]);
    const [txnAccountId, setTxnAccountId] = useState<string>('');

    // Account transaction history (cash movements)
    const [accountTransactions, setAccountTransactions] = useState<any[]>([]);

    // Selected symbol for chart (from watchlist clicks)
    const [selectedChartSymbol, setSelectedChartSymbol] = useState<string | undefined>(undefined);

    const isStockAccount = account.type === 'STOCK_PORTFOLIO';

    // Helper to determine accurate "As of" message
    const getUpdateStatusMessage = () => {
        const now = new Date();


        // Check if weekend (Sat/Sun)
        const day = now.toLocaleString('en-US', { timeZone: 'America/New_York', weekday: 'short' });
        const timeParts = now.toLocaleString('en-US', { timeZone: 'America/New_York', hour: 'numeric', minute: 'numeric', hour12: false }).split(':');
        const hour = parseInt(timeParts[0]);
        const minute = parseInt(timeParts[1]);
        const timeVal = hour + (minute / 60);

        const isWeekend = day === 'Sat' || day === 'Sun';
        // Market Hours: 9:30 - 16:00 ET Mon-Fri
        const isMarketHours = !isWeekend && timeVal >= 9.5 && timeVal < 16.0;

        if (isMarketHours) {
            return `Real-time Market Data (As of ${now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})`;
        } else {
            // If closed, explain why
            let reason = "Market Closed";
            if (isWeekend) reason = "Market Closed (Weekend)";
            else if (timeVal >= 16.0) reason = "Market Closed (After Hours)";
            else if (timeVal < 9.5) reason = "Pre-Market";

            // Calculate last close date roughly for display
            const closeDate = new Date(now);
            if (isWeekend) {
                const dayNum = now.getDay(); // 0Sun, 6Sat
                closeDate.setDate(now.getDate() - (dayNum === 0 ? 2 : 1));
            } else if (timeVal < 9.5) {
                // If Monday morning, go back to Friday. Else yesterday.
                const dayNum = now.getDay();
                closeDate.setDate(now.getDate() - (dayNum === 1 ? 3 : 1));
            }
            // else today is the close date (after 4pm)

            return `${reason} - Data as of ${closeDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;
        }
    };

    useEffect(() => {
        if (isStockAccount) {
            loadStockData();
        }
    }, [account.id]);

    // Fetch account transactions for all account types
    useEffect(() => {
        const loadAccountTransactions = async () => {
            try {
                const res = await apiService.getAccountTransactions(account.id);
                if (res && res.success) {
                    setAccountTransactions(res.data || []);
                }
            } catch (e) {
                console.error('Failed to load account transactions', e);
            }
        };
        loadAccountTransactions();
    }, [account.id]);

    useEffect(() => {
        if (isStockAccount && activeTab === 'performance') {
            loadHistory(historyPeriod);
        }
    }, [activeTab, historyPeriod, isStockAccount, loadHistory]);

    const loadStockData = async () => {
        setIsLoading(true);
        try {
            const [txnsRes, holdingsRes, portfolioRes, accountsRes] = await Promise.all([
                apiService.getTransactions(),
                apiService.getHoldings(),
                apiService.getPortfolio(false),
                apiService.getAccounts()
            ]);

            if (txnsRes && (txnsRes as any).success) setTransactions((txnsRes as any).data || []);
            if (holdingsRes && (holdingsRes as any).success) setHoldings((holdingsRes as any).data || []);
            if (portfolioRes && (portfolioRes as any).success) {
                setPortfolioSummary((portfolioRes as any).data || {});
                // Trigger parent update to refresh sidebar balance with new portfolio value
                onUpdate();
            }
            if (accountsRes && (accountsRes as any).success) {
                setAllAccounts((accountsRes as any).data || []);
            }

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
            const payload: any = {
                symbol: txnSymbol.toUpperCase(),
                type: txnType,
                quantity: parseFloat(txnQty),
                pricePerShare: parseFloat(txnPrice),
                transactionDate: txnDate,
                totalCost: parseFloat(txnQty) * parseFloat(txnPrice)
            };
            if (txnAccountId) {
                payload.accountId = parseInt(txnAccountId);
            }
            await apiService.addTransaction(payload);
            setIsAddTxnOpen(false);
            // Reset form
            setTxnSymbol('');
            setTxnQty('');
            setTxnPrice('');
            setTxnAccountId('');
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

    // Calculate Day Change
    const totalCurrentValue = isStockAccount ? holdings.reduce((sum, h) => sum + (h.currentValue || 0), 0) : 0;
    const totalDayChange = isStockAccount ? holdings.reduce((sum, h) => sum + ((h.dailyChange || 0) * h.quantity), 0) : 0;
    const previousValue = totalCurrentValue - totalDayChange;
    const totalDayChangePercent = previousValue !== 0 ? (totalDayChange / previousValue) * 100 : 0;

    return (
        <div className="p-6 animate-fadeIn">
            <div className={`grid grid-cols-1 gap-6 ${isStockAccount ? 'lg:grid-cols-[80fr_20fr]' : ''}`}>

                {/* Left Column: Header + Content */}
                <div className="space-y-6">
                    {/* Account Header */}
                    <div className="bg-card rounded-lg p-6 shadow-sm border border-border flex justify-between items-center">
                        <div>
                            {account.isManual && (
                                <div className="flex items-center space-x-2 text-muted text-sm mb-1">
                                    <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">Manual</span>
                                </div>
                            )}
                            <h1 className="text-3xl font-bold text-main">{account.name}</h1>
                            <div className="text-sm text-muted mt-1">
                                {isStockAccount ? (
                                    getUpdateStatusMessage()
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
                                <div className="group flex items-center space-x-12">
                                    {/* Section 1: Balance (Legacy Edit logic maintained) */}
                                    <div className="flex flex-col items-end">
                                        <div className="text-sm text-muted mb-1">Current Balance</div>
                                        <div className="flex items-center">
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
                                    </div>

                                    {/* Section 2: Day Change */}
                                    {isStockAccount && (
                                        <div className="flex flex-col items-end border-l border-border pl-12">
                                            <div className="text-sm text-muted mb-1">Day Change</div>
                                            <div className={`text-2xl font-medium ${totalDayChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                {totalDayChange >= 0 ? '+' : ''}${Math.abs(totalDayChange).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                                <span className="text-lg ml-2">({totalDayChange >= 0 ? '+' : ''}{totalDayChangePercent.toFixed(2)}%)</span>
                                            </div>
                                        </div>
                                    )}

                                    {/* Section 3: YTD Return */}
                                    {isStockAccount && portfolioSummary?.totalGainLossYTD !== undefined && (
                                        <div className="flex flex-col items-end border-l border-border pl-12">
                                            <div className="text-sm text-muted mb-1">YTD Return</div>
                                            <div className={`text-2xl font-medium ${portfolioSummary.totalGainLossYTD >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                {portfolioSummary.totalGainLossYTD >= 0 ? '+' : ''}${Math.abs(portfolioSummary.totalGainLossYTD).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>

                    {isStockAccount ? (
                        <>
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
                                                            <th className="py-3 px-4 text-right">Value</th>
                                                            <th className="py-3 px-4 text-right">Current Price</th>
                                                            <th className="py-3 px-4 text-right">Today's Change</th>
                                                            <th className="py-3 px-4 text-right">Total Gain/Loss</th>
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
                                                                <td className="py-3 px-4 text-right font-medium text-main">${(h.currentValue || 0).toLocaleString()}</td>
                                                                <td className="py-3 px-4 text-right">
                                                                    <span className="font-medium text-main">
                                                                        ${(h.currentPrice || 0).toFixed(2)}
                                                                    </span>
                                                                </td>
                                                                <td className="py-3 px-4 text-right">
                                                                    <div className="flex flex-col items-end">
                                                                        <span className={`font-medium ${(h.dailyChange || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                                            {(h.dailyChange || 0) * h.quantity >= 0 ? '+' : ''}${(Math.abs((h.dailyChange || 0) * h.quantity)).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                                                        </span>
                                                                        <span className={`text-xs ${(h.dailyChange || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                                            ({(h.dailyChange || 0) >= 0 ? '+' : ''}{(h.dailyChangePercentage || 0).toFixed(2)}%)
                                                                        </span>
                                                                    </div>
                                                                </td>
                                                                <div className="flex flex-col items-end">
                                                                    <span className={`font-medium ${(h.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                                                        {(h.totalGainLoss || 0) >= 0 ? '+' : ''}${Math.abs(h.totalGainLoss || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                                                    </span>
                                                                    <span className="text-xs">
                                                                        ({(h.totalGainLoss || 0) >= 0 ? '+' : ''}{(h.totalGainLossPercentage || 0).toFixed(2)}%)
                                                                    </span>
                                                                </div>
                                                            </tr>
                                                        ))}
                                                    </tbody>
                                                </table>
                                            </div>

                                            {/* Right Column: Market Indices + Stock Performance Chart */}
                                            <div className="flex flex-col gap-4">
                                                {holdings.length > 0 && (
                                                    <div className="flex-1">
                                                        <StockPerformanceChart holdings={holdings} externalSymbol={selectedChartSymbol} />
                                                    </div>
                                                )}
                                            </div>
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
                                        <div className="bg-card rounded-lg p-6 animate-fadeIn">
                                            {activeSandboxPortfolioId ? (
                                                <SandboxPortfolioDetailComponent
                                                    portfolioId={activeSandboxPortfolioId}
                                                    onBack={() => setActiveSandboxPortfolioId(null)}
                                                />
                                            ) : (
                                                <SandboxDashboard
                                                    onSelectPortfolio={(id) => setActiveSandboxPortfolioId(id)}
                                                />
                                            )}
                                        </div>
                                    )}
                                </>
                            )}
                        </>
                    ) : (
                        /* Default/Manual Account View — Transaction History */
                        <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                            <h3 className="text-lg font-medium text-main mb-4 flex items-center">
                                <History className="h-5 w-5 mr-2" />
                                Transaction History
                            </h3>
                            {accountTransactions.length === 0 ? (
                                <div className="text-center py-8">
                                    <History className="h-10 w-10 text-muted mx-auto mb-3 opacity-40" />
                                    <p className="text-muted text-sm">No transactions yet for this account.</p>
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                            <tr className="border-b border-border text-muted">
                                                <th className="text-left py-2 px-3 font-medium">Date</th>
                                                <th className="text-left py-2 px-3 font-medium">Description</th>
                                                <th className="text-right py-2 px-3 font-medium">Amount</th>
                                                <th className="text-right py-2 px-3 font-medium">Balance</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {accountTransactions.map((txn: any) => (
                                                <tr key={txn.id} className="border-b border-border/50 hover:bg-page/50 transition-colors">
                                                    <td className="py-2.5 px-3 text-main">
                                                        {new Date(txn.transactionDate).toLocaleDateString()}
                                                    </td>
                                                    <td className="py-2.5 px-3 text-main">{txn.description}</td>
                                                    <td className={`py-2.5 px-3 text-right font-medium ${txn.type === 'CREDIT' ? 'text-green-600' : 'text-red-600'}`}>
                                                        {txn.type === 'CREDIT' ? '+' : '-'}${Number(txn.amount).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                                    </td>
                                                    <td className="py-2.5 px-3 text-right text-main font-medium">
                                                        ${Number(txn.newBalance).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </div>

                {isStockAccount && (
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
                                    <span className="text-muted">Total Return</span>
                                    <span className={`font-medium ${totalGainLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                        {totalGainLoss >= 0 ? '+' : ''}${totalGainLoss.toLocaleString()}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
            {/* Add Transaction Modal */}
            {
                isAddTxnOpen && (
                    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                        <div className="bg-card rounded-lg border border-border shadow-lg p-6 w-full max-w-md animate-in fade-in zoom-in duration-200">
                            <h3 className="text-lg font-bold text-main mb-4">Add Real Transaction</h3>
                            <form onSubmit={handleAddTransactionSubmit} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">From Account</label>
                                    <select
                                        className="w-full bg-page border border-border rounded px-3 py-2 text-main"
                                        value={txnAccountId}
                                        onChange={e => setTxnAccountId(e.target.value)}
                                    >
                                        <option value="">Default (Fidelity Cash)</option>
                                        {allAccounts.filter(a => a.type === 'CASH').map(a => (
                                            <option key={a.id} value={a.id}>{a.name} (${(a.balance || 0).toLocaleString()})</option>
                                        ))}
                                    </select>
                                </div>
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
