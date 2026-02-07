import React, { useState, useEffect } from 'react';
import {
  Users,
  Edit,
  Save,
  X,
  Heart,
  GraduationCap,
  Car,
  BookOpen,
  Trophy,
  RefreshCw,
  Clock
} from 'lucide-react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from './UI/LoadingSpinner';

interface FamilyMember {
  id: number;
  name: string;
  primaryActivity: string;
  status: string;
  notes?: string;
  updatedAt: string;
}

interface FamilyPulseProps {
  className?: string;
}

const FamilyPulse: React.FC<FamilyPulseProps> = ({ className = "" }) => {
  const [familyMembers, setFamilyMembers] = useState<FamilyMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [editingMember, setEditingMember] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({
    primaryActivity: '',
    status: '',
    notes: ''
  });
  const { success, error } = useNotification();

  // Get icon for family member based on their activity
  const getActivityIcon = (activity: string) => {
    const activityLower = activity.toLowerCase();

    if (activityLower.includes('wsu') || activityLower.includes('college') || activityLower.includes('university')) {
      return <GraduationCap className="h-4 w-4 text-blue-500" />;
    } else if (activityLower.includes('driving') || activityLower.includes('car')) {
      return <Car className="h-4 w-4 text-green-500" />;
    } else if (activityLower.includes('school') || activityLower.includes('study')) {
      return <BookOpen className="h-4 w-4 text-purple-500" />;
    } else if (activityLower.includes('sport') || activityLower.includes('game')) {
      return <Trophy className="h-4 w-4 text-orange-500" />;
    } else {
      return <Heart className="h-4 w-4 text-red-500" />;
    }
  };

  // Get status color
  const getStatusColor = (status: string) => {
    const statusLower = status.toLowerCase();

    if (statusLower.includes('great') || statusLower.includes('excellent') || statusLower.includes('amazing')) {
      return 'text-green-600 bg-green-50 border-green-200';
    } else if (statusLower.includes('good') || statusLower.includes('well') || statusLower.includes('fine')) {
      return 'text-blue-600 bg-blue-50 border-blue-200';
    } else if (statusLower.includes('busy') || statusLower.includes('working') || statusLower.includes('studying')) {
      return 'text-yellow-600 bg-yellow-50 border-yellow-200';
    } else if (statusLower.includes('tired') || statusLower.includes('stressed') || statusLower.includes('difficult')) {
      return 'text-orange-600 bg-orange-50 border-orange-200';
    } else {
      return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  // Load family pulse data
  const loadFamilyPulse = async (isRefresh = false) => {
    try {
      if (isRefresh) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }

      const response = await apiService.getFamilyPulse();

      if (response.success) {
        setFamilyMembers(response.data || []);
      }
    } catch (err: any) {
      error('Failed to load family pulse data', err.message);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  // Start editing a family member
  const startEditing = (member: FamilyMember) => {
    setEditingMember(member.id);
    setEditForm({
      primaryActivity: member.primaryActivity,
      status: member.status,
      notes: member.notes || ''
    });
  };

  // Cancel editing
  const cancelEditing = () => {
    setEditingMember(null);
    setEditForm({
      primaryActivity: '',
      status: '',
      notes: ''
    });
  };

  // Save family member update
  const saveMemberUpdate = async (memberId: number) => {
    try {
      const response = await apiService.updateFamilyMember(memberId, editForm);

      if (response.success) {
        // Update the local state
        setFamilyMembers(prev => prev.map(member =>
          member.id === memberId
            ? { ...member, ...editForm, updatedAt: new Date().toISOString() }
            : member
        ));

        success('Family member updated successfully');
        cancelEditing();
      }
    } catch (err: any) {
      error('Failed to update family member', err.message);
    }
  };

  // Format relative time
  const formatRelativeTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMinutes / 60);
    const diffInDays = Math.floor(diffInHours / 24);

    if (diffInMinutes < 1) {
      return 'Just now';
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`;
    } else if (diffInHours < 24) {
      return `${diffInHours}h ago`;
    } else if (diffInDays === 1) {
      return 'Yesterday';
    } else if (diffInDays < 7) {
      return `${diffInDays}d ago`;
    } else {
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
      });
    }
  };

  useEffect(() => {
    loadFamilyPulse();
  }, []);

  if (isLoading) {
    return (
      <div className={`bg-gradient-to-br from-pink-500 to-rose-600 rounded-xl shadow-sm p-4 text-white ${className}`}>
        <div className="flex items-center justify-center h-32">
          <LoadingSpinner size="medium" />
        </div>
      </div>
    );
  }

  return (
    <div className={`bg-gradient-to-br from-pink-500 to-rose-600 rounded-xl shadow-sm p-4 text-white relative overflow-hidden ${className}`}>
      {/* Background Element */}
      <div className="absolute top-2 right-2 opacity-10 pointer-events-none">
        <Users className="h-16 w-16" />
      </div>

      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Users className="h-6 w-6 text-pink-200" />
          <h3 className="text-lg font-bold">Family Pulse</h3>
        </div>

        <button
          onClick={() => loadFamilyPulse(true)}
          disabled={isRefreshing}
          className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors disabled:opacity-50"
          title="Refresh family pulse"
        >
          <RefreshCw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Family Members */}
      {familyMembers.length > 0 ? (
        <div className="space-y-3">
          {familyMembers.map(member => (
            <div key={member.id} className="bg-white bg-opacity-10 rounded-lg p-3">
              {editingMember === member.id ? (
                /* Edit Mode */
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <h4 className="font-semibold text-lg">{member.name}</h4>
                    <div className="flex items-center space-x-1">
                      <button
                        onClick={() => saveMemberUpdate(member.id)}
                        className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors"
                        title="Save changes"
                      >
                        <Save className="h-4 w-4 text-green-300" />
                      </button>
                      <button
                        onClick={cancelEditing}
                        className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors"
                        title="Cancel editing"
                      >
                        <X className="h-4 w-4 text-red-300" />
                      </button>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div>
                      <label className="block text-xs text-pink-200 mb-1">Primary Activity</label>
                      <input
                        type="text"
                        value={editForm.primaryActivity}
                        onChange={(e) => setEditForm(prev => ({ ...prev, primaryActivity: e.target.value }))}
                        className="w-full px-2 py-1 bg-white bg-opacity-20 rounded text-white placeholder-pink-200 text-sm focus:outline-none focus:ring-2 focus:ring-white focus:ring-opacity-50"
                        placeholder="What are they up to?"
                      />
                    </div>

                    <div>
                      <label className="block text-xs text-pink-200 mb-1">Status</label>
                      <input
                        type="text"
                        value={editForm.status}
                        onChange={(e) => setEditForm(prev => ({ ...prev, status: e.target.value }))}
                        className="w-full px-2 py-1 bg-white bg-opacity-20 rounded text-white placeholder-pink-200 text-sm focus:outline-none focus:ring-2 focus:ring-white focus:ring-opacity-50"
                        placeholder="How are they doing?"
                      />
                    </div>

                    <div>
                      <label className="block text-xs text-pink-200 mb-1">Notes (Optional)</label>
                      <textarea
                        value={editForm.notes}
                        onChange={(e) => setEditForm(prev => ({ ...prev, notes: e.target.value }))}
                        rows={2}
                        className="w-full px-2 py-1 bg-white bg-opacity-20 rounded text-white placeholder-pink-200 text-sm focus:outline-none focus:ring-2 focus:ring-white focus:ring-opacity-50 resize-none"
                        placeholder="Additional notes..."
                      />
                    </div>
                  </div>
                </div>
              ) : (
                /* View Mode */
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <h4 className="font-semibold text-lg">{member.name}</h4>
                      {getActivityIcon(member.primaryActivity)}
                    </div>
                    <button
                      onClick={() => startEditing(member)}
                      className="p-1 hover:bg-white hover:bg-opacity-20 rounded transition-colors opacity-70 hover:opacity-100"
                      title="Edit member"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                  </div>

                  <div className="space-y-2">
                    <div>
                      <div className="text-sm text-pink-200">Primary Activity</div>
                      <div className="font-medium">{member.primaryActivity}</div>
                    </div>

                    <div>
                      <div className="text-sm text-pink-200">Status</div>
                      <div className={`inline-block px-2 py-1 rounded-full text-xs font-medium border ${getStatusColor(member.status)}`}>
                        {member.status}
                      </div>
                    </div>

                    {member.notes && (
                      <div>
                        <div className="text-sm text-pink-200">Notes</div>
                        <div className="text-sm opacity-90">{member.notes}</div>
                      </div>
                    )}

                    <div className="flex items-center space-x-1 text-xs text-pink-200 pt-1">
                      <Clock className="h-3 w-3" />
                      <span>Updated {formatRelativeTime(member.updatedAt)}</span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        /* Empty State */
        <div className="text-center py-8">
          <Users className="h-8 w-8 mx-auto mb-2 text-pink-300" />
          <p className="text-sm mb-1">No family members found</p>
          <p className="text-xs text-pink-200">
            Family pulse data will appear here when available
          </p>
        </div>
      )}

      {/* Summary */}
      {familyMembers.length > 0 && (
        <div className="mt-4 pt-3 border-t border-white border-opacity-20">
          <div className="text-center">
            <p className="text-xs text-pink-200">
              {familyMembers.length} family {familyMembers.length === 1 ? 'member' : 'members'} tracked
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default FamilyPulse;