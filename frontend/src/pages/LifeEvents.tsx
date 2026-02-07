import React, { useState } from 'react';
import BearsTracker from '../components/BearsTracker';
import BerkeleyCountdown from '../components/BerkeleyCountdown';
import NFLDraftCountdown from '../components/NFLDraftCountdown';
import FamilyPulse from '../components/FamilyPulse';
import DigitalGardenView from '../components/DigitalGardenView';
import SportsStandings from '../components/SportsStandings';
import BearsSeasonStats from '../components/BearsSeasonStats';
import BearsRoster from '../components/BearsRoster';
import TabNavigation from '../components/UI/TabNavigation';
import {
    LayoutGrid,
    Trophy,
    Timer,
    Users,
    BookOpen
} from 'lucide-react';

const LifeEvents: React.FC = () => {
    const [activeTab, setActiveTab] = useState('overview');

    const tabs = [
        { id: 'overview', label: 'Overview', icon: LayoutGrid },
        { id: 'sports', label: 'Sports', icon: Trophy },
        { id: 'milestones', label: 'Milestones', icon: Timer },
        { id: 'family', label: 'Family', icon: Users },
        { id: 'garden', label: 'Garden', icon: BookOpen },
    ];

    return (
        <div className="w-full p-6 bg-page min-h-screen transition-colors duration-300">
            <div className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-main">Life</h1>
                    <p className="text-muted mt-2">Tracking important events, countdowns, and family updates.</p>
                </div>

                <TabNavigation
                    tabs={tabs}
                    activeTab={activeTab}
                    onTabChange={setActiveTab}
                />
            </div>

            <div className="animate-fade-in">
                {/* Overview Tab */}
                {activeTab === 'overview' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        <div className="space-y-6">
                            <BearsTracker />
                        </div>
                        <div className="space-y-6">
                            <BerkeleyCountdown />
                        </div>
                        <div className="space-y-6">
                            <FamilyPulse />
                        </div>
                        <div className="md:col-span-2 lg:col-span-3">
                            <div className="bg-white dark:bg-slate-800 rounded-xl p-4 shadow-sm border border-gray-100 dark:border-gray-700">
                                <div className="flex items-center space-x-2 mb-3">
                                    <BookOpen className="h-5 w-5 text-blue-500" />
                                    <h3 className="font-bold text-main">Digital Garden Highlights</h3>
                                </div>
                                <p className="text-sm text-muted">Recent thoughts and musings...</p>
                                <div className="mt-4 h-32 flex items-center justify-center bg-gray-50 dark:bg-slate-900 rounded-lg border border-dashed border-gray-300 dark:border-gray-700">
                                    <span className="text-muted text-sm italic">Select "Garden" tab for full view</span>
                                </div>
                            </div>
                        </div>
                    </div>
                )}





                {/* Sports Tab */}
                {activeTab === 'sports' && (
                    <div className="space-y-6">
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            {/* Bears Focused Column */}
                            <div className="space-y-6">
                                <BearsTracker />
                                <BearsSeasonStats />
                                <NFLDraftCountdown />
                            </div>

                            {/* League Focused Column */}
                            <div className="md:col-span-2 space-y-6">
                                <SportsStandings />
                                <BearsRoster />
                            </div>
                        </div>
                    </div>
                )}

                {/* Milestones Tab */}
                {activeTab === 'milestones' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        <BerkeleyCountdown />
                        <NFLDraftCountdown />
                        {/* Placeholder for future countdowns */}
                        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-dashed border-gray-300 dark:border-gray-700 flex flex-col items-center justify-center text-center opacity-70">
                            <Timer className="h-8 w-8 text-gray-400 mb-2" />
                            <p className="font-medium text-main">New Countdown</p>
                            <p className="text-xs text-muted">Add a new event to track</p>
                        </div>
                    </div>
                )}

                {/* Family Tab */}
                {activeTab === 'family' && (
                    <div className="w-full">
                        <FamilyPulse />
                    </div>
                )}

                {/* Garden Tab */}
                {activeTab === 'garden' && (
                    <div className="w-full">
                        <DigitalGardenView />
                    </div>
                )}
            </div>
        </div>
    );
};

export default LifeEvents;
