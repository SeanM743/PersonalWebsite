import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';
import ErrorBoundary from './components/Utilities/ErrorBoundary';
import { ThemeProvider } from './contexts/ThemeContext';
import { PortfolioProvider } from './contexts/PortfolioContext';
import Header from './components/Layout/Header';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import Portfolio from './pages/Portfolio';
import Calendar from './pages/Calendar';
import Content from './pages/Content';
import Chat from './pages/Chat';
import LifeEvents from './pages/LifeEvents';
import NewsPage from './pages/NewsPage';
import NewsSettings from './pages/NewsSettings';

import LoadingSpinner from './components/UI/LoadingSpinner';
import { apiService } from './services/apiService';

const AppContent: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  // Pre-fetch/Check news in background when app loads
  React.useEffect(() => {
    if (isAuthenticated) {
      // Smart refresh (only if stale)
      apiService.refreshNews(false).catch(console.error);
    }
  }, [isAuthenticated]);

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />

      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/portfolio" element={<Portfolio />} />
          <Route path="/calendar" element={<Calendar />} />
          <Route path="/content" element={<Content />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/life-events" element={<LifeEvents />} />
          <Route path="/news" element={<NewsPage />} />
          <Route path="/news/settings" element={<NewsSettings />} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </main>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <Router>
      <AuthProvider>
        <ThemeProvider>
          <NotificationProvider>
            <PortfolioProvider>
              <ErrorBoundary>
                <AppContent />
              </ErrorBoundary>
            </PortfolioProvider>
          </NotificationProvider>
        </ThemeProvider>
      </AuthProvider>
    </Router>
  );
};

export default App;