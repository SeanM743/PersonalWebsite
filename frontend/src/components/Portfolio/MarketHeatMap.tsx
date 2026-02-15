import React, { useState, useEffect, useCallback } from 'react';
import { apiService } from '../../services/apiService';
import { Treemap, ResponsiveContainer, Tooltip } from 'recharts';
import LoadingSpinner from '../UI/LoadingSpinner';

interface HeatMapStock {
    symbol: string;
    name: string;
    price: number;
    changePercent: number;
    weight: number;
}

interface HeatMapSector {
    name: string;
    stocks: HeatMapStock[];
}

interface HeatMapData {
    sectors: HeatMapSector[];
}

// Color mapping: maps daily % change to a color
const getColor = (changePercent: number): string => {
    if (changePercent >= 3) return '#16a34a';    // vivid green
    if (changePercent >= 2) return '#22c55e';
    if (changePercent >= 1) return '#4ade80';
    if (changePercent >= 0.5) return '#86efac';
    if (changePercent >= 0) return '#bbf7d0';    // light green
    if (changePercent >= -0.5) return '#fecaca'; // light red
    if (changePercent >= -1) return '#fca5a5';
    if (changePercent >= -2) return '#f87171';
    if (changePercent >= -3) return '#ef4444';
    return '#dc2626';                             // vivid red
};

const getTextColor = (changePercent: number): string => {
    const abs = Math.abs(changePercent);
    if (abs < 0.5) return '#374151'; // dark text on light bg
    return '#ffffff'; // white text on saturated bg
};

interface TreemapContentProps {
    x: number;
    y: number;
    width: number;
    height: number;
    name: string;
    changePercent: number;
    depth: number;
    index: number;
}

const CustomTreemapContent: React.FC<TreemapContentProps> = ({ x, y, width, height, name, changePercent, depth }) => {
    if (depth !== 1) return null;
    if (width < 30 || height < 20) return null;

    const bgColor = getColor(changePercent || 0);
    const textColor = getTextColor(changePercent || 0);
    const showPercent = width > 50 && height > 35;
    const fontSize = width > 80 ? 12 : 10;

    return (
        <g>
            <rect
                x={x}
                y={y}
                width={width}
                height={height}
                style={{
                    fill: bgColor,
                    stroke: 'rgba(var(--bg-page), 0.8)',
                    strokeWidth: 2,
                    rx: 3,
                    ry: 3,
                }}
            />
            <text
                x={x + width / 2}
                y={y + height / 2 - (showPercent ? 6 : 0)}
                textAnchor="middle"
                dominantBaseline="central"
                fill={textColor}
                fontSize={fontSize}
                fontWeight="bold"
            >
                {name}
            </text>
            {showPercent && (
                <text
                    x={x + width / 2}
                    y={y + height / 2 + 10}
                    textAnchor="middle"
                    dominantBaseline="central"
                    fill={textColor}
                    fontSize={10}
                    opacity={0.9}
                >
                    {(changePercent || 0) >= 0 ? '+' : ''}{(changePercent || 0).toFixed(2)}%
                </text>
            )}
        </g>
    );
};

const CustomTooltip: React.FC<any> = ({ active, payload }) => {
    if (!active || !payload || !payload.length) return null;
    const data = payload[0]?.payload;
    if (!data || !data.symbol) return null;

    return (
        <div className="bg-card border border-border rounded-lg p-3 shadow-lg max-w-[200px]">
            <div className="font-bold text-main text-sm">{data.symbol}</div>
            <div className="text-muted text-xs truncate">{data.fullName}</div>
            <div className="mt-1.5 space-y-0.5">
                <div className="text-xs text-muted">
                    Price: <span className="text-main font-medium">${data.price?.toFixed(2)}</span>
                </div>
                <div className={`text-xs font-semibold ${data.changePercent >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                    {data.changePercent >= 0 ? '▲' : '▼'} {Math.abs(data.changePercent).toFixed(2)}%
                </div>
            </div>
        </div>
    );
};

const MarketHeatMap: React.FC = () => {
    const [heatMapData, setHeatMapData] = useState<HeatMapData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedSector, setSelectedSector] = useState<string>('all');
    const [viewMode, setViewMode] = useState<'market' | 'portfolio' | 'watchlist'>('market');

    const loadMarketHeatMap = useCallback(async () => {
        try {
            setLoading(true);
            const data = await apiService.getMarketHeatMap() as HeatMapData;
            setHeatMapData(data);
            setError(null);
        } catch (err: any) {
            console.error('Failed to load heat map data:', err);
            setError('Failed to load market heat map');
        } finally {
            setLoading(false);
        }
    }, []);

    const loadCustomHeatMap = useCallback(async (mode: 'portfolio' | 'watchlist') => {
        try {
            setLoading(true);
            let symbols: string[] = [];

            if (mode === 'portfolio') {
                const holdings = await apiService.getHoldings() as any[];
                symbols = holdings.map((h: any) => h.symbol);
            } else {
                const watchlist = await apiService.getWatchlist() as any[];
                symbols = watchlist.map((w: any) => w.symbol);
            }

            if (symbols.length === 0) {
                setHeatMapData({ sectors: [] });
                setError(mode === 'portfolio' ? 'No holdings in your portfolio' : 'Your watchlist is empty');
                setLoading(false);
                return;
            }

            const data = await apiService.getCustomHeatMap(symbols) as HeatMapData;
            setHeatMapData(data);
            setError(null);
        } catch (err: any) {
            console.error('Failed to load custom heat map:', err);
            setError('Failed to load heat map data');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (viewMode === 'market') {
            loadMarketHeatMap();
        } else {
            loadCustomHeatMap(viewMode);
        }
        setSelectedSector('all');
    }, [viewMode, loadMarketHeatMap, loadCustomHeatMap]);

    if (loading) {
        return (
            <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                <h3 className="text-lg font-semibold text-main mb-4">Market Heat Map</h3>
                <div className="h-[400px] flex items-center justify-center">
                    <LoadingSpinner />
                </div>
            </div>
        );
    }

    if (error || !heatMapData) {
        return (
            <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
                <h3 className="text-lg font-semibold text-main mb-4">Market Heat Map</h3>
                <div className="h-[200px] flex items-center justify-center text-muted">
                    {error || 'No data available'}
                </div>
            </div>
        );
    }

    // Build treemap data
    const sectors = selectedSector === 'all'
        ? heatMapData.sectors
        : heatMapData.sectors.filter(s => s.name === selectedSector);

    const treemapData = sectors.flatMap(sector =>
        sector.stocks.map(stock => ({
            name: stock.symbol,
            fullName: stock.name,
            symbol: stock.symbol,
            size: stock.weight,
            changePercent: stock.changePercent,
            price: stock.price,
            sector: sector.name,
        }))
    );

    const allSectors = heatMapData.sectors.map(s => s.name);

    const subtitle = viewMode === 'market'
        ? 'S&P 500 top stocks • sized by market cap • colored by daily change'
        : viewMode === 'portfolio'
            ? 'Your portfolio holdings • equal weight • colored by daily change'
            : 'Your watchlist • equal weight • colored by daily change';

    return (
        <div className="bg-card rounded-lg p-6 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-4">
                <div>
                    <h3 className="text-lg font-semibold text-main">Market Heat Map</h3>
                    <p className="text-sm text-muted">{subtitle}</p>
                </div>
                <div className="flex items-center gap-2">
                    <select
                        value={viewMode}
                        onChange={(e) => setViewMode(e.target.value as any)}
                        className="text-xs bg-page border border-border text-main rounded-md px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-primary"
                    >
                        <option value="market">S&P 500</option>
                        <option value="portfolio">My Portfolio</option>
                        <option value="watchlist">My Watchlist</option>
                    </select>
                    {viewMode === 'market' && (
                        <select
                            value={selectedSector}
                            onChange={(e) => setSelectedSector(e.target.value)}
                            className="text-xs bg-page border border-border text-main rounded-md px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-primary"
                        >
                            <option value="all">All Sectors</option>
                            {allSectors.map(s => (
                                <option key={s} value={s}>{s}</option>
                            ))}
                        </select>
                    )}
                </div>
            </div>

            {/* Color Legend */}
            <div className="flex items-center justify-center gap-1 mb-4 text-[10px] text-muted">
                <span>-3%</span>
                <div className="flex gap-0.5">
                    {[
                        '#dc2626', '#ef4444', '#f87171', '#fca5a5', '#fecaca',
                        '#bbf7d0', '#86efac', '#4ade80', '#22c55e', '#16a34a'
                    ].map((color, i) => (
                        <div key={i} className="w-5 h-3 rounded-sm" style={{ backgroundColor: color }} />
                    ))}
                </div>
                <span>+3%</span>
            </div>

            <div className="h-[420px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                    <Treemap
                        data={treemapData}
                        dataKey="size"
                        aspectRatio={4 / 3}
                        stroke="none"
                        content={<CustomTreemapContent x={0} y={0} width={0} height={0} name="" changePercent={0} depth={0} index={0} />}
                    >
                        <Tooltip content={<CustomTooltip />} />
                    </Treemap>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export default MarketHeatMap;
