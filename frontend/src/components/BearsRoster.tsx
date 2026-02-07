import React, { useEffect, useState } from 'react';
import { sportsService } from '../services/apiService';
import { Users, Search } from 'lucide-react';
import LoadingSpinner from './UI/LoadingSpinner';

const BearsRoster: React.FC<{ className?: string }> = ({ className = '' }) => {
    const [roster, setRoster] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [activeFilter, setActiveFilter] = useState('All');
    const displayYear = new Date().getMonth() < 8 ? new Date().getFullYear() - 1 : new Date().getFullYear();

    useEffect(() => {
        const loadRoster = async () => {
            try {
                const response = await sportsService.getBearsRoster(displayYear);
                if (response.data) setRoster(response.data);
            } catch (e) {
                console.error("Failed to load roster", e);
            } finally {
                setLoading(false);
            }
        };
        loadRoster();
    }, []);

    // Categorize positions
    const getGeneralPosition = (pos: string) => {
        const p = pos.toUpperCase();
        if (['QB', 'RB', 'WR', 'TE', 'OL', 'OT', 'G', 'C', 'FB', 'T'].includes(p)) return 'Offense';
        if (['DL', 'DE', 'DT', 'LB', 'ILB', 'OLB', 'CB', 'S', 'FS', 'SS', 'DB', 'NT'].includes(p)) return 'Defense';
        if (['K', 'P', 'LS', 'H'].includes(p)) return 'Special Teams';
        return 'Other';
    };

    const filteredRoster = roster.filter(p => {
        const matchSearch = p.player_name.toLowerCase().includes(search.toLowerCase()) ||
            p.position.toLowerCase().includes(search.toLowerCase());

        if (activeFilter === 'All') return matchSearch;
        return matchSearch && getGeneralPosition(p.position) === activeFilter;
    });

    const filters = ['All', 'Offense', 'Defense', 'Special Teams'];

    if (loading) return <LoadingSpinner />;

    return (
        <div className={`bg-card rounded-xl p-6 shadow-sm border border-border flex flex-col h-[500px] ${className}`}>
            <div className="flex flex-col space-y-4 mb-4">
                <div className="flex items-center justify-between">
                    <h3 className="font-bold text-main flex items-center">
                        <Users className="h-5 w-5 mr-2 text-blue-600" />
                        Roster
                    </h3>
                    <div className="relative">
                        <Search className="h-4 w-4 absolute left-2 top-2 text-muted" />
                        <input
                            type="text"
                            placeholder="Search..."
                            className="pl-8 pr-3 py-1 text-sm bg-page rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 text-main w-40 border border-border"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                    </div>
                </div>

                {/* Filter Tabs */}
                <div className="flex flex-wrap gap-2">
                    {filters.map(filter => (
                        <button
                            key={filter}
                            onClick={() => setActiveFilter(filter)}
                            className={`
                                px-3 py-1 rounded-full text-xs font-medium transition-colors
                                ${activeFilter === filter
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'bg-page text-muted hover:bg-gray-200 dark:hover:bg-gray-700'
                                }
                            `}
                        >
                            {filter}
                        </button>
                    ))}
                </div>
            </div>

            <div className="flex-1 overflow-y-auto pr-1 custom-scrollbar">
                <table className="w-full text-sm text-left">
                    <thead className="bg-page text-muted sticky top-0 z-10">
                        <tr>
                            <th className="px-3 py-2 rounded-tl-lg">#</th>
                            <th className="px-3 py-2">Name</th>
                            <th className="px-3 py-2">Pos</th>
                            <th className="px-3 py-2">Exp</th>
                            <th className="px-3 py-2 rounded-tr-lg">College</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                        {filteredRoster.map((player, idx) => (
                            <tr key={idx} className="hover:bg-page transition-colors group">
                                <td className="px-3 py-2 font-mono text-xs text-muted group-hover:text-main">{player.jersey_number}</td>
                                <td className="px-3 py-2 font-medium text-main">{player.player_name}</td>
                                <td className="px-3 py-2">
                                    <span className="px-1.5 py-0.5 rounded text-xs bg-page font-mono border border-border">
                                        {player.position}
                                    </span>
                                </td>
                                <td className="px-3 py-2 text-muted text-xs">{player.years_exp}</td>
                                <td className="px-3 py-2 text-muted text-xs truncate max-w-[100px]" title={player.college}>{player.college}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {filteredRoster.length === 0 && (
                    <div className="flex flex-col items-center justify-center h-40 text-muted space-y-2">
                        <Users className="h-8 w-8 opacity-20" />
                        <p>No players found</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default BearsRoster;
