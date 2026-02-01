import React, { useState, useEffect, useMemo } from 'react';
import {
  Book,
  Film,
  Tv,
  Music,
  Gamepad2,
  Calendar,
  Filter,
  Maximize2,
  Minimize2,
  X,
  Star,
  ExternalLink
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import { LifeLogEntry, LifeLogType } from './LifeLogView';
import LoadingSpinner from './UI/LoadingSpinner';

interface TimelineProps {
  className?: string;
}

interface TimelineEntry extends LifeLogEntry {
  lane: number;
  startPosition: number;
  width: number;
}

interface TimelineLane {
  entries: TimelineEntry[];
  height: number;
}

const Timeline: React.FC<TimelineProps> = ({ className = "" }) => {
  const [entries, setEntries] = useState<LifeLogEntry[]>([]);
  const [timelineEntries, setTimelineEntries] = useState<TimelineEntry[]>([]);
  const [lanes, setLanes] = useState<TimelineLane[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedTypes, setSelectedTypes] = useState<Set<LifeLogType>>(new Set(Object.values(LifeLogType)));
  const [showFilters, setShowFilters] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [timeRange, setTimeRange] = useState({ start: new Date(), end: new Date() });
  const [selectedEntry, setSelectedEntry] = useState<LifeLogEntry | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [hoveredEntry, setHoveredEntry] = useState<number | null>(null);
  const [filterTransition, setFilterTransition] = useState(false);
  const { error } = useNotification();

  // Helper function to check if two timeline entries overlap
  const entriesOverlap = (entry1: TimelineEntry, entry2: TimelineEntry, padding: number = 5): boolean => {
    const entry1End = entry1.startPosition + entry1.width;
    const entry2End = entry2.startPosition + entry2.width;

    return !(entry1.startPosition >= entry2End + padding || entry1End + padding <= entry2.startPosition);
  };

  // Helper function to calculate the optimal lane for an entry
  const findOptimalLane = (entry: TimelineEntry, lanes: TimelineLane[]): number => {
    let bestLane = lanes.length; // Default to new lane
    let bestScore = Infinity;

    for (let i = 0; i < lanes.length; i++) {
      const lane = lanes[i];
      const hasOverlap = lane.entries.some(existingEntry =>
        entriesOverlap(entry, existingEntry)
      );

      if (!hasOverlap) {
        // Calculate a score for this lane (prefer lanes with earlier end times)
        const laneEndTime = Math.max(0, ...lane.entries.map(e => e.startPosition + e.width));
        const gapSize = entry.startPosition - laneEndTime;
        const score = i * 100 + Math.max(0, -gapSize); // Prefer lower lanes and smaller gaps

        if (score < bestScore) {
          bestScore = score;
          bestLane = i;
        }
      }
    }

    return bestLane;
  };

  // Helper function to group entries by type for better visual organization
  const groupEntriesByType = (entries: TimelineEntry[]): Map<LifeLogType, TimelineEntry[]> => {
    const groups = new Map<LifeLogType, TimelineEntry[]>();

    Object.values(LifeLogType).forEach(type => {
      groups.set(type, entries.filter(entry => entry.type === type));
    });

    return groups;
  };

  // Icon mapping for Life Log types
  const getIconForType = (type: LifeLogType) => {
    switch (type) {
      case LifeLogType.BOOK:
        return <Book className="h-4 w-4" />;
      case LifeLogType.MOVIE:
        return <Film className="h-4 w-4" />;
      case LifeLogType.SHOW:
        return <Tv className="h-4 w-4" />;
      case LifeLogType.ALBUM:
        return <Music className="h-4 w-4" />;
      case LifeLogType.HOBBY:
        return <Gamepad2 className="h-4 w-4" />;
      default:
        return <Book className="h-4 w-4" />;
    }
  };

  // Color mapping for Life Log types
  const getColorForType = (type: LifeLogType) => {
    switch (type) {
      case LifeLogType.BOOK:
        return {
          bg: 'bg-amber-500',
          border: 'border-amber-600',
          text: 'text-amber-900 dark:text-amber-100',
          light: 'bg-amber-100 dark:bg-amber-900/30'
        };
      case LifeLogType.MOVIE:
        return {
          bg: 'bg-red-500',
          border: 'border-red-600',
          text: 'text-red-900 dark:text-red-100',
          light: 'bg-red-100 dark:bg-red-900/30'
        };
      case LifeLogType.SHOW:
        return {
          bg: 'bg-purple-500',
          border: 'border-purple-600',
          text: 'text-purple-900 dark:text-purple-100',
          light: 'bg-purple-100 dark:bg-purple-900/30'
        };
      case LifeLogType.ALBUM:
        return {
          bg: 'bg-blue-500',
          border: 'border-blue-600',
          text: 'text-blue-900 dark:text-blue-100',
          light: 'bg-blue-100 dark:bg-blue-900/30'
        };
      case LifeLogType.HOBBY:
        return {
          bg: 'bg-green-500',
          border: 'border-green-600',
          text: 'text-green-900 dark:text-green-100',
          light: 'bg-green-100 dark:bg-green-900/30'
        };
      default:
        return {
          bg: 'bg-gray-500',
          border: 'border-gray-600',
          text: 'text-gray-900 dark:text-gray-100',
          light: 'bg-gray-100 dark:bg-gray-800'
        };
    }
  };

  // Load timeline entries
  const loadTimelineEntries = async () => {
    try {
      setIsLoading(true);
      const response = await apiService.getLifeLogTimeline();

      if (response.success) {
        const timelineData = response.data || [];
        setEntries(timelineData);

        // Calculate time range
        if (timelineData.length > 0) {
          const allTimestamps = timelineData
            .filter((entry: LifeLogEntry) => entry.startDate || entry.endDate)
            .flatMap((entry: LifeLogEntry) => {
              const start = entry.startDate ? new Date(entry.startDate).getTime() : new Date(entry.endDate!).getTime();
              const end = entry.endDate ? new Date(entry.endDate).getTime() : start;
              return [start, end];
            });

          if (allTimestamps.length > 0) {
            const minDate = new Date(Math.min(...allTimestamps));
            const maxDate = new Date(Math.max(...allTimestamps));

            // Add some padding to the range (7 days)
            // Add minimal padding to the range (1 day) to avoid edge clipping
            minDate.setDate(minDate.getDate() - 1);
            maxDate.setDate(maxDate.getDate() + 1);

            setTimeRange({ start: minDate, end: maxDate });
          }
        }
      }
    } catch (err: any) {
      error('Failed to load timeline entries', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Calculate timeline positions and lane assignments
  const calculateTimelineLayout = useMemo(() => {
    const filteredEntries = entries.filter(entry =>
      selectedTypes.has(entry.type) && (entry.startDate || entry.endDate)
    );

    if (filteredEntries.length === 0) {
      setTimelineEntries([]);
      setLanes([]);
      return;
    }

    // Use 100 as base for percentage calculation
    const timelineWidth = 100;
    const totalDuration = timeRange.end.getTime() - timeRange.start.getTime();

    // Calculate positions and widths for entries
    const positionedEntries: TimelineEntry[] = filteredEntries.map(entry => {
      // Logic for determining start/end:
      // 1. Both present: Range [Start, End]
      // 2. Only Start: Range [Start, Today] (Ongoing)
      // 3. Only End: Point [End, End] (Single Event like Movie)

      const startDate = entry.startDate
        ? new Date(entry.startDate)
        : new Date(entry.endDate!); // fallback to endDate if no start

      const endDate = entry.endDate
        ? new Date(entry.endDate)
        : new Date(); // fallback to today if no end (ongoing)

      const startPosition = ((startDate.getTime() - timeRange.start.getTime()) / totalDuration) * timelineWidth;
      const endPosition = ((endDate.getTime() - timeRange.start.getTime()) / totalDuration) * timelineWidth;

      // Use a smaller minimum width for visual readability (in %) - reduced from 10% to 1% to prevent distortion
      const minVisualWidth = 1;
      const temporalWidth = endPosition - startPosition;
      const width = Math.max(temporalWidth, minVisualWidth);

      return {
        ...entry,
        lane: 0, // Will be assigned later
        startPosition: Math.max(0, startPosition),
        width: Math.min(width, timelineWidth - startPosition)
      };
    });

    // Sort by start position for optimal lane assignment
    positionedEntries.sort((a, b) => a.startPosition - b.startPosition);

    // Enhanced lane assignment algorithm with type grouping
    const assignedLanes: TimelineLane[] = [];

    // Group entries by type for better visual organization
    const typeGroups = groupEntriesByType(positionedEntries);
    const typeOrder = [LifeLogType.BOOK, LifeLogType.MOVIE, LifeLogType.SHOW, LifeLogType.ALBUM, LifeLogType.HOBBY];

    // Process each type group to maintain visual coherence
    typeOrder.forEach(type => {
      const typeEntries = typeGroups.get(type) || [];

      typeEntries.forEach(entry => {
        const optimalLane = findOptimalLane(entry, assignedLanes);

        // Create new lane if needed
        while (optimalLane >= assignedLanes.length) {
          assignedLanes.push({ entries: [], height: 60 });
        }

        entry.lane = optimalLane;
        assignedLanes[optimalLane].entries.push(entry);
      });
    });

    // Optimize lane heights based on content
    assignedLanes.forEach((lane) => {
      // Adjust lane height based on entry count and content
      const maxEntriesInSamePosition = Math.max(1,
        ...lane.entries.map(entry =>
          lane.entries.filter(other =>
            Math.abs(other.startPosition - entry.startPosition) < 50
          ).length
        )
      );

      lane.height = Math.max(60, Math.min(80, 50 + maxEntriesInSamePosition * 5));
    });

    // Advanced lane compaction algorithm
    const compactLanes = (lanes: TimelineLane[]): TimelineLane[] => {
      const compacted: TimelineLane[] = [];
      const allEntries = lanes.flatMap(lane => lane.entries);

      // Sort all entries by start position for optimal compaction
      allEntries.sort((a, b) => a.startPosition - b.startPosition);

      allEntries.forEach(entry => {
        const optimalLane = findOptimalLane(entry, compacted);

        // Create new lane if needed
        while (optimalLane >= compacted.length) {
          compacted.push({ entries: [], height: 60 });
        }

        entry.lane = optimalLane;
        compacted[optimalLane].entries.push(entry);
      });

      // Optimize lane heights based on density
      compacted.forEach((lane) => {
        const entryDensity = lane.entries.length;
        const maxOverlap = Math.max(1,
          ...lane.entries.map(entry => {
            const overlappingEntries = lane.entries.filter(other =>
              other !== entry && entriesOverlap(entry, other, 0)
            );
            return overlappingEntries.length + 1;
          })
        );

        // Adjust height based on density and overlap
        lane.height = Math.max(50, Math.min(80, 45 + maxOverlap * 8 + entryDensity * 2));
      });

      return compacted.filter(lane => lane.entries.length > 0); // Remove empty lanes
    };

    const finalLanes = compactLanes(assignedLanes);

    setTimelineEntries(positionedEntries);
    setLanes(finalLanes);
  }, [entries, selectedTypes, timeRange]);

  // Toggle type filter with smooth transition
  const toggleTypeFilter = (type: LifeLogType) => {
    setFilterTransition(true);

    setTimeout(() => {
      const newSelectedTypes = new Set(selectedTypes);
      if (newSelectedTypes.has(type)) {
        newSelectedTypes.delete(type);
      } else {
        newSelectedTypes.add(type);
      }
      setSelectedTypes(newSelectedTypes);

      setTimeout(() => setFilterTransition(false), 100);
    }, 150);
  };

  // Handle entry click to show details
  const handleEntryClick = (entry: LifeLogEntry) => {
    setSelectedEntry(entry);
    setIsDetailModalOpen(true);
  };

  // Handle entry hover
  const handleEntryHover = (entryId: number | null) => {
    setHoveredEntry(entryId);
  };

  // Close detail modal
  const closeDetailModal = () => {
    setIsDetailModalOpen(false);
    setSelectedEntry(null);
  };

  // Select all types
  const selectAllTypes = () => {
    setFilterTransition(true);
    setTimeout(() => {
      setSelectedTypes(new Set(Object.values(LifeLogType)));
      setTimeout(() => setFilterTransition(false), 100);
    }, 150);
  };

  // Deselect all types
  const deselectAllTypes = () => {
    setFilterTransition(true);
    setTimeout(() => {
      setSelectedTypes(new Set());
      setTimeout(() => setFilterTransition(false), 100);
    }, 150);
  };

  // Get entry statistics for current filter
  const getFilterStats = () => {
    const totalEntries = entries.length;
    const filteredEntries = entries.filter(entry => selectedTypes.has(entry.type));
    const typeStats = Object.values(LifeLogType).map(type => ({
      type,
      count: entries.filter(entry => entry.type === type).length,
      visible: selectedTypes.has(type)
    }));

    return { totalEntries, filteredEntries: filteredEntries.length, typeStats };
  };

  // Format date for timeline markers
  const formatTimelineDate = (date: Date, type: 'month' | 'week' = 'month') => {
    if (type === 'week') {
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
      });
    }
    return date.toLocaleDateString('en-US', {
      month: 'short',
      year: 'numeric'
    });
  };

  // Generate timeline markers
  const generateTimelineMarkers = () => {
    const markers = [];
    const durationMs = timeRange.end.getTime() - timeRange.start.getTime();
    const durationDays = durationMs / (1000 * 60 * 60 * 24);

    // Determine interval type based on duration
    const isShortDuration = durationDays < 60;

    const current = new Date(timeRange.start);

    if (isShortDuration) {
      // For short duration, show weekly markers (Mondays)
      // Find previous Monday
      const day = current.getDay();
      const diff = current.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
      current.setDate(diff);

      // Ensure time is reset to avoid drift
      current.setHours(0, 0, 0, 0);

      while (current <= timeRange.end) {
        // Only add if within view (with small buffer)
        if (current >= timeRange.start) {
          const position = ((current.getTime() - timeRange.start.getTime()) / durationMs) * 100;

          if (position >= 0 && position <= 100) {
            markers.push({
              date: new Date(current),
              position: position,
              type: 'week' as const
            });
          }
        }

        // Next week
        current.setDate(current.getDate() + 7);
      }
    } else {
      // For longer duration, show monthly markers (1st of month)
      current.setDate(1); // Start of month

      // Ensure time is reset
      current.setHours(0, 0, 0, 0);

      while (current <= timeRange.end) {
        // Only add if within view (or close enough to be meaningful context)
        if (current >= timeRange.start || current.getMonth() === timeRange.start.getMonth()) {
          const position = ((current.getTime() - timeRange.start.getTime()) / durationMs) * 100;

          if (position >= 0 && position <= 100) {
            markers.push({
              date: new Date(current),
              position: position,
              type: 'month' as const
            });
          }
        }

        current.setMonth(current.getMonth() + 1); // Every month
      }
    }

    return markers;
  };

  useEffect(() => {
    loadTimelineEntries();
  }, []);

  useEffect(() => {
    calculateTimelineLayout;
  }, [calculateTimelineLayout]);

  if (isLoading) {
    return (
      <div className={`bg-white rounded-xl shadow-sm p-6 ${className}`}>
        <div className="flex items-center justify-center h-64">
          <LoadingSpinner size="large" />
        </div>
      </div>
    );
  }

  const timelineMarkers = generateTimelineMarkers();

  return (
    <div className={`bg-card rounded-xl shadow-sm p-6 border border-border ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-main">Timeline</h2>
          <p className="text-sm text-muted mt-1">Visual chronology of your activities and media</p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="p-2 text-muted hover:text-primary transition-colors rounded-lg hover:bg-page"
            title="Toggle filters"
          >
            <Filter className="h-5 w-5" />
          </button>
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-2 text-muted hover:text-primary transition-colors rounded-lg hover:bg-page"
            title={isExpanded ? "Collapse timeline" : "Expand timeline"}
          >
            {isExpanded ? <Minimize2 className="h-5 w-5" /> : <Maximize2 className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {/* Enhanced Type Filters */}
      {showFilters && (
        <div className={`mb-6 p-4 bg-page rounded-lg transition-all duration-300 ease-in-out ${filterTransition ? 'opacity-50 scale-95' : 'opacity-100 scale-100'
          }`}>
          {/* Filter Controls */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-medium text-main">Filter by Type</h3>
              <span className="text-xs text-muted">
                ({getFilterStats().filteredEntries} of {getFilterStats().totalEntries} entries)
              </span>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={selectAllTypes}
                className="text-xs text-primary hover:text-primary/80 transition-colors"
              >
                Select All
              </button>
              <span className="text-muted">|</span>
              <button
                onClick={deselectAllTypes}
                className="text-xs text-muted hover:text-main transition-colors"
              >
                Clear All
              </button>
            </div>
          </div>

          {/* Type Filter Buttons */}
          <div className="flex flex-wrap gap-2 mb-4">
            {Object.values(LifeLogType).map(type => {
              const colors = getColorForType(type);
              const isSelected = selectedTypes.has(type);
              const typeCount = entries.filter(entry => entry.type === type).length;

              return (
                <button
                  key={type}
                  onClick={() => toggleTypeFilter(type)}
                  className={`flex items-center space-x-2 px-3 py-2 rounded-lg border-2 transition-all duration-200 hover:scale-105 active:scale-95 ${isSelected
                    ? `${colors.bg} ${colors.border} text-white shadow-md`
                    : `${colors.light} ${colors.border} ${colors.text} hover:${colors.bg} hover:text-white`
                    }`}
                  disabled={filterTransition}
                >
                  {getIconForType(type)}
                  <span className="text-sm font-medium">
                    {type.toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                  </span>
                  <span className={`text-xs px-1.5 py-0.5 rounded-full ${isSelected ? 'bg-white bg-opacity-20' : 'bg-gray-200'
                    }`}>
                    {typeCount}
                  </span>
                </button>
              );
            })}
          </div>

          {/* Filter Statistics */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-2 text-xs">
            {getFilterStats().typeStats.map(({ type, count, visible }) => {
              const colors = getColorForType(type);
              return (
                <div
                  key={type}
                  className={`flex items-center justify-between p-2 rounded ${visible ? colors.light : 'bg-page'
                    } transition-colors duration-200`}
                >
                  <span className={visible ? colors.text : 'text-gray-500'}>
                    {type.toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                  </span>
                  <span className={`font-medium ${visible ? colors.text : 'text-gray-500'}`}>
                    {count}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Timeline */}
      {timelineEntries.length > 0 ? (
        <div className={`relative ${isExpanded ? 'h-96' : 'h-64'} overflow-x-auto overflow-y-hidden`}>
          <div
            className="relative w-full min-w-[800px]"
            style={{
              height: `${lanes.reduce((total, lane, index) =>
                total + (lane.height || 60) + (index < lanes.length - 1 ? 10 : 0), 60
              )}px`
            }}
          >
            {/* Timeline markers */}
            <div className="absolute top-0 left-0 w-full h-8 border-b border-gray-200">
              {timelineMarkers.map((marker, index) => (
                <div
                  key={index}
                  className="absolute top-0 h-full flex flex-col items-center"
                  style={{ left: `${marker.position}%` }}
                >
                  <div className="w-px h-full bg-gray-300"></div>
                  {/* Smart label alignment: Left align first, Right align last, Center others */}
                  <div className={`absolute top-0 text-xs text-muted bg-card px-1 ${marker.position < 5 ? 'left-0' :
                    marker.position > 95 ? 'right-0' :
                      '-translate-x-1/2'
                    }`}>
                    {formatTimelineDate(marker.date, marker.type)}
                  </div>
                </div>
              ))}
            </div>

            {/* Timeline entries with enhanced interactions */}
            <div className={`absolute top-10 left-0 w-full transition-all duration-300 ${filterTransition ? 'opacity-50 scale-98' : 'opacity-100 scale-100'
              }`}>
              {timelineEntries.map((entry, index) => {
                const colors = getColorForType(entry.type);
                const lane = lanes[entry.lane];
                const laneHeight = lane?.height || 60;
                const isHovered = hoveredEntry === entry.id;
                const isVisible = selectedTypes.has(entry.type);
                const isTopLane = entry.lane === 0;
                const isSmallEntry = entry.width < 5;

                return (
                  <div
                    key={entry.id}
                    className={`absolute ${colors.bg} ${colors.border} border-2 rounded-lg p-2 shadow-sm cursor-pointer group transition-all duration-300 ${isVisible
                      ? 'opacity-100 scale-100'
                      : 'opacity-0 scale-95 pointer-events-none'
                      } ${isHovered
                        ? 'shadow-xl scale-110 z-50 ring-2 ring-white ring-opacity-50'
                        : 'hover:shadow-lg hover:scale-105 hover:z-40'
                      }`}
                    style={{
                      left: `${entry.startPosition}%`,
                      top: `${entry.lane * (laneHeight + 10)}px`,
                      width: `${entry.width}%`,
                      height: `${Math.min(laneHeight - 10, 50)}px`,
                      animationDelay: `${index * 30}ms`,
                      transitionDelay: isVisible ? `${index * 20}ms` : '0ms',
                      // Ensure small entries have a minimum clickable area and visual presence
                      minWidth: isSmallEntry ? '40px' : 'auto',
                      overflow: 'visible' // Allow tooltip/text to break out
                    }}
                    onClick={() => handleEntryClick(entry)}
                    onMouseEnter={() => handleEntryHover(entry.id)}
                    onMouseLeave={() => handleEntryHover(null)}
                    title={`${entry.title} (${entry.type})`}
                  >
                    <div className="flex items-center space-x-2 h-full relative">
                      <div className="text-white flex-shrink-0 transition-transform duration-200 group-hover:scale-110">
                        {getIconForType(entry.type)}
                      </div>

                      {/* Text Container - Logic for small entries */}
                      <div className={`flex-1 min-w-0 ${isSmallEntry ? 'absolute left-8 w-48 z-10' : ''}`}>
                        <div className={`text-white text-sm font-medium transition-all duration-200 ${isSmallEntry ? 'drop-shadow-md' : 'truncate'}`}>
                          {entry.title}
                        </div>
                        <div className={`text-white text-xs opacity-90 transition-opacity duration-200 group-hover:opacity-100 ${isSmallEntry ? 'hidden group-hover:block drop-shadow-md' : ''}`}>
                          {entry.startDate ? (
                            <>
                              {new Date(entry.startDate).toLocaleDateString('en-US', {
                                month: 'short',
                                day: 'numeric'
                              })}
                              {entry.endDate && (
                                <span className="ml-1">
                                  - {new Date(entry.endDate).toLocaleDateString('en-US', {
                                    month: 'short',
                                    day: 'numeric'
                                  })}
                                </span>
                              )}
                            </>
                          ) : (
                            // Only end date (e.g. Movie)
                            <span>
                              {new Date(entry.endDate!).toLocaleDateString('en-US', {
                                month: 'short',
                                day: 'numeric'
                              })}
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Enhanced duration indicator - Only for wide entries */}
                      {!isSmallEntry && entry.startDate && entry.endDate && (
                        <div className="text-white text-xs opacity-75 flex-shrink-0 bg-black bg-opacity-20 px-1 rounded transition-all duration-200 group-hover:bg-opacity-30">
                          {Math.ceil((new Date(entry.endDate).getTime() - new Date(entry.startDate).getTime()) / (1000 * 60 * 60 * 24))}d
                        </div>
                      )}
                    </div>

                    {/* Enhanced hover tooltip - Smart Positioning */}
                    <div className={`absolute left-1/2 transform -translate-x-1/2 px-3 py-2 bg-gray-900 text-white text-xs rounded-lg shadow-lg transition-all duration-200 pointer-events-none whitespace-nowrap z-50 ${isHovered
                      ? `opacity-100 ${isTopLane ? 'translate-y-0' : 'translate-y-0'}`
                      : `opacity-0 ${isTopLane ? '-translate-y-1' : 'translate-y-1'}`
                      } ${isTopLane
                        ? 'top-full mt-3'
                        : 'bottom-full mb-3'
                      }`}>
                      <div className="font-medium">{entry.title}</div>
                      <div className="text-gray-300 text-xs">
                        {entry.type} • Lane {entry.lane + 1}
                        {entry.rating && ` • ${entry.rating}/5 ⭐`}
                      </div>

                      {/* Tooltip arrow - Smart direction */}
                      <div className={`absolute left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-transparent ${isTopLane
                        ? 'bottom-full border-b-4 border-b-gray-900' // Arrow points up
                        : 'top-full border-t-4 border-t-gray-900'    // Arrow points down
                        }`}></div>
                    </div>

                    {/* Click indicator */}
                    <div className={`absolute inset-0 bg-white bg-opacity-20 rounded-lg transition-opacity duration-150 ${isHovered ? 'opacity-100' : 'opacity-0'
                      }`}></div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      ) : (
        <div className="text-center py-12">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Calendar className="h-8 w-8 text-gray-400" />
          </div>
          <p className="text-gray-500 mb-2">
            {selectedTypes.size === 0
              ? 'Select entry types to view timeline.'
              : 'No timeline entries available.'
            }
          </p>
          <p className="text-sm text-gray-400">
            Add Life Log entries with dates to see them on the timeline.
          </p>
        </div>
      )}

      {/* Timeline Legend */}
      {timelineEntries.length > 0 && (
        <div className="mt-6 pt-4 border-t border-gray-200">
          <div className="flex items-center justify-between text-sm text-gray-500">
            <div>
              Showing {timelineEntries.length} {timelineEntries.length === 1 ? 'entry' : 'entries'}
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-1">
                <Calendar className="h-4 w-4" />
                <span>
                  {formatTimelineDate(timeRange.start)} - {formatTimelineDate(timeRange.end)}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}


      {/* Detail Modal */}
      {
        isDetailModalOpen && selectedEntry && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto animate-fadeIn">
              <div className={`p-6 ${getColorForType(selectedEntry.type).light} rounded-t-xl border-b border-gray-200 relative`}>
                <button
                  onClick={closeDetailModal}
                  className="absolute top-4 right-4 p-1 rounded-full bg-white bg-opacity-50 hover:bg-opacity-100 transition-colors"
                >
                  <X className="h-5 w-5 text-gray-600" />
                </button>

                <div className="flex items-center space-x-3 mb-2">
                  <div className={`p-2 rounded-lg bg-white bg-opacity-50 ${getColorForType(selectedEntry.type).text}`}>
                    {getIconForType(selectedEntry.type)}
                  </div>
                  <span className={`text-sm font-semibold uppercase tracking-wider ${getColorForType(selectedEntry.type).text}`}>
                    {selectedEntry.type}
                  </span>
                </div>

                <h3 className="text-2xl font-bold text-gray-900 mb-1">{selectedEntry.title}</h3>

                <div className="flex items-center space-x-4 text-sm text-gray-600 mt-2">
                  <div className="flex items-center space-x-1">
                    <Calendar className="h-4 w-4" />
                    <span>
                      {selectedEntry.startDate ? (
                        new Date(selectedEntry.startDate).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })
                      ) : (
                        new Date(selectedEntry.endDate!).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })
                      )}
                    </span>
                  </div>

                  {selectedEntry.rating && (
                    <div className="flex items-center space-x-1">
                      <Star className="h-4 w-4 fill-current text-yellow-500" />
                      <span className="font-medium">{selectedEntry.rating}/5</span>
                    </div>
                  )}
                </div>
              </div>

              <div className="p-6 space-y-4">
                {selectedEntry.keyTakeaway && (
                  <div>
                    <h4 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Key Takeaway</h4>
                    <p className="text-gray-800 bg-gray-50 p-3 rounded-lg border border-gray-100 italic">
                      "{selectedEntry.keyTakeaway}"
                    </p>
                  </div>
                )}

                {selectedEntry.metadata && (
                  <div>
                    <h4 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Details</h4>
                    <div className="bg-gray-50 rounded-lg p-4 text-sm">
                      {(() => {
                        try {
                          const metadata = JSON.parse(selectedEntry.metadata);
                          return (
                            <div className="grid grid-cols-1 gap-2">
                              {Object.entries(metadata).map(([key, value]) => {
                                // Skip null/empty values and internal fields
                                if (!value || key === 'cover_i' || key === 'key' || key === 'color_name') return null;

                                return (
                                  <div key={key} className="flex flex-col sm:flex-row sm:space-x-2">
                                    <span className="font-medium text-gray-700 capitalize min-w-[120px]">
                                      {key.replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').trim()}:
                                    </span>
                                    <span className="text-gray-900 break-words">
                                      {Array.isArray(value) ? value.join(', ') : String(value)}
                                    </span>
                                  </div>
                                );
                              })}
                            </div>
                          );
                        } catch (e) {
                          // Fallback for non-JSON content
                          return <p className="text-gray-700 whitespace-pre-line">{selectedEntry.metadata}</p>;
                        }
                      })()}
                    </div>
                  </div>
                )}

                {selectedEntry.externalId && (
                  <div className="pt-2">
                    <a
                      href={selectedEntry.externalId}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center space-x-2 text-blue-600 hover:text-blue-700 font-medium"
                    >
                      <span>View External Resource</span>
                      <ExternalLink className="h-4 w-4" />
                    </a>
                  </div>
                )}

                {(selectedEntry.type === LifeLogType.HOBBY) && selectedEntry.intensity && (
                  <div>
                    <h4 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Intensity</h4>
                    <div className="flex items-center space-x-1">
                      {[...Array(5)].map((_, i) => (
                        <div
                          key={i}
                          className={`h-2 w-8 rounded-full ${i < selectedEntry.intensity!
                            ? 'bg-green-500'
                            : 'bg-gray-200'
                            }`}
                        />
                      ))}
                      <span className="ml-2 text-sm text-gray-600 font-medium">{selectedEntry.intensity}/5</span>
                    </div>
                  </div>
                )}
              </div>

              <div className="p-4 border-t border-gray-100 bg-gray-50 flex justify-end">
                <button
                  onClick={closeDetailModal}
                  className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 font-medium transition-colors"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
    </div>
  );
};

export default Timeline;