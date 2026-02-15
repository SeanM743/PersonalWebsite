import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Briefcase } from 'lucide-react';
import { apiService } from '../../services/apiService';
import { SandboxPortfolio } from './SandboxTypes';
import LoadingSpinner from '../UI/LoadingSpinner';

interface SandboxDashboardProps {
    onSelectPortfolio?: (id: number) => void;
}

const SandboxDashboard: React.FC<SandboxDashboardProps> = ({ onSelectPortfolio }) => {
    const navigate = useNavigate();
    const [portfolios, setPortfolios] = useState<SandboxPortfolio[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newPortfolio, setNewPortfolio] = useState({
        name: '',
        description: '',
        initialBalance: 100000
    });

    useEffect(() => {
        fetchPortfolios();
    }, []);

    const fetchPortfolios = async () => {
        try {
            const data = await apiService.getSandboxPortfolios();
            setPortfolios(data);
        } catch (error) {
            console.error('Failed to fetch sandbox portfolios:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const [editingPortfolioId, setEditingPortfolioId] = useState<number | null>(null);

    const handleCreateSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            if (editingPortfolioId) {
                await apiService.updateSandboxPortfolio(editingPortfolioId, newPortfolio);
            } else {
                await apiService.createSandboxPortfolio(newPortfolio);
            }
            setIsCreateModalOpen(false);
            resetForm();
            fetchPortfolios();
        } catch (error) {
            console.error('Failed to save portfolio:', error);
        }
    };

    const resetForm = () => {
        setNewPortfolio({ name: '', description: '', initialBalance: 100000 });
        setEditingPortfolioId(null);
    };

    const openCreateModal = () => {
        resetForm();
        setIsCreateModalOpen(true);
    };

    const openEditModal = (e: React.MouseEvent, portfolio: SandboxPortfolio) => {
        e.stopPropagation();
        setNewPortfolio({
            name: portfolio.name,
            description: portfolio.description,
            initialBalance: portfolio.initialBalance
        });
        setEditingPortfolioId(portfolio.id);
        setIsCreateModalOpen(true);
    };

    const handlePortfolioClick = (id: number) => {
        if (onSelectPortfolio) {
            onSelectPortfolio(id);
        } else {
            navigate(`/sandbox/${id}`);
        }
    };

    if (isLoading) return <div className="flex justify-center p-8"><LoadingSpinner /></div>;

    return (
        <div className="container mx-auto px-4 py-8 max-w-7xl animate-fadeIn">
            {!onSelectPortfolio && (
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-main">Stock Sandbox</h1>
                        <p className="text-muted mt-1">Paper trade and test strategies risk-free</p>
                    </div>
                </div>
            )}

            <div className="flex justify-between items-center mb-6">
                {onSelectPortfolio && (
                    <div>
                        <h3 className="text-lg font-bold text-main">Your Sandbox Portfolios</h3>
                        <p className="text-sm text-muted">Select a portfolio to view details and trade.</p>
                    </div>
                )}
                <button
                    onClick={openCreateModal}
                    className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                >
                    <Plus size={20} />
                    New Portfolio
                </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {portfolios.map((portfolio) => (
                    <div
                        key={portfolio.id}
                        onClick={() => handlePortfolioClick(portfolio.id)}
                        className="bg-card rounded-xl shadow-sm border border-border p-6 hover:shadow-md transition cursor-pointer group relative"
                    >
                        <div className="absolute top-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity">
                            <button
                                onClick={(e) => openEditModal(e, portfolio)}
                                className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-full transition"
                                title="Edit Portfolio"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
                            </button>
                        </div>

                        <div className="flex justify-between items-start mb-4">
                            <div className="p-3 bg-blue-50 rounded-lg group-hover:bg-blue-100 transition">
                                <Briefcase className="text-blue-600" size={24} />
                            </div>
                        </div>

                        <h3 className="text-xl font-semibold text-main mb-2 pr-8">{portfolio.name}</h3>
                        <p className="text-muted text-sm mb-4 line-clamp-2 h-10">{portfolio.description}</p>

                        <div className="pt-4 border-t border-border">
                            <div className="flex justify-between items-end">
                                <div>
                                    <p className="text-xs text-muted uppercase font-medium">Starting Cash</p>
                                    <p className="text-lg font-semibold text-main">
                                        ${portfolio.initialBalance.toLocaleString()}
                                    </p>
                                </div>
                                <div className="text-right">
                                    <p className="text-xs text-muted uppercase font-medium">Available</p>
                                    <p className="text-lg font-medium text-main/80">
                                        ${portfolio.currentBalance.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}

                {/* Create New Card */}
                <div
                    onClick={openCreateModal}
                    className="bg-page rounded-xl border-2 border-dashed border-border p-6 flex flex-col items-center justify-center text-center hover:border-blue-400 hover:bg-blue-50/10 transition cursor-pointer min-h-[200px]"
                >
                    <div className="p-3 bg-card rounded-full shadow-sm mb-3">
                        <Plus className="text-muted" size={24} />
                    </div>
                    <h3 className="text-lg font-medium text-main">Create New Portfolio</h3>
                    <p className="text-muted text-sm mt-1">Test a new strategy</p>
                </div>
            </div>

            {/* Create/Edit Modal */}
            {isCreateModalOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-card rounded-xl max-w-md w-full p-6 animate-in fade-in zoom-in duration-200">
                        <h2 className="text-2xl font-bold mb-4 text-main">{editingPortfolioId ? 'Edit Portfolio' : 'New Portfolio'}</h2>
                        <form onSubmit={handleCreateSubmit}>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Portfolio Name</label>
                                    <input
                                        type="text"
                                        required
                                        className="w-full p-2 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                        value={newPortfolio.name}
                                        onChange={(e) => setNewPortfolio({ ...newPortfolio, name: e.target.value })}
                                        placeholder="e.g. High Risk Tech"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Description</label>
                                    <textarea
                                        className="w-full p-2 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                        value={newPortfolio.description}
                                        onChange={(e) => setNewPortfolio({ ...newPortfolio, description: e.target.value })}
                                        placeholder="What is the strategy for this portfolio?"
                                        rows={3}
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-muted mb-1">Initial Balance</label>
                                    <select
                                        className="w-full p-2 border border-border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none bg-page text-main"
                                        value={newPortfolio.initialBalance}
                                        onChange={(e) => setNewPortfolio({ ...newPortfolio, initialBalance: Number(e.target.value) })}
                                    >
                                        <option value={10000}>$10,000</option>
                                        <option value={50000}>$50,000</option>
                                        <option value={100000}>$100,000</option>
                                        <option value={1000000}>$1,000,000</option>
                                    </select>
                                    {editingPortfolioId && (
                                        <p className="text-xs text-amber-600 mt-1">
                                            Changing initial balance will adjust current cash by the difference.
                                        </p>
                                    )}
                                </div>
                            </div>
                            <div className="flex justify-end gap-3 mt-6">
                                <button
                                    type="button"
                                    onClick={() => { setIsCreateModalOpen(false); resetForm(); }}
                                    className="px-4 py-2 text-muted hover:bg-page rounded-lg transition"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                                >
                                    {editingPortfolioId ? 'Save Changes' : 'Create Portfolio'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default SandboxDashboard;
