import React, { useState, useEffect } from 'react';
import {
  Shield,
  Calendar,
  Clock,
  MapPin,
  TrendingUp,
  TrendingDown,
  Wifi,
  WifiOff,
  RefreshCw
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from './UI/LoadingSpinner';

interface GameInfo {
  opponent: string;
  date: string;
  time: string;
  location: string;
  isHome: boolean;
  week?: number;
  season?: string;
}

interface GameResult {
  opponent: string;
  date: string;
  bearsScore: number;
  opponentScore: number;
  isWin: boolean;
  isHome: boolean;
}

interface BearsData {
  nextGame?: GameInfo;
  lastGame?: GameResult;
  record?: {
    wins: number;
    losses: number;
    ties: number;
  };
  isApiAvailable: boolean;
  lastUpdated: string;
  isCachedData: boolean;
}

interface BearsTrackerProps {
  className?: string;
}

const BearsTracker: React.FC<BearsTrackerProps> = ({ className = "" }) => {
  const [bearsData, setBearsData] = useState<BearsData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const { error } = useNotification();

  // Load Bears data
  const loadBearsData = async (isRefresh = false) => {
    try {
      if (isRefresh) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }

      const response = await apiService.getBearsTracker();

      if (response.success) {
        setBearsData(response.data);
      }
    } catch (err: any) {
      error('Failed to load Bears data', err.message);

      // Set fallback data when API fails
      setBearsData({
        isApiAvailable: false,
        lastUpdated: new Date().toISOString(),
        isCachedData: true
      });
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  // Format date for display
  const formatGameDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric'
    });
  };

  // Format time for display
  const formatGameTime = (timeString: string) => {
    try {
      // Handle AM/PM logic manually if implicit parsing fails
      if (timeString.toLowerCase().includes('am') || timeString.toLowerCase().includes('pm')) {
        return timeString;
      }

      const time = new Date(`2000-01-01T${timeString}`);
      if (isNaN(time.getTime())) {
        return timeString;
      }
      return time.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
    } catch {
      return timeString;
    }
  };

  // Get days until next game
  const getDaysUntilGame = (dateString: string) => {
    const gameDate = new Date(dateString);
    const today = new Date();
    const diffTime = gameDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Tomorrow';
    if (diffDays > 0) return `${diffDays} days`;
    return 'Past';
  };

  // Get record display
  const getRecordDisplay = (record?: { wins: number; losses: number; ties: number }) => {
    if (!record) return 'N/A';
    if (record.ties > 0) {
      return `${record.wins}-${record.losses}-${record.ties}`;
    }
    return `${record.wins}-${record.losses}`;
  };

  // Get win percentage
  const getWinPercentage = (record?: { wins: number; losses: number; ties: number }) => {
    if (!record || (record.wins + record.losses + record.ties) === 0) return 0;
    const totalGames = record.wins + record.losses + record.ties;
    const winPercentage = ((record.wins + (record.ties * 0.5)) / totalGames) * 100;
    return Math.round(winPercentage);
  };

  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<BearsData | null>(null);

  useEffect(() => {
    loadBearsData();
  }, []);

  // Initialize edit form when entering edit mode
  useEffect(() => {
    if (isEditing && bearsData) {
      setEditForm(JSON.parse(JSON.stringify(bearsData))); // Deep copy
    }
  }, [isEditing, bearsData]);

  const handleSave = async () => {
    if (!editForm) return;

    try {
      setIsRefreshing(true);
      // Auto-calculate logic if needed, e.g. outcome
      const response = await apiService.updateBearsTracker(editForm);
      if (response.success) {
        setBearsData(response.data);
        setIsEditing(false);
        // Force refresh to ensure all clients get update
        loadBearsData(true);
      }
    } catch (err: any) {
      error('Failed to update Bears data', err.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  const updateNestedField = (path: string, value: any) => {
    if (!editForm) return;

    setEditForm(prev => {
      if (!prev) return null;
      const newState = { ...prev };

      const parts = path.split('.');
      let current: any = newState;

      for (let i = 0; i < parts.length - 1; i++) {
        if (!current[parts[i]]) current[parts[i]] = {};
        current = current[parts[i]];
      }

      const lastPart = parts[parts.length - 1];
      // Handle numeric conversions
      if (typeof value === 'string' && !isNaN(Number(value)) && value.trim() !== '' &&
        (lastPart.toLowerCase().includes('score') || lastPart === 'wins' || lastPart === 'losses' || lastPart === 'ties')) {
        current[lastPart] = Number(value);
      } else {
        current[lastPart] = value;
      }

      return newState;
    });
  };

  if (isLoading) {
    return (
      <div className={`bg-gradient-to-br from-blue-900 to-orange-600 rounded-xl shadow-sm p-4 text-white ${className}`}>
        <div className="flex items-center justify-center h-24">
          <LoadingSpinner size="medium" />
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-gradient-to-br from-blue-900 to-orange-600 rounded-xl shadow-sm p-4 text-white relative overflow-hidden ${className}`}>
      {/* Bears Logo Background */}
      <div className="absolute top-2 right-2 opacity-10 pointer-events-none">
        <Shield className="h-16 w-16" />
      </div>

      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Shield className="h-6 w-6 text-orange-400" />
          <h3 className="text-lg font-bold">Chicago Bears</h3>
        </div>

        <div className="flex items-center space-x-2">
          {/* Edit Button */}
          <button
            onClick={() => setIsEditing(!isEditing)}
            className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors"
            title="Edit Data"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
          </button>

          {/* API Status Indicator */}
          <div className="flex items-center space-x-1">
            {bearsData?.isApiAvailable ? (
              <Wifi className="h-4 w-4 text-green-400" />
            ) : (
              <WifiOff className="h-4 w-4 text-red-400" />
            )}
            {bearsData?.isCachedData && (
              <span className="text-xs text-orange-300">Cached</span>
            )}
          </div>

          {/* Refresh Button */}
          <button
            onClick={() => loadBearsData(true)}
            disabled={isRefreshing}
            className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors disabled:opacity-50"
            title="Refresh data"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {isEditing && editForm ? (
        /* Edit Mode */
        <div className="bg-black bg-opacity-30 p-3 rounded-lg space-y-4 text-sm animate-fade-in max-h-96 overflow-y-auto">

          {/* Record Section */}
          <div className="space-y-2">
            <div className="font-bold text-orange-300 border-b border-white border-opacity-20 pb-1">Record</div>
            <div className="grid grid-cols-3 gap-2">
              <div>
                <label className="block text-xs opacity-70">Wins</label>
                <input
                  type="number"
                  value={editForm.record?.wins || 0}
                  onChange={(e) => updateNestedField('record.wins', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Losses</label>
                <input
                  type="number"
                  value={editForm.record?.losses || 0}
                  onChange={(e) => updateNestedField('record.losses', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Ties</label>
                <input
                  type="number"
                  value={editForm.record?.ties || 0}
                  onChange={(e) => updateNestedField('record.ties', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
            </div>
          </div>

          {/* Next Game Section */}
          <div className="space-y-2">
            <div className="font-bold text-orange-300 border-b border-white border-opacity-20 pb-1">Next Game</div>
            <div className="grid grid-cols-2 gap-2">
              <div className="col-span-2">
                <label className="block text-xs opacity-70">Opponent</label>
                <input
                  type="text"
                  value={editForm.nextGame?.opponent || ''}
                  onChange={(e) => updateNestedField('nextGame.opponent', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Date</label>
                <input
                  type="datetime-local"
                  value={editForm.nextGame?.date ? new Date(editForm.nextGame.date).toISOString().slice(0, 16) : ''}
                  onChange={(e) => updateNestedField('nextGame.date', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1 text-xs"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Home Game?</label>
                <input
                  type="checkbox"
                  checked={editForm.nextGame?.isHome || false}
                  onChange={(e) => updateNestedField('nextGame.isHome', e.target.checked)}
                  className="mt-2"
                />
                <span className="ml-2 text-xs">Yes</span>
              </div>
            </div>
          </div>

          {/* Last Game Section */}
          <div className="space-y-2">
            <div className="font-bold text-orange-300 border-b border-white border-opacity-20 pb-1">Last Game</div>
            <div className="grid grid-cols-2 gap-2">
              <div className="col-span-2">
                <label className="block text-xs opacity-70">Opponent</label>
                <input
                  type="text"
                  value={editForm.lastGame?.opponent || ''}
                  onChange={(e) => updateNestedField('lastGame.opponent', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Bears Score</label>
                <input
                  type="number"
                  value={editForm.lastGame?.bearsScore || 0}
                  onChange={(e) => updateNestedField('lastGame.bearsScore', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Opp Score</label>
                <input
                  type="number"
                  value={editForm.lastGame?.opponentScore || 0}
                  onChange={(e) => updateNestedField('lastGame.opponentScore', e.target.value)}
                  className="w-full bg-white bg-opacity-20 rounded px-2 py-1"
                />
              </div>
              <div>
                <label className="block text-xs opacity-70">Win?</label>
                <input
                  type="checkbox"
                  checked={editForm.lastGame?.isWin || false}
                  onChange={(e) => updateNestedField('lastGame.isWin', e.target.checked)}
                  className="mt-2"
                />
                <span className="ml-2 text-xs">Yes</span>
              </div>
              <div>
                <label className="block text-xs opacity-70">Home Game?</label>
                <input
                  type="checkbox"
                  checked={editForm.lastGame?.isHome || false}
                  onChange={(e) => updateNestedField('lastGame.isHome', e.target.checked)}
                  className="mt-2"
                />
                <span className="ml-2 text-xs">Yes</span>
              </div>
            </div>
          </div>

          <div className="pt-2 flex justify-end space-x-2">
            <button
              onClick={() => setIsEditing(false)}
              className="px-3 py-1 bg-white bg-opacity-10 hover:bg-opacity-20 rounded text-xs transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-3 py-1 bg-orange-500 hover:bg-orange-600 rounded text-xs font-bold transition-colors"
            >
              Save Changes
            </button>
          </div>
        </div>
      ) : (
        /* View Mode */
        bearsData?.isApiAvailable && bearsData.nextGame ? (
          <div className="space-y-4">
            {/* Next Game */}
            <div className="bg-white bg-opacity-10 rounded-lg p-3">
              <div className="flex items-center space-x-2 mb-2">
                <Calendar className="h-4 w-4 text-orange-400" />
                <span className="text-sm font-medium">Next Game</span>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold">
                    {bearsData.nextGame.isHome ? 'vs' : '@'} {bearsData.nextGame.opponent}
                  </span>
                  <span className="text-sm text-orange-300">
                    {getDaysUntilGame(bearsData.nextGame.date)}
                  </span>
                </div>

                <div className="flex items-center space-x-4 text-sm">
                  <div className="flex items-center space-x-1">
                    <Calendar className="h-3 w-3" />
                    <span>{formatGameDate(bearsData.nextGame.date)}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <Clock className="h-3 w-3" />
                    <span>{formatGameTime(bearsData.nextGame.time)}</span>
                  </div>
                </div>

                {bearsData.nextGame.location && (
                  <div className="flex items-center space-x-1 text-sm">
                    <MapPin className="h-3 w-3" />
                    <span>{bearsData.nextGame.location}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Last Game Result */}
            {bearsData.lastGame && (
              <div className="bg-white bg-opacity-10 rounded-lg p-3">
                <div className="flex items-center space-x-2 mb-2">
                  {bearsData.lastGame.isWin ? (
                    <TrendingUp className="h-4 w-4 text-green-400" />
                  ) : (
                    <TrendingDown className="h-4 w-4 text-red-400" />
                  )}
                  <span className="text-sm font-medium">Last Game</span>
                </div>

                <div className="space-y-1">
                  <div className="flex items-center justify-between">
                    <span>
                      {bearsData.lastGame.isHome ? 'vs' : '@'} {bearsData.lastGame.opponent}
                    </span>
                    <span className={`font-bold ${bearsData.lastGame.isWin ? 'text-green-400' : 'text-red-400'}`}>
                      {bearsData.lastGame.isWin ? 'W' : 'L'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span>Score</span>
                    <span>
                      {bearsData.lastGame.bearsScore} - {bearsData.lastGame.opponentScore}
                    </span>
                  </div>
                  <div className="text-xs text-gray-300">
                    {formatGameDate(bearsData.lastGame.date)}
                  </div>
                </div>
              </div>
            )}

            {/* Season Record */}
            {bearsData.record && (
              <div className="flex items-center justify-between text-sm">
                <span>Season Record</span>
                <div className="flex items-center space-x-2">
                  <span className="font-semibold">{getRecordDisplay(bearsData.record)}</span>
                  <span className="text-orange-300">({getWinPercentage(bearsData.record)}%)</span>
                </div>
              </div>
            )}
          </div>
        ) : (
          /* API Unavailable State */
          <div className="text-center py-6">
            <WifiOff className="h-8 w-8 mx-auto mb-2 text-red-400" />
            <p className="text-sm mb-1">NFL API Unavailable</p>
            <p className="text-xs text-orange-300">
              {bearsData?.isCachedData
                ? 'Showing cached data from previous update'
                : 'Unable to fetch current game information'
              }
            </p>

            {/* Fallback Information */}
            <div className="mt-4 p-3 bg-white bg-opacity-10 rounded-lg">
              <div className="flex items-center justify-center space-x-2 mb-2">
                <Shield className="h-4 w-4 text-orange-400" />
                <span className="text-sm font-medium">Chicago Bears</span>
              </div>
              <p className="text-xs text-gray-300">
                Check back later for game updates
              </p>
            </div>
          </div>
        ))}

      {/* Last Updated */}
      {!isEditing && bearsData?.lastUpdated && (
        <div className="mt-4 pt-3 border-t border-white border-opacity-20">
          <p className="text-xs text-orange-300 text-center">
            Updated {new Date(bearsData.lastUpdated).toLocaleString()}
          </p>
        </div>
      )}
    </div>
  );
};

export default BearsTracker;