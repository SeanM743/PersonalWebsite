import React, { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'bears';

interface ThemeContextType {
    theme: Theme;
    setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    // Initialize theme from localStorage or default to 'light'
    const [theme, setTheme] = useState<Theme>(() => {
        const savedTheme = localStorage.getItem('theme');
        return (savedTheme as Theme) || 'light';
    });

    useEffect(() => {
        const root = window.document.documentElement;

        // Remove old theme attributes
        root.removeAttribute('data-theme');
        root.classList.remove('dark');

        // Apply new theme
        if (theme === 'dark') {
            root.classList.add('dark');
            root.setAttribute('data-theme', 'dark');
        } else if (theme === 'bears') {
            root.setAttribute('data-theme', 'bears');
            // Bears theme is effectively a dark mode variant, so we add 'dark' class for Tailwind dark: utilities
            root.classList.add('dark');
        } else {
            root.setAttribute('data-theme', 'light');
        }

        // Persist to localStorage
        localStorage.setItem('theme', theme);
    }, [theme]);

    return (
        <ThemeContext.Provider value={{ theme, setTheme }}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (context === undefined) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
};
