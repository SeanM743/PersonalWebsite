import React from 'react';
import BearsTracker from '../Sports/BearsTracker';
import BerkeleyCountdown from '../Dashboard/BerkeleyCountdown';
import FamilyPulse from '../Dashboard/FamilyPulse';

interface LifeSignalsPanelProps {
  className?: string;
}

const LifeSignalsPanel: React.FC<LifeSignalsPanelProps> = ({ className = "" }) => {
  return (
    <div className={`space-y-4 ${className}`}>
      {/* Bears Tracker */}
      <BearsTracker />

      {/* Berkeley Countdown */}
      <BerkeleyCountdown />

      {/* Family Pulse */}
      <FamilyPulse />
    </div>
  );
};

export default LifeSignalsPanel;