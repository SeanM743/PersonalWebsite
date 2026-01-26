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
      const time = new Date(`2000-01-01T${timeString}`);
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

  useEffect(() => {
    loadBearsData();
  }, []);

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
      <div className="absolute top-2 right-2 opacity-10">
        <Shield className="h-16 w-16" />
      </div>

      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Shield className="h-6 w-6 text-orange-400" />
          <h3 className="text-lg font-bold">Chicago Bears</h3>
        </div>
        
        <div className="flex items-center space-x-2">
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

      {/* Content */}
      {bearsData?.isApiAvailable && bearsData.nextGame ? (
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
      )}

      {/* Last Updated */}
      {bearsData?.lastUpdated && (
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