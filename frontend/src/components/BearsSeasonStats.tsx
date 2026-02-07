import React, { useEffect, useState } from 'react';
import { sportsService } from '../services/apiService';
import { TrendingUp, TrendingDown, Target, Shield, Skull } from 'lucide-react';
import LoadingSpinner from './UI/LoadingSpinner';

const BearsSeasonStats: React.FC<{ className?: string }> = ({ className = '' }) => {
    const [stats, setStats] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const displayYear = new Date().getMonth() < 8 ? new Date().getFullYear() - 1 : new Date().getFullYear();

    useEffect(() => {
        const loadStats = async () => {
            try {
                const response = await sportsService.getBearsSummary(displayYear);
                setStats(response.data);
            } catch (e) {
                console.error("Failed to load Bears stats", e);
            } finally {
                setLoading(false);
            }
        };
        loadStats();
    }, []);

    if (loading) return <LoadingSpinner />;
    if (!stats) return null; // Graceful fallback

    return (
        <div className={`bg-gradient-to-br from-blue-900 to-orange-700 rounded-xl p-5 shadow-sm text-white ${className}`}>
            <h3 className="font-bold text-lg mb-4 flex items-center">
                <Shield className="h-5 w-5 mr-2" />
                {displayYear} Season Specs
            </h3>

            <div className="grid grid-cols-2 gap-4">
                <div className="bg-black bg-opacity-20 p-3 rounded-lg text-center">
                    <p className="text-2xl font-bold">{stats.wins}-{stats.losses}</p>
                    <p className="text-xs text-orange-200 uppercase tracking-wider">Record</p>
                </div>

                <div className="bg-black bg-opacity-20 p-3 rounded-lg text-center">
                    <p className="text-2xl font-bold">{stats.points_for}</p>
                    <p className="text-xs text-orange-200 uppercase tracking-wider">Points For</p>
                </div>

                <div className="bg-black bg-opacity-20 p-3 rounded-lg text-center">
                    <p className="text-2xl font-bold">{stats.points_against}</p>
                    <p className="text-xs text-orange-200 uppercase tracking-wider">Points Against</p>
                </div>

                <div className="bg-black bg-opacity-20 p-3 rounded-lg text-center">
                    <p className="text-2xl font-bold">{(stats.win_pct * 100).toFixed(1)}%</p>
                    <p className="text-xs text-orange-200 uppercase tracking-wider">Win %</p>
                </div>
            </div>
        </div>
    );
};

export default BearsSeasonStats;
