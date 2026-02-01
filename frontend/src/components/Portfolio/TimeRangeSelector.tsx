import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, MoreHorizontal } from 'lucide-react';

interface TimeRangeSelectorProps {
    selectedPeriod: string;
    onSelectPeriod: (period: string) => void;
    className?: string; // Allow custom classes
}

const TimeRangeSelector: React.FC<TimeRangeSelectorProps> = ({
    selectedPeriod,
    onSelectPeriod,
    className
}) => {
    const [isMoreOpen, setIsMoreOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    // Primary visible ranges
    const primaryRanges = ['1D', '3D', '5D', '1M'];

    // Secondary hidden ranges
    const secondaryRanges = ['3M', '6M', 'YTD', '1Y', '3Y', '5Y', 'ALL'];

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsMoreOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    const handleSelect = (period: string) => {
        onSelectPeriod(period);
        setIsMoreOpen(false); // Close dropdown on selection
    };

    // Check if the currently selected period is one of the secondary ones
    const isSecondarySelected = secondaryRanges.includes(selectedPeriod);

    return (
        <div className={`flex items-center space-x-1 bg-page rounded-lg p-1 border border-border relative ${className || ''}`}>
            {/* Primary Ranges */}
            {primaryRanges.map((period) => (
                <button
                    key={period}
                    onClick={() => handleSelect(period)}
                    className={`px-3 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap ${selectedPeriod === period
                            ? 'bg-primary text-white shadow-sm'
                            : 'text-muted hover:text-main hover:bg-muted/10'
                        }`}
                >
                    {period}
                </button>
            ))}

            <div className="h-4 w-px bg-border mx-1" />

            {/* Dropdown Trigger */}
            <div className="relative" ref={dropdownRef}>
                <button
                    onClick={() => setIsMoreOpen(!isMoreOpen)}
                    className={`px-2 py-1 flex items-center text-xs font-medium rounded-md transition-colors ${isSecondarySelected || isMoreOpen
                            ? 'bg-muted/20 text-main'
                            : 'text-muted hover:text-main hover:bg-muted/10'
                        }`}
                >
                    {isSecondarySelected ? (
                        // If a secondary option is selected, show it
                        <span className="text-primary font-bold">{selectedPeriod}</span>
                    ) : (
                        // Otherwise show "More" or Icon
                        <span className="flex items-center">
                            More <ChevronDown className={`ml-1 h-3 w-3 transition-transform ${isMoreOpen ? 'rotate-180' : ''}`} />
                        </span>
                    )}
                </button>

                {/* Dropdown Menu */}
                {isMoreOpen && (
                    <div className="absolute right-0 top-full mt-1 w-32 bg-card border border-border rounded-md shadow-lg z-50 py-1 animate-in fade-in zoom-in-95 duration-100">
                        <div className="grid grid-cols-1 gap-0.5">
                            {secondaryRanges.map((period) => (
                                <button
                                    key={period}
                                    onClick={() => handleSelect(period)}
                                    className={`px-4 py-2 text-left text-xs hover:bg-page transition-colors flex justify-between items-center ${selectedPeriod === period ? 'text-primary font-semibold' : 'text-main'
                                        }`}
                                >
                                    {period}
                                    {selectedPeriod === period && <span className="h-1.5 w-1.5 rounded-full bg-primary" />}
                                </button>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default TimeRangeSelector;
