import React, { useEffect, useState } from 'react';
import { sportsService } from '../services/apiService';
import { Trophy, RefreshCw, AlertTriangle } from 'lucide-react';
import LoadingSpinner from './UI/LoadingSpinner';

interface TeamRecord {
    team_name: string;
    wins: number;
    losses: number;
    ties: number;
    win_pct: number;
    conference: string;
    division: string;
}

const SportsStandings: React.FC<{ className?: string }> = ({ className = '' }) => {
    const [standings, setStandings] = useState<TeamRecord[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const currentYear = new Date().getFullYear();
    // Adjust for NFL season start (if early in year, show previous season)
    const displayYear = new Date().getMonth() < 8 ? currentYear - 1 : currentYear;

    const fetchStandings = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await sportsService.getStandings(displayYear);
            // Ensure data is array
            const data = response.data || [];
            // Sort by Win % desc
            const sorted = [...data].sort((a, b) => b.win_pct - a.win_pct);
            setStandings(sorted);
        } catch (err) {
            console.error(err);
            setError('Failed to load standings. Ensure Sports Service is running.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStandings();
    }, []);

    const [activeConference, setActiveConference] = useState('NFC');
    const [activeDivision, setActiveDivision] = useState('All');

    // ... (fetch logic remains same)

    if (loading) return <LoadingSpinner />;

    if (error) {
        // ... (error handling remains same)
        return (
            <div className={`bg-card rounded-xl p-6 shadow-sm border border-red-200 dark:border-red-900 ${className}`}>
                <div className="flex flex-col items-center justify-center text-center text-red-500">
                    <AlertTriangle className="h-8 w-8 mb-2" />
                    <p>{error}</p>
                    <button onClick={fetchStandings} className="mt-4 px-4 py-2 bg-red-100 dark:bg-red-900 rounded-lg text-sm font-medium">Retry</button>
                </div>
            </div>
        );
    }

    const divisions = ['North', 'South', 'East', 'West'];

    const DivisionTable = ({ divisionName, teams }: { divisionName: string, teams: TeamRecord[] }) => (
        <div className="mb-6 last:mb-0">
            <h4 className="font-bold text-main mb-3 border-b border-border pb-2 flex items-center text-sm uppercase tracking-wide opacity-80">
                {divisionName}
            </h4>
            <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                    <thead className="bg-page text-muted">
                        <tr>
                            <th className="px-3 py-2 rounded-l-lg">Team</th>
                            <th className="px-3 py-2 text-center">W</th>
                            <th className="px-3 py-2 text-center">L</th>
                            <th className="px-3 py-2 text-center">T</th>
                            <th className="px-3 py-2 text-right rounded-r-lg">Pct</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                        {teams.map((team) => (
                            <tr key={team.team_name} className="hover:bg-page transition-colors">
                                <td className="px-3 py-2 font-medium text-main">{team.team_name}</td>
                                <td className="px-3 py-2 text-center text-main">{team.wins}</td>
                                <td className="px-3 py-2 text-center text-main">{team.losses}</td>
                                <td className="px-3 py-2 text-center text-muted">{team.ties}</td>
                                <td className="px-3 py-2 text-right font-mono text-main">{team.win_pct?.toFixed(3)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );

    return (
        <div className={`bg-card rounded-xl p-6 shadow-sm border border-border ${className}`}>
            <div className="flex flex-col space-y-4 mb-6">
                <div className="flex items-center justify-between">
                    <h3 className="text-lg font-bold text-main">NFL Standings ({displayYear})</h3>
                    <button onClick={fetchStandings} className="p-1 hover:bg-page rounded-full">
                        <RefreshCw className="h-4 w-4 text-muted" />
                    </button>
                </div>

                {/* Conference Selector */}
                <div className="flex p-1 bg-page rounded-lg border border-border self-start">
                    {['NFC', 'AFC'].map(conf => (
                        <button
                            key={conf}
                            onClick={() => setActiveConference(conf)}
                            className={`px-6 py-1.5 rounded-md text-sm font-bold transition-all ${activeConference === conf
                                    ? 'bg-white dark:bg-slate-700 text-blue-600 shadow-sm'
                                    : 'text-muted hover:text-main'
                                }`}
                        >
                            {conf}
                        </button>
                    ))}
                </div>

                {/* Division Selector */}
                <div className="flex flex-wrap gap-2 border-b border-border pb-1">
                    <button
                        onClick={() => setActiveDivision('All')}
                        className={`px-3 py-1 rounded-full text-xs font-medium transition-colors border ${activeDivision === 'All'
                                ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 text-blue-600'
                                : 'border-transparent text-muted hover:bg-page'
                            }`}
                    >
                        All Divisions
                    </button>
                    {divisions.map(div => (
                        <button
                            key={div}
                            onClick={() => setActiveDivision(div)}
                            className={`px-3 py-1 rounded-full text-xs font-medium transition-colors border ${activeDivision === div
                                    ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 text-blue-600'
                                    : 'border-transparent text-muted hover:bg-page'
                                }`}
                        >
                            {div}
                        </button>
                    ))}
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
                {(activeDivision === 'All' ? divisions : [activeDivision]).map(div => {
                    const divisionName = `${activeConference} ${div}`;
                    const divisionTeams = standings.filter(t => t.division === divisionName || (t.conference === activeConference && t.division.includes(div)));

                    if (divisionTeams.length === 0) return null;

                    return (
                        <DivisionTable key={div} divisionName={divisionName} teams={divisionTeams} />
                    );
                })}
            </div>

            {standings.length === 0 && !loading && (
                <div className="text-center py-10 text-muted">
                    No standings data available.
                </div>
            )}
        </div>
    );
};

export default SportsStandings;
