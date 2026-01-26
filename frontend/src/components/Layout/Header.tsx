import React from 'react';
import { NavLink } from 'react-router-dom';
import { Bell, User, LogOut } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import ThemeSwitcher from '../ThemeSwitcher';

const Header: React.FC = () => {
  const { user, logout } = useAuth();

  const navigation = [
    { name: 'Home', href: '/dashboard' },
    { name: 'Finance', href: '/portfolio' },
    { name: 'Life', href: '/life-events' },
    { name: 'Calendar', href: '/calendar' },
    { name: 'Monitoring', href: '/monitoring' },
  ];

  return (
    <header className="bg-card shadow-sm border-b border-border transition-colors duration-300">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 justify-between items-center">
          {/* Left side - Brand */}
          <div className="flex items-center">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-main rounded-full flex items-center justify-center">
                <span className="text-page text-sm font-bold">S</span>
              </div>
              <h1 className="text-xl font-semibold text-main">
                Sean's Spot
              </h1>
            </div>
          </div>

          {/* Center - Navigation */}
          <div className="flex space-x-1">
            {navigation.map((item) => (
              <NavLink
                key={item.name}
                to={item.href}
                className={({ isActive }) =>
                  `px-4 py-2 text-sm font-medium rounded-lg transition-colors ${isActive
                    ? 'bg-blue-500 text-white'
                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                  }`
                }
              >
                {item.name}
              </NavLink>
            ))}
          </div>

          {/* Right side - User menu */}
          <div className="flex items-center space-x-4">
            <button className="p-2 text-muted hover:text-main focus:outline-none focus:ring-2 focus:ring-primary rounded-lg">
              <Bell className="h-5 w-5" />
            </button>

            <ThemeSwitcher />

            <div className="relative group">
              <button className="flex items-center space-x-2 p-2 text-muted hover:text-main focus:outline-none focus:ring-2 focus:ring-primary rounded-lg">
                <User className="h-5 w-5" />
                <span className="hidden sm:block text-sm font-medium">
                  {user?.username}
                </span>
              </button>

              <div className="absolute right-0 mt-2 w-48 bg-card border border-border rounded-md shadow-lg py-1 z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
                <div className="px-4 py-2 text-sm text-main border-b border-border">
                  <div className="font-medium">{user?.username}</div>
                  <div className="text-muted text-xs">{user?.role}</div>
                </div>
                <button
                  onClick={logout}
                  className="flex items-center w-full px-4 py-2 text-sm text-muted hover:text-main hover:bg-page"
                >
                  <LogOut className="h-4 w-4 mr-2" />
                  Sign out
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;