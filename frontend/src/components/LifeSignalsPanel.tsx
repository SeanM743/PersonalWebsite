import React from 'react';
import BearsTracker from './BearsTracker';
import BerkeleyCountdown from './BerkeleyCountdown';
import FamilyPulse from './FamilyPulse';

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