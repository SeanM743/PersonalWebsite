import React from 'react';
import { useTheme } from '../contexts/ThemeContext';
import { Sun, Moon } from 'lucide-react'; // Using emoji for Bears

// Icon wrapper for consistent sizing
const IconWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <div className="w-5 h-5 flex items-center justify-center">{children}</div>
);

const ThemeSwitcher: React.FC = () => {
    const { theme, setTheme } = useTheme();

    return (
        <div className="flex items-center space-x-1 bg-white/10 backdrop-blur-sm rounded-full p-1 border border-white/20">
            <button
                onClick={() => setTheme('light')}
                className={`p-1.5 rounded-full transition-all duration-200 ${theme === 'light'
                    ? 'bg-white text-yellow-500 shadow-sm'
                    : 'text-gray-400 hover:text-white'
                    }`}
                title="Light Mode"
            >
                <IconWrapper><Sun size={16} /></IconWrapper>
            </button>

            <button
                onClick={() => setTheme('dark')}
                className={`p-1.5 rounded-full transition-all duration-200 ${theme === 'dark'
                    ? 'bg-gray-700 text-blue-400 shadow-sm'
                    : 'text-gray-400 hover:text-white'
                    }`}
                title="Dark Mode"
            >
                <IconWrapper><Moon size={16} /></IconWrapper>
            </button>

            <button
                onClick={() => setTheme('bears')}
                className={`p-1.5 rounded-full transition-all duration-200 ${theme === 'bears'
                    ? 'bg-orange-600 text-white shadow-sm'
                    : 'text-gray-400 hover:text-white'
                    }`}
                title="Bears Theme"
            >
                <IconWrapper>
                    <span className="font-bold text-xs">🐻</span>
                </IconWrapper>
            </button>
        </div>
    );
};

export default ThemeSwitcher;
