import React, { useState, useEffect } from 'react';
import {
  GraduationCap,
  Calendar,
  Clock,
  Star,
  Sparkles,
  RefreshCw
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from './UI/LoadingSpinner';

interface CountdownData {
  targetDate: string;
  daysRemaining: number;
  hasReached: boolean;
  description: string;
  milestone: string;
}

interface BerkeleyCountdownProps {
  className?: string;
}

const BerkeleyCountdown: React.FC<BerkeleyCountdownProps> = ({ className = "" }) => {
  const [countdownData, setCountdownData] = useState<CountdownData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());

  /* Edit Mode State */
  const [isEditing, setIsEditing] = useState(false);
  const [editDate, setEditDate] = useState('');

  const { error } = useNotification();

  // Initialize edit date when data loads
  useEffect(() => {
    if (countdownData) {
      setEditDate(countdownData.targetDate.split('T')[0]); // YYYY-MM-DD
    }
  }, [countdownData]);

  // Load countdown data
  const loadCountdownData = async (isRefresh = false) => {
    try {
      if (isRefresh) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }

      const response = await apiService.getBerkeleyCountdown();

      if (response.success) {
        setCountdownData(response.data);
      }
    } catch (err: any) {
      error('Failed to load Berkeley countdown', err.message);

      // Set fallback data
      const targetDate = new Date('2026-06-01'); // Summer 2026 fallback
      const now = new Date();
      const diffTime = targetDate.getTime() - now.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

      setCountdownData({
        targetDate: targetDate.toISOString(),
        daysRemaining: Math.max(0, diffDays),
        hasReached: diffDays <= 0,
        description: 'Berkeley Summer 2026',
        milestone: 'Summer 2026'
      });
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  // Update current time every minute
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000); // Update every minute

    return () => clearInterval(timer);
  }, []);

  // Calculate time breakdown
  const getTimeBreakdown = (days: number) => {
    const years = Math.floor(days / 365);
    const remainingDays = days % 365;
    const months = Math.floor(remainingDays / 30);
    const finalDays = remainingDays % 30;

    return { years, months, days: finalDays };
  };

  // Format target date
  const formatTargetDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  // Get progress percentage (assuming 4-year program starting from 2022)
  const getProgressPercentage = (daysRemaining: number) => {
    const totalDays = 4 * 365; // 4 years
    const daysPassed = totalDays - daysRemaining;
    return Math.max(0, Math.min(100, (daysPassed / totalDays) * 100));
  };

  // Get motivational message based on days remaining
  const getMotivationalMessage = (days: number) => {
    if (days <= 0) {
      return "🎉 Congratulations! The moment has arrived!";
    } else if (days <= 30) {
      return "🔥 The final stretch! Almost there!";
    } else if (days <= 90) {
      return "💪 Getting close! Keep pushing forward!";
    } else if (days <= 365) {
      return "🎯 Less than a year to go! Stay focused!";
    } else if (days <= 730) {
      return "📚 Making great progress! Keep it up!";
    } else {
      return "🌟 The journey continues! Every day counts!";
    }
  };

  useEffect(() => {
    loadCountdownData();
  }, []);

  if (isLoading) {
    return (
      <div className={`bg-gradient-to-br from-blue-600 to-purple-700 rounded-xl shadow-sm p-4 text-white ${className}`}>
        <div className="flex items-center justify-center h-32">
          <LoadingSpinner size="medium" />
        </div>
      </div>
    );
  }

  if (!countdownData) {
    return (
      <div className={`bg-gradient-to-br from-blue-600 to-purple-700 rounded-xl shadow-sm p-4 text-white ${className}`}>
        <div className="text-center py-8">
          <GraduationCap className="h-8 w-8 mx-auto mb-2 text-blue-300" />
          <p className="text-sm">Unable to load countdown data</p>
        </div>
      </div>
    );
  }

  const timeBreakdown = getTimeBreakdown(countdownData.daysRemaining);
  const progressPercentage = getProgressPercentage(countdownData.daysRemaining);

  const handleSaveDate = async () => {
    if (!editDate) return;

    try {
      setIsRefreshing(true);
      const response = await apiService.updateBerkeleyCountdown(editDate);
      if (response.success) {
        setCountdownData(response.data);
        setIsEditing(false);
      }
    } catch (err: any) {
      error('Failed to update date', err.message);
    } finally {
      setIsRefreshing(false);
    }
  };

  return (
    <div className={`bg-gradient-to-br from-blue-600 to-purple-700 rounded-xl shadow-sm p-4 text-white relative overflow-hidden ${className}`}>
      {/* Background Elements */}
      <div className="absolute top-2 right-2 opacity-10 pointer-events-none">
        <GraduationCap className="h-16 w-16" />
      </div>

      {/* Sparkles Animation */}
      {countdownData.hasReached && (
        <div className="absolute inset-0 pointer-events-none">
          <Sparkles className="absolute top-4 left-4 h-4 w-4 text-yellow-300 animate-pulse" />
          <Star className="absolute top-8 right-8 h-3 w-3 text-yellow-400 animate-bounce" />
          <Sparkles className="absolute bottom-6 left-8 h-3 w-3 text-yellow-300 animate-pulse delay-500" />
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <GraduationCap className="h-6 w-6 text-yellow-400" />
          <h3 className="text-lg font-bold">Berkeley Countdown</h3>
        </div>

        <div className="flex items-center space-x-1">
          {/* Edit Button */}
          <button
            onClick={() => setIsEditing(!isEditing)}
            className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors"
            title="Edit Date"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path></svg>
          </button>

          <button
            onClick={() => loadCountdownData(true)}
            disabled={isRefreshing}
            className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors disabled:opacity-50"
            title="Refresh countdown"
          >
            <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Edit Mode UI */}
      {isEditing && (
        <div className="mb-4 bg-white bg-opacity-20 p-3 rounded-lg animate-fade-in">
          <label className="block text-xs font-bold mb-1">Target Date</label>
          <div className="flex space-x-2">
            <input
              type="date"
              value={editDate}
              onChange={(e) => setEditDate(e.target.value)}
              className="flex-1 bg-white bg-opacity-90 text-gray-900 rounded px-2 py-1 text-sm outline-none focus:ring-2 focus:ring-yellow-400"
            />
            <button
              onClick={handleSaveDate}
              disabled={isRefreshing}
              className="bg-yellow-500 hover:bg-yellow-600 text-white px-3 py-1 rounded text-xs font-bold transition-colors"
            >
              Save
            </button>
          </div>
        </div>
      )}

      {/* Main Content */}
      {countdownData.hasReached ? (
        /* Celebration State */
        <div className="text-center py-6">
          <div className="mb-4">
            <Star className="h-12 w-12 mx-auto text-yellow-400 animate-bounce" />
          </div>
          <h4 className="text-xl font-bold mb-2">🎉 Congratulations! 🎉</h4>
          <p className="text-lg mb-2">{countdownData.milestone}</p>
          <p className="text-sm text-blue-200">
            The moment has finally arrived!
          </p>

          <div className="mt-4 p-3 bg-white bg-opacity-20 rounded-lg">
            <p className="text-sm font-medium">Target Date Reached</p>
            <p className="text-xs text-blue-200">
              {formatTargetDate(countdownData.targetDate)}
            </p>
          </div>
        </div>
      ) : (
        /* Countdown State */
        <div className="space-y-4">
          {/* Days Remaining - Large Display */}
          <div className="text-center">
            <div className="text-4xl font-bold mb-1">
              {countdownData.daysRemaining.toLocaleString()}
            </div>
            <div className="text-sm text-blue-200">
              {countdownData.daysRemaining === 1 ? 'day' : 'days'} remaining
            </div>
          </div>

          {/* Time Breakdown */}
          {timeBreakdown.years > 0 && (
            <div className="grid grid-cols-3 gap-2 text-center text-sm">
              <div className="bg-white bg-opacity-10 rounded-lg p-2">
                <div className="font-bold text-lg">{timeBreakdown.years}</div>
                <div className="text-xs text-blue-200">
                  {timeBreakdown.years === 1 ? 'Year' : 'Years'}
                </div>
              </div>
              <div className="bg-white bg-opacity-10 rounded-lg p-2">
                <div className="font-bold text-lg">{timeBreakdown.months}</div>
                <div className="text-xs text-blue-200">
                  {timeBreakdown.months === 1 ? 'Month' : 'Months'}
                </div>
              </div>
              <div className="bg-white bg-opacity-10 rounded-lg p-2">
                <div className="font-bold text-lg">{timeBreakdown.days}</div>
                <div className="text-xs text-blue-200">
                  {timeBreakdown.days === 1 ? 'Day' : 'Days'}
                </div>
              </div>
            </div>
          )}

          {/* Progress Bar */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span>Progress</span>
              <span>{Math.round(progressPercentage)}%</span>
            </div>
            <div className="w-full bg-white bg-opacity-20 rounded-full h-2">
              <div
                className="bg-gradient-to-r from-yellow-400 to-yellow-300 h-2 rounded-full transition-all duration-500"
                style={{ width: `${progressPercentage}%` }}
              />
            </div>
          </div>

          {/* Target Information */}
          <div className="bg-white bg-opacity-10 rounded-lg p-3">
            <div className="flex items-center space-x-2 mb-2">
              <Calendar className="h-4 w-4 text-yellow-400" />
              <span className="text-sm font-medium">Target Date</span>
            </div>
            <div className="space-y-1">
              <p className="font-semibold">{countdownData.milestone}</p>
              <p className="text-sm text-blue-200">
                {formatTargetDate(countdownData.targetDate)}
              </p>
            </div>
          </div>

          {/* Motivational Message */}
          <div className="text-center p-3 bg-white bg-opacity-10 rounded-lg">
            <p className="text-sm font-medium">
              {getMotivationalMessage(countdownData.daysRemaining)}
            </p>
          </div>
        </div>
      )}

      {/* Current Time */}
      <div className="mt-4 pt-3 border-t border-white border-opacity-20">
        <div className="flex items-center justify-center space-x-2 text-xs text-blue-200">
          <Clock className="h-3 w-3" />
          <span>
            {currentTime.toLocaleString('en-US', {
              weekday: 'short',
              month: 'short',
              day: 'numeric',
              hour: 'numeric',
              minute: '2-digit',
              hour12: true
            })}
          </span>
        </div>
      </div>
    </div>
  );
};

export default BerkeleyCountdown;