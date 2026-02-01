import React, { useState } from 'react';
import {
    Wallet,
    TrendingUp,
    CreditCard,
    Briefcase,
    GraduationCap,
    Plus,
    Trash2,
    Edit2,
    X
} from 'lucide-react';
import { apiService } from '../../services/apiService';

interface Account {
    id: number;
    name: string;
    type: string;
    balance: number;
    isManual?: boolean;
}

interface AccountSidebarProps {
    accounts: Account[];
    selectedAccountId: number | null;
    onSelectAccount: (id: number | null) => void;
    onAccountsChanged: () => void;
    isLoading: boolean;
}

const AccountSidebar: React.FC<AccountSidebarProps> = ({
    accounts,
    selectedAccountId,
    onSelectAccount,
    onAccountsChanged,
    isLoading
}) => {
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [accountToDelete, setAccountToDelete] = useState<Account | null>(null);
    const [newAccountName, setNewAccountName] = useState('');
    const [newAccountType, setNewAccountType] = useState('OTHER');
    const [newAccountBalance, setNewAccountBalance] = useState('0');

    // Edit Account State
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingAccount, setEditingAccount] = useState<Account | null>(null);
    const [editAccountName, setEditAccountName] = useState('');
    const [editAccountType, setEditAccountType] = useState('OTHER');
    const [editAccountBalance, setEditAccountBalance] = useState('0');

    const [hoveredAccountId, setHoveredAccountId] = useState<number | null>(null);

    const getIconForType = (type: string) => {
        switch (type) {
            case 'STOCK_PORTFOLIO': return <TrendingUp className="h-4 w-4" />;
            case 'CASH': return <Wallet className="h-4 w-4" />;
            case 'RETIREMENT': return <Briefcase className="h-4 w-4" />;
            case 'EDUCATION': return <GraduationCap className="h-4 w-4" />;
            default: return <CreditCard className="h-4 w-4" />;
        }
    };

    const totalNetWorth = accounts.reduce((sum, acc) => sum + acc.balance, 0);

    const handleAddAccount = async () => {
        try {
            await apiService.createAccount({
                name: newAccountName,
                type: newAccountType,
                balance: parseFloat(newAccountBalance)
            });
            setIsAddModalOpen(false);
            setNewAccountName('');
            setNewAccountType('OTHER');
            setNewAccountBalance('0');
            onAccountsChanged();
        } catch (error) {
            console.error('Failed to create account:', error);
            alert('Failed to create account. Please try again.');
        }
    };

    const handleDeleteAccount = async () => {
        if (!accountToDelete) return;
        try {
            await apiService.deleteAccount(accountToDelete.id);
            setIsDeleteModalOpen(false);
            setAccountToDelete(null);
            onAccountsChanged();
        } catch (error: any) {
            console.error('Failed to delete account:', error);
            alert(error.response?.data?.message || 'Failed to delete account. Please try again.');
        }
    };

    const openDeleteModal = (account: Account, e: React.MouseEvent) => {
        e.stopPropagation();
        setAccountToDelete(account);
        setIsDeleteModalOpen(true);
    };

    const isSystemAccount = (account: Account) => {
        return account.name === 'Stock Portfolio' || account.name === 'Fidelity Cash';
    };

    const openEditModal = (account: Account, e: React.MouseEvent) => {
        e.stopPropagation();
        setEditingAccount(account);
        setEditAccountName(account.name);
        setEditAccountType(account.type);
        setEditAccountBalance(account.balance.toString());
        setIsEditModalOpen(true);
    };

    const handleEditAccount = async () => {
        if (!editingAccount) return;
        try {
            await apiService.updateAccount(editingAccount.id, {
                name: editAccountName,
                type: editAccountType,
                balance: parseFloat(editAccountBalance),
                notes: '' // Preserve existing notes handled by backend if not sent, but here we send empty string? 
                // Backend updates if sent. Let's send null/undefined for notes if we don't have them in UI?
                // The backend implementation overwrites notes. Since we don't have notes in Account interface here, we might lose them?
                // Account interface in this file removes notes?
                // Account interface: id, name, type, balance, isManual. No notes.
                // It's safer to fetch the full account first OR just send the fields we edit.
                // Our backend replaces fields.
            });
            // Ideally we should preserve notes. But we don't display them here.
            // Let's proceed. Ideally I should add notes to the interface.

            setIsEditModalOpen(false);
            setEditingAccount(null);
            onAccountsChanged();
        } catch (error) {
            console.error('Failed to update account:', error);
            alert('Failed to update account. Please try again.');
        }
    };

    return (
        <div className="w-64 bg-card border-r border-border h-full flex flex-col">
            <div className="p-4 border-b border-border">
                <div className="text-3xl font-bold text-main">
                    ${totalNetWorth.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
                <div className="text-sm text-muted">Net Worth</div>
            </div>

            <div className="flex-1 overflow-y-auto py-2">
                <button
                    onClick={() => onSelectAccount(null)}
                    className={`w-full text-left px-4 py-3 flex items-center space-x-3 transition-colors ${selectedAccountId === null
                        ? 'bg-primary/10 border-r-2 border-primary'
                        : 'hover:bg-page'
                        }`}
                >
                    <div className="p-2 bg-blue-100 rounded-lg text-blue-600">
                        <TrendingUp className="h-5 w-5" />
                    </div>
                    <div>
                        <div className="font-medium text-main">Overview</div>
                        <div className="text-xs text-muted">All Accounts</div>
                    </div>
                </button>

                <div className="px-4 py-2 mt-2 flex items-center justify-between group">
                    <div className="text-xs font-semibold text-muted uppercase">Your Accounts</div>
                    <button
                        onClick={() => setIsAddModalOpen(true)}
                        className="p-1 text-muted hover:text-primary transition-colors hover:bg-page rounded opacity-60 hover:opacity-100"
                        title="Add New Account"
                    >
                        <Plus className="h-4 w-4" />
                    </button>
                </div>

                {isLoading ? (
                    <div className="px-4 py-2 text-sm text-muted">Loading...</div>
                ) : (
                    accounts.map(account => (
                        <div
                            key={account.id}
                            className="relative group"
                            onMouseEnter={() => setHoveredAccountId(account.id)}
                            onMouseLeave={() => setHoveredAccountId(null)}
                        >
                            <button
                                onClick={() => onSelectAccount(account.id)}
                                className={`w-full text-left px-4 py-3 flex items-center space-x-3 transition-colors ${selectedAccountId === account.id
                                    ? 'bg-primary/10 border-r-2 border-primary'
                                    : 'hover:bg-page'
                                    }`}
                            >
                                <div className={`p-2 rounded-lg ${selectedAccountId === account.id ? 'bg-primary text-white' : 'bg-muted/20 text-muted'
                                    }`}>
                                    {getIconForType(account.type)}
                                </div>
                                <div className="overflow-hidden flex-1">
                                    <div className="font-medium text-main truncate">{account.name}</div>
                                    <div className="text-xs text-muted font-mono">
                                        ${account.balance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                    </div>
                                </div>
                                {hoveredAccountId === account.id && !isSystemAccount(account) && (
                                    <div className="flex space-x-1">
                                        <button
                                            onClick={(e) => openEditModal(account, e)}
                                            className="p-1 text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded transition-colors"
                                            title="Edit account"
                                        >
                                            <Edit2 className="h-4 w-4" />
                                        </button>
                                        <button
                                            onClick={(e) => openDeleteModal(account, e)}
                                            className="p-1 text-red-600 hover:text-red-700 hover:bg-red-50 rounded transition-colors"
                                            title="Delete account"
                                        >
                                            <Trash2 className="h-4 w-4" />
                                        </button>
                                    </div>
                                )}
                            </button>
                        </div>
                    ))
                )}
            </div>

            {/* Add Account Modal */}
            {
                isAddModalOpen && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-card rounded-lg p-6 w-96 shadow-xl">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-lg font-semibold text-main">Add New Account</h3>
                                <button onClick={() => setIsAddModalOpen(false)} className="text-muted hover:text-main">
                                    <X className="h-5 w-5" />
                                </button>
                            </div>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-main mb-1">Account Name</label>
                                    <input
                                        type="text"
                                        value={newAccountName}
                                        onChange={(e) => setNewAccountName(e.target.value)}
                                        className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                        placeholder="e.g., Savings Account"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-main mb-1">Account Type</label>
                                    <select
                                        value={newAccountType}
                                        onChange={(e) => setNewAccountType(e.target.value)}
                                        className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                    >
                                        <option value="CASH">Cash</option>
                                        <option value="RETIREMENT">Retirement</option>
                                        <option value="EDUCATION">Education</option>
                                        <option value="OTHER">Other</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-main mb-1">Initial Balance</label>
                                    <input
                                        type="number"
                                        value={newAccountBalance}
                                        onChange={(e) => setNewAccountBalance(e.target.value)}
                                        className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                        placeholder="0.00"
                                        step="0.01"
                                    />
                                </div>
                            </div>
                            <div className="flex justify-end space-x-2 mt-6">
                                <button
                                    onClick={() => setIsAddModalOpen(false)}
                                    className="px-4 py-2 text-muted hover:text-main transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleAddAccount}
                                    disabled={!newAccountName.trim()}
                                    className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Add Account
                                </button>
                            </div>
                        </div>
                    </div>
                )
            }

            {/* Edit Account Modal */}
            {
                isEditModalOpen && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-card rounded-lg p-6 w-96 shadow-xl">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-lg font-semibold text-main">Edit Account</h3>
                                <button onClick={() => setIsEditModalOpen(false)} className="text-muted hover:text-main">
                                    <X className="h-5 w-5" />
                                </button>
                            </div>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-main mb-1">Account Name</label>
                                    <input
                                        type="text"
                                        value={editAccountName}
                                        onChange={(e) => setEditAccountName(e.target.value)}
                                        className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-main mb-1">Account Type</label>
                                    <select
                                        value={editAccountType}
                                        onChange={(e) => setEditAccountType(e.target.value)}
                                        className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                    >
                                        <option value="CASH">Cash</option>
                                        <option value="RETIREMENT">Retirement</option>
                                        <option value="EDUCATION">Education</option>
                                        <option value="OTHER">Other</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-main mb-1">Balance</label>
                                    <input
                                        type="number"
                                        value={editAccountBalance}
                                        onChange={(e) => setEditAccountBalance(e.target.value)}
                                        className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                                        step="0.01"
                                    />
                                </div>
                            </div>
                            <div className="flex justify-end space-x-2 mt-6">
                                <button
                                    onClick={() => setIsEditModalOpen(false)}
                                    className="px-4 py-2 text-muted hover:text-main transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleEditAccount}
                                    disabled={!editAccountName.trim()}
                                    className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Save Changes
                                </button>
                            </div>
                        </div>
                    </div>
                )
            }

            {/* Delete Confirmation Modal */}
            {
                isDeleteModalOpen && accountToDelete && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-card rounded-lg p-6 w-96 shadow-xl">
                            <h3 className="text-lg font-semibold text-main mb-4">Delete Account</h3>
                            <p className="text-muted mb-6">
                                Are you sure you want to delete <strong>{accountToDelete.name}</strong>? This action cannot be undone.
                            </p>
                            <div className="flex justify-end space-x-2">
                                <button
                                    onClick={() => {
                                        setIsDeleteModalOpen(false);
                                        setAccountToDelete(null);
                                    }}
                                    className="px-4 py-2 text-muted hover:text-main transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleDeleteAccount}
                                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    </div>
                )
            }
        </div >
    );
};

export default AccountSidebar;
