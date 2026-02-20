import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/apiService';
import { usePortfolio } from '../../contexts/PortfolioContext';
import WatchlistWidget from './WatchlistWidget';
import MarketIndices from './MarketIndices';
import MarketHeatMap from './MarketHeatMap';
import PortfolioMetrics from './PortfolioMetrics';
import AssetAllocation from './AssetAllocation';
import TopHoldingsList from './TopHoldingsList';
import PortfolioPerformanceChart from './PortfolioPerformanceChart';
import { Link } from 'react-router-dom';
import { FlaskConical, ExternalLink } from 'lucide-react';

interface Holding {
    id: number;
    symbol: string;
    quantity: number;
    purchasePrice: number;
    totalInvestment: number;
    currentPrice?: number;
    currentValue?: number;
    totalGainLoss?: number;
    totalGainLossPercentage?: number;
}

interface CompletePortfolioSummary {
    stockPortfolio: any;
    totalPortfolioValue: number;
    allocationByType: Record<string, number>;
    totalStockValue: number;
    totalGainLossYTD: number;
    totalGainLossPercentageYTD: number;
    totalGainLoss7d?: number;
    totalGainLossPercentage7d?: number;
    totalGainLoss1m?: number;
    totalGainLossPercentage1m?: number;
    totalGainLoss3m?: number;
    totalGainLossPercentage3m?: number;
}

interface PortfolioResponse<T> {
    success: boolean;
    data?: T;
    message?: string;
}

const PortfolioOverview: React.FC = () => {
    const [completePortfolio, setCompletePortfolio] = useState<CompletePortfolioSummary | null>(null);
    const [holdings, setHoldings] = useState<Holding[]>([]);
    const [selectedPeriod, setSelectedPeriod] = useState('1M');

    // Use Portfolio Context for history data
    const { historyData, isHistoryLoading, loadHistory } = usePortfolio();

    const loadPortfolioData = async () => {
        try {
            const [completeRes, holdingsRes] = await Promise.all([
                apiService.getCompletePortfolio(true) as Promise<PortfolioResponse<CompletePortfolioSummary>>,
                apiService.getHoldings() as Promise<PortfolioResponse<Holding[]>>,
            ]);

            if (completeRes.success) {
                setCompletePortfolio(completeRes.data || null);
            }

            if (holdingsRes.success) {
                setHoldings(holdingsRes.data || []);
            }
        } catch (err: any) {
            console.error('Failed to load portfolio', err);
        }
    };

    useEffect(() => {
        loadPortfolioData();
    }, []);

    // Load history data when period changes (lazy loaded, not on mount)
    useEffect(() => {
        if (selectedPeriod) {
            loadHistory(selectedPeriod);
        }
    }, [selectedPeriod, loadHistory]);

    // Calculate top holding by current value
    const topHolding = holdings.reduce((prev, current) => {
        if (!prev) return current;
        const prevValue = prev.currentValue || prev.totalInvestment;
        const currentValue = current.currentValue || current.totalInvestment;
        return (currentValue > prevValue) ? current : prev;
    }, holdings[0]);

    return (
        <div className="space-y-6 p-6 animate-fadeIn">
            <div className="grid grid-cols-1 xl:grid-cols-[3fr_1fr] gap-6">
                {/* Left Column: Metrics + Charts + Allocation */}
                <div className="space-y-6">
                    {/* Top Metrics Cards */}
                    <PortfolioMetrics
                        completePortfolio={completePortfolio}
                        topHolding={topHolding}
                    />

                    {/* Portfolio Performance Section */}
                    <PortfolioPerformanceChart
                        selectedPeriod={selectedPeriod}
                        onSelectPeriod={setSelectedPeriod}
                        isHistoryLoading={isHistoryLoading}
                        historyData={historyData}
                    />

                    {/* Bottom Split: Allocation + Top Holdings */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <AssetAllocation
                            allocationByType={completePortfolio?.allocationByType}
                            totalPortfolioValue={completePortfolio?.totalPortfolioValue}
                        />

                        <TopHoldingsList
                            holdings={holdings}
                        />
                    </div>

                    {/* Market Heat Map */}
                    <MarketHeatMap />
                </div>

                {/* Right Column: Sandbox + Market + Watchlist */}
                <div className="space-y-6">
                    {/* Sandbox Link */}
                    <Link to="/sandbox" className="block p-6 bg-card rounded-lg shadow-sm border border-border hover:border-blue-500 transition-all group">
                        <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center space-x-2 text-main">
                                <FlaskConical className="h-5 w-5 text-blue-500" />
                                <span className="font-semibold">Sandbox Portfolio</span>
                            </div>
                            <ExternalLink className="h-4 w-4 text-muted group-hover:text-blue-500 transition-colors" />
                        </div>
                        <p className="text-sm text-muted">Test trading strategies with paper money.</p>
                    </Link>

                    {/* Market Overview */}
                    <MarketIndices />

                    {/* Watchlist */}
                    <WatchlistWidget />
                </div>
            </div>
        </div>
    );
};

export default PortfolioOverview;
