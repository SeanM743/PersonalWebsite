import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { Calendar as CalendarIcon, Plus, Clock, MapPin } from 'lucide-react';

interface CalendarEvent {
  id: string;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  location?: string;
}

const Calendar: React.FC = () => {
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const { error, success } = useNotification();

  const loadEvents = async () => {
    try {
      setIsLoading(true);
      const response = await apiService.getCalendarEvents();
      if (response.success) {
        setEvents(response.data || []);
      }
    } catch (err: any) {
      error('Failed to load events', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const formatEventTime = (startTime: string, endTime: string) => {
    const start = new Date(startTime);
    const end = new Date(endTime);
    return `${start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  };

  const getEventsForDate = (date: string) => {
    return events.filter(event => 
      new Date(event.startTime).toDateString() === new Date(date).toDateString()
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  const todayEvents = getEventsForDate(selectedDate);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Calendar</h1>
        <button className="btn-primary flex items-center space-x-2">
          <Plus className="h-4 w-4" />
          <span>New Event</span>
        </button>
      </div>

      {/* Date Selector */}
      <div className="card">
        <div className="flex items-center space-x-4">
          <CalendarIcon className="h-5 w-5 text-gray-400" />
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="input-field"
          />
          <span className="text-sm text-gray-500">
            {todayEvents.length} event{todayEvents.length !== 1 ? 's' : ''} on this day
          </span>
        </div>
      </div>

      {/* Events List */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          Events for {new Date(selectedDate).toLocaleDateString()}
        </h2>

        {todayEvents.length > 0 ? (
          <div className="space-y-4">
            {todayEvents.map((event) => (
              <div key={event.id} className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h3 className="font-medium text-gray-900">{event.title}</h3>
                    {event.description && (
                      <p className="text-sm text-gray-600 mt-1">{event.description}</p>
                    )}
                    
                    <div className="flex items-center space-x-4 mt-2 text-sm text-gray-500">
                      <div className="flex items-center space-x-1">
                        <Clock className="h-4 w-4" />
                        <span>{formatEventTime(event.startTime, event.endTime)}</span>
                      </div>
                      {event.location && (
                        <div className="flex items-center space-x-1">
                          <MapPin className="h-4 w-4" />
                          <span>{event.location}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <CalendarIcon className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">No events</h3>
            <p className="mt-1 text-sm text-gray-500">
              No events scheduled for this day.
            </p>
          </div>
        )}
      </div>

      {/* Upcoming Events */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Upcoming Events</h2>
        
        {events.length > 0 ? (
          <div className="space-y-3">
            {events
              .filter(event => new Date(event.startTime) > new Date())
              .slice(0, 5)
              .map((event) => (
                <div key={event.id} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg">
                  <div className="flex-1">
                    <div className="font-medium text-gray-900">{event.title}</div>
                    <div className="text-sm text-gray-500">
                      {new Date(event.startTime).toLocaleDateString()} at{' '}
                      {new Date(event.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                </div>
              ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            No upcoming events
          </div>
        )}
      </div>
    </div>
  );
};

export default Calendar;