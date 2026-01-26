import React from 'react';
import BearsTracker from '../components/BearsTracker';
import BerkeleyCountdown from '../components/BerkeleyCountdown';
import FamilyPulse from '../components/FamilyPulse';

const LifeEvents: React.FC = () => {
    return (
        <div className="max-w-7xl mx-auto p-6 bg-gray-50 min-h-screen">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900">Life Events</h1>
                <p className="text-gray-600 mt-2">Tracking important events, countdowns, and family updates.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* Bears Tracker */}
                <div className="space-y-6">
                    <BearsTracker />
                </div>

                {/* Berkeley Countdown */}
                <div className="space-y-6">
                    <BerkeleyCountdown />
                </div>

                {/* Family Pulse */}
                <div className="space-y-6">
                    <FamilyPulse />
                </div>
            </div>
        </div>
    );
};

export default LifeEvents;
