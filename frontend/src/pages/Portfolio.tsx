import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { 
  Plus, 
  RefreshCw, 
  TrendingUp, 
  TrendingDown, 
  Edit, 
  Trash2,
  DollarSign
} from 'lucide-react';

interface Holding {
  id: number;
  symbol: string;
  quantity: number;
  purchasePrice: number;
  totalInvestment: number;
  notes?: string;
}

interface PortfolioSummary {
  totalInvestment: number;
  currentValue: number;
  totalGainLoss: number;
  totalGainLossPercentage: number;
  dailyChange: number;
  dailyChangePercentage: number;
  totalPositions: number;
  stockPerformances: any[];
}

const Portfolio: React.FC = () => {
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingHolding, setEditingHolding] = useState<Holding | null>(null);
  const { error, success } = useNotification();

  const loadPortfolioData = async (showRefreshMessage = false) => {
    try {
      setIsRefreshing(true);
      
      const [portfolioRes, holdingsRes] = await Promise.all([
        apiService.getPortfolio(true),
        apiService.getHoldings(),
      ]);

      if (portfolioRes.success) {
        setPortfolio(portfolioRes.data);
      }
      
      if (holdingsRes.success) {
        setHoldings(holdingsRes.data);
      }
      
      if (showRefreshMessage) {
        success('Portfolio refreshed successfully');
      }
    } catch (err: any) {
      error('Failed to load portfolio', err.message);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadPortfolioData();
  }, []);

  const handleRefresh = async () => {
    try {
      await apiService.refreshPortfolio();
      await loadPortfolioData(true);
    } catch (err: any) {
      error('Failed to refresh portfolio', err.message);
    }
  };

  const handleDeleteHolding = async (symbol: string) => {
    if (!confirm(`Are you sure you want to delete ${symbol}?`)) return;
    
    try {
      await apiService.deleteHolding(symbol);
      success('Holding deleted successfully');
      loadPortfolioData();
    } catch (err: any) {
      error('Failed to delete holding', err.message);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Portfolio</h1>
        <div className="flex space-x-3">
          <button
            onClick={() => setShowAddModal(true)}
            className="btn-primary flex items-center space-x-2"
          >
            <Plus className="h-4 w-4" />
            <span>Add Stock</span>
          </button>
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="btn-secondary flex items-center space-x-2"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Portfolio Summary */}
      {portfolio && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Value</p>
                <p className="text-2xl font-bold text-gray-900">
                  ${portfolio.currentValue?.toFixed(2) || '0.00'}
                </p>
              </div>
              <DollarSign className="h-8 w-8 text-blue-500" />
            </div>
          </div>

          <div className="card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Gain/Loss</p>
                <p className={`text-2xl font-bold ${
                  (portfolio.totalGainLoss || 0) >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {(portfolio.totalGainLoss || 0) >= 0 ? '+' : ''}
                  ${portfolio.totalGainLoss?.toFixed(2) || '0.00'}
                </p>
              </div>
              {(portfolio.totalGainLoss || 0) >= 0 ? (
                <TrendingUp className="h-8 w-8 text-green-500" />
              ) : (
                <TrendingDown className="h-8 w-8 text-red-500" />
              )}
            </div>
          </div>

          <div className="card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Daily Change</p>
                <p className={`text-2xl font-bold ${
                  (portfolio.dailyChange || 0) >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {(portfolio.dailyChange || 0) >= 0 ? '+' : ''}
                  ${portfolio.dailyChange?.toFixed(2) || '0.00'}
                </p>
              </div>
              {(portfolio.dailyChange || 0) >= 0 ? (
                <TrendingUp className="h-8 w-8 text-green-500" />
              ) : (
                <TrendingDown className="h-8 w-8 text-red-500" />
              )}
            </div>
          </div>

          <div className="card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Positions</p>
                <p className="text-2xl font-bold text-gray-900">
                  {portfolio.totalPositions || 0}
                </p>
              </div>
              <div className="h-8 w-8 bg-purple-100 rounded-full flex items-center justify-center">
                <span className="text-purple-600 font-bold">
                  {portfolio.totalPositions || 0}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Holdings Table */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Holdings</h2>
          <div className="text-sm text-gray-500">
            {holdings.length} position{holdings.length !== 1 ? 's' : ''}
          </div>
        </div>

        {holdings.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Symbol
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Quantity
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Purchase Price
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Total Investment
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {holdings.map((holding) => (
                  <tr key={holding.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="font-medium text-gray-900">{holding.symbol}</div>
                      {holding.notes && (
                        <div className="text-sm text-gray-500">{holding.notes}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {holding.quantity}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      ${holding.purchasePrice?.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      ${holding.totalInvestment?.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => setEditingHolding(holding)}
                          className="text-blue-600 hover:text-blue-900"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteHolding(holding.symbol)}
                          className="text-red-600 hover:text-red-900"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-12">
            <TrendingUp className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No holdings</h3>
            <p className="mt-1 text-sm text-gray-500">
              Get started by adding your first stock position.
            </p>
            <div className="mt-6">
              <button
                onClick={() => setShowAddModal(true)}
                className="btn-primary"
              >
                <Plus className="h-4 w-4 mr-2" />
                Add Stock
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Add/Edit Modal would go here */}
      {(showAddModal || editingHolding) && (
        <HoldingModal
          holding={editingHolding}
          onClose={() => {
            setShowAddModal(false);
            setEditingHolding(null);
          }}
          onSave={() => {
            setShowAddModal(false);
            setEditingHolding(null);
            loadPortfolioData();
          }}
        />
      )}
    </div>
  );
};

// Simple modal component for adding/editing holdings
const HoldingModal: React.FC<{
  holding?: Holding | null;
  onClose: () => void;
  onSave: () => void;
}> = ({ holding, onClose, onSave }) => {
  const [formData, setFormData] = useState({
    symbol: holding?.symbol || '',
    quantity: holding?.quantity || '',
    purchasePrice: holding?.purchasePrice || '',
    notes: holding?.notes || '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { error, success } = useNotification();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const data = {
        symbol: formData.symbol.toUpperCase(),
        quantity: parseFloat(formData.quantity as string),
        purchasePrice: parseFloat(formData.purchasePrice as string),
        notes: formData.notes,
      };

      if (holding) {
        await apiService.updateHolding(holding.symbol, data);
        success('Holding updated successfully');
      } else {
        await apiService.addHolding(data);
        success('Holding added successfully');
      }
      
      onSave();
    } catch (err: any) {
      error('Failed to save holding', err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" onClick={onClose} />
        
        <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
          <form onSubmit={handleSubmit}>
            <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                {holding ? 'Edit Holding' : 'Add New Holding'}
              </h3>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Symbol</label>
                  <input
                    type="text"
                    required
                    className="input-field mt-1"
                    value={formData.symbol}
                    onChange={(e) => setFormData({ ...formData, symbol: e.target.value })}
                    placeholder="AAPL"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700">Quantity</label>
                  <input
                    type="number"
                    step="0.00000001"
                    required
                    className="input-field mt-1"
                    value={formData.quantity}
                    onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                    placeholder="10"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700">Purchase Price</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    className="input-field mt-1"
                    value={formData.purchasePrice}
                    onChange={(e) => setFormData({ ...formData, purchasePrice: e.target.value })}
                    placeholder="150.00"
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700">Notes (Optional)</label>
                  <textarea
                    className="input-field mt-1"
                    rows={3}
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    placeholder="Any additional notes..."
                  />
                </div>
              </div>
            </div>
            
            <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
              <button
                type="submit"
                disabled={isSubmitting}
                className="btn-primary sm:ml-3 sm:w-auto"
              >
                {isSubmitting ? <LoadingSpinner size="small" className="mr-2" /> : null}
                {holding ? 'Update' : 'Add'} Holding
              </button>
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary mt-3 sm:mt-0 sm:w-auto"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Portfolio;