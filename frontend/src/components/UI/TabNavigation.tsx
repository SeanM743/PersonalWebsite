import React from 'react';
import { LucideIcon } from 'lucide-react';

interface Tab {
    id: string;
    label: string;
    icon: LucideIcon;
}

interface TabNavigationProps {
    tabs: Tab[];
    activeTab: string;
    onTabChange: (tabId: string) => void;
    className?: string;
}

const TabNavigation: React.FC<TabNavigationProps> = ({
    tabs,
    activeTab,
    onTabChange,
    className = ""
}) => {
    return (
        <div className={`bg-card rounded-xl shadow-sm p-1.5 inline-flex flex-wrap gap-1 border border-border ${className}`}>
            {tabs.map((tab) => {
                const isActive = activeTab === tab.id;
                const Icon = tab.icon;

                return (
                    <button
                        key={tab.id}
                        onClick={() => onTabChange(tab.id)}
                        className={`
              flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-300
              ${isActive
                                ? 'bg-blue-600 text-white shadow-md transform scale-105'
                                : 'text-muted hover:bg-page hover:text-primary'
                            }
            `}
                    >
                        <Icon className={`h-4 w-4 ${isActive ? 'animate-bounce-subtle' : ''}`} />
                        <span>{tab.label}</span>
                    </button>
                );
            })}
        </div>
    );
};

export default TabNavigation;
