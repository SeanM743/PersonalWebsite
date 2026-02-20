import React, { useState, useEffect, useMemo } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { Calendar as CalendarIcon, Clock, MapPin, ChevronLeft, ChevronRight } from 'lucide-react';

interface CalendarEvent {
  id: string;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  location?: string;
}

/** Parse a YYYY-MM-DD string as a local date (avoids UTC off-by-one) */
const parseLocalDate = (dateStr: string): Date => {
  const [y, m, d] = dateStr.split('-').map(Number);
  return new Date(y, m - 1, d);
};

const Calendar: React.FC = () => {
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const { error } = useNotification();

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

  // ── helpers ──────────────────────────────────────────────────
  const formatEventTime = (startTime: string, endTime: string) => {
    const start = new Date(startTime);
    const end = new Date(endTime);
    return `${start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} – ${end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  };

  const isSameDay = (d1: Date, d2: Date) =>
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate();

  const eventsForDate = useMemo(
    () =>
      events.filter((e) => {
        const eventDate = new Date(e.startTime);
        return isSameDay(eventDate, selectedDate);
      }),
    [events, selectedDate],
  );

  const upcomingEvents = useMemo(
    () =>
      events
        .filter((e) => new Date(e.startTime) > new Date())
        .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
        .slice(0, 8),
    [events],
  );

  // ── mini-calendar helpers ────────────────────────────────────
  const [viewMonth, setViewMonth] = useState(new Date());

  const daysInMonth = useMemo(() => {
    const year = viewMonth.getFullYear();
    const month = viewMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay(); // 0=Sun
    const totalDays = new Date(year, month + 1, 0).getDate();

    const days: (Date | null)[] = [];
    for (let i = 0; i < firstDay; i++) days.push(null);
    for (let d = 1; d <= totalDays; d++) days.push(new Date(year, month, d));
    return days;
  }, [viewMonth]);

  const monthLabel = viewMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  const prevMonth = () =>
    setViewMonth(new Date(viewMonth.getFullYear(), viewMonth.getMonth() - 1, 1));
  const nextMonth = () =>
    setViewMonth(new Date(viewMonth.getFullYear(), viewMonth.getMonth() + 1, 1));

  const hasEventsOnDay = (day: Date) =>
    events.some((e) => isSameDay(new Date(e.startTime), day));

  const isToday = (day: Date) => isSameDay(day, new Date());

  // ── formatted header date ────────────────────────────────────
  const selectedDateLabel = selectedDate.toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  });

  // ── render ───────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="w-full p-6 bg-page min-h-screen transition-colors duration-300">
      {/* Two-column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* ══════ LEFT: Mini Calendar + Upcoming ══════ */}
        <div className="space-y-6">
          {/* Mini Calendar */}
          <div className="bg-card rounded-xl shadow-sm border border-border p-5">
            {/* Month nav */}
            <div className="flex items-center justify-between mb-4">
              <button onClick={prevMonth} className="p-1.5 rounded-lg hover:bg-page transition-colors text-muted hover:text-main">
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="text-sm font-semibold text-main">{monthLabel}</span>
              <button onClick={nextMonth} className="p-1.5 rounded-lg hover:bg-page transition-colors text-muted hover:text-main">
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>

            {/* Weekday headers */}
            <div className="grid grid-cols-7 gap-1 mb-1">
              {['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map((d) => (
                <div key={d} className="text-center text-xs font-medium text-muted py-1">
                  {d}
                </div>
              ))}
            </div>

            {/* Day cells */}
            <div className="grid grid-cols-7 gap-1">
              {daysInMonth.map((day, i) =>
                day ? (
                  <button
                    key={i}
                    onClick={() => setSelectedDate(day)}
                    className={`
                      relative text-sm rounded-lg py-1.5 transition-all duration-150
                      ${isSameDay(day, selectedDate)
                        ? 'bg-primary text-white font-semibold shadow-sm'
                        : isToday(day)
                          ? 'bg-primary/15 text-primary font-medium'
                          : 'text-main hover:bg-page'}
                    `}
                  >
                    {day.getDate()}
                    {hasEventsOnDay(day) && !isSameDay(day, selectedDate) && (
                      <span className="absolute bottom-0.5 left-1/2 -translate-x-1/2 w-1 h-1 bg-primary rounded-full" />
                    )}
                  </button>
                ) : (
                  <div key={i} />
                ),
              )}
            </div>

            {/* Jump to today */}
            <button
              onClick={() => {
                const today = new Date();
                setSelectedDate(today);
                setViewMonth(new Date(today.getFullYear(), today.getMonth(), 1));
              }}
              className="mt-3 w-full text-xs text-center text-primary hover:text-primary/80 font-medium transition-colors"
            >
              Today
            </button>
          </div>

          {/* Upcoming Events */}
          <div className="bg-card rounded-xl shadow-sm border border-border p-5">
            <h2 className="text-sm font-semibold text-main mb-3 uppercase tracking-wider">Upcoming</h2>

            {upcomingEvents.length > 0 ? (
              <div className="space-y-2">
                {upcomingEvents.map((event) => (
                  <button
                    key={event.id}
                    onClick={() => {
                      const d = new Date(event.startTime);
                      setSelectedDate(d);
                      setViewMonth(new Date(d.getFullYear(), d.getMonth(), 1));
                    }}
                    className="w-full text-left flex items-start gap-3 p-2.5 rounded-lg hover:bg-page transition-colors group"
                  >
                    {/* Accent bar */}
                    <div className="w-1 self-stretch rounded-full bg-primary/60 group-hover:bg-primary transition-colors flex-shrink-0" />
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-medium text-main truncate">{event.title}</div>
                      <div className="text-xs text-muted mt-0.5">
                        {new Date(event.startTime).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                        {' · '}
                        {new Date(event.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-sm text-muted text-center py-4">No upcoming events</p>
            )}
          </div>
        </div>

        {/* ══════ RIGHT: Day Detail ══════ */}
        <div className="lg:col-span-2 space-y-6">
          {/* Day header */}
          <div className="bg-card rounded-xl shadow-sm border border-border p-5">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-xl font-bold text-main">{selectedDateLabel}</h1>
                <p className="text-sm text-muted mt-0.5">
                  {eventsForDate.length} event{eventsForDate.length !== 1 ? 's' : ''}
                </p>
              </div>
              <CalendarIcon className="h-6 w-6 text-muted" />
            </div>
          </div>

          {/* Events list */}
          <div className="bg-card rounded-xl shadow-sm border border-border p-5">
            {eventsForDate.length > 0 ? (
              <div className="space-y-3">
                {eventsForDate.map((event) => (
                  <div
                    key={event.id}
                    className="flex gap-4 p-4 rounded-lg border border-border hover:bg-page/50 transition-colors"
                  >
                    {/* Time stripe */}
                    <div className="flex-shrink-0 w-1 rounded-full bg-primary" />

                    <div className="min-w-0 flex-1">
                      <h3 className="font-semibold text-main">{event.title}</h3>
                      {event.description && (
                        <p className="text-sm text-muted mt-1 line-clamp-2">{event.description}</p>
                      )}

                      <div className="flex flex-wrap items-center gap-4 mt-2 text-xs text-muted">
                        <div className="flex items-center gap-1">
                          <Clock className="h-3.5 w-3.5" />
                          <span>{formatEventTime(event.startTime, event.endTime)}</span>
                        </div>
                        {event.location && (
                          <div className="flex items-center gap-1">
                            <MapPin className="h-3.5 w-3.5" />
                            <span>{event.location}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-16">
                <CalendarIcon className="mx-auto h-10 w-10 text-muted/40" />
                <h3 className="mt-3 text-sm font-medium text-main">No events</h3>
                <p className="mt-1 text-xs text-muted">Nothing scheduled for this day.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Calendar;