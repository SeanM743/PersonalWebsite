import React, { useState, useEffect } from 'react';
import { Cloud, MapPin, Loader2, Thermometer, Droplets } from 'lucide-react';
import { apiService } from '../../services/apiService';

interface WeatherData {
    temperature: number;
    feelsLike: number;
    tempMin: number;
    tempMax: number;
    humidity: number;
    description: string;
    icon: string;
    locationName: string;
    country: string;
    timestamp: number;
}

const WeatherWidget: React.FC = () => {
    const [weather, setWeather] = useState<WeatherData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchWeather = async () => {
            try {
                setIsLoading(true);
                const data = await apiService.getWeather() as WeatherData;
                setWeather(data);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch weather:", err);
                setError("Unable to load weather data.");
            } finally {
                setIsLoading(false);
            }
        };

        fetchWeather();

        // Refresh weather data every 30 minutes
        const intervalId = setInterval(fetchWeather, 30 * 60 * 1000);
        return () => clearInterval(intervalId);
    }, []);

    // Get the icon URL from OpenWeatherMap
    const getIconUrl = (iconCode: string) => `https://openweathermap.org/img/wn/${iconCode}@2x.png`;

    // Dynamic background based on temperature
    const getBackgroundStyle = () => {
        if (!weather || weather.temperature === 0) return 'bg-gradient-to-br from-gray-500 to-slate-600';

        const temp = weather.temperature;
        if (temp <= 32) return 'bg-gradient-to-br from-blue-600 to-indigo-800'; // Freezing
        if (temp <= 50) return 'bg-gradient-to-br from-blue-400 to-blue-600';  // Cold
        if (temp <= 70) return 'bg-gradient-to-br from-teal-400 to-emerald-600'; // Mild
        if (temp <= 85) return 'bg-gradient-to-br from-orange-400 to-amber-600'; // Warm
        return 'bg-gradient-to-br from-red-500 to-rose-700'; // Hot
    };

    if (isLoading && !weather) {
        return (
            <div className="bg-card rounded-xl shadow-sm p-6 border border-border h-full flex flex-col items-center justify-center min-h-[160px]">
                <Loader2 className="h-8 w-8 text-primary animate-spin mb-2" />
                <p className="text-sm text-muted">Loading weather...</p>
            </div>
        );
    }

    if (error && !weather) {
        return (
            <div className="bg-card rounded-xl shadow-sm p-6 border border-border h-full flex flex-col items-center justify-center min-h-[160px]">
                <Cloud className="h-8 w-8 text-muted mb-2 opacity-50" />
                <p className="text-sm text-muted">{error}</p>
            </div>
        );
    }

    if (!weather) return null;

    return (
        <div className={`rounded-xl shadow-sm p-5 border border-white/20 h-full text-white relative overflow-hidden ${getBackgroundStyle()}`}>

            {/* Background Decorative Element */}
            <div className="absolute -right-6 -top-6 opacity-10 pointer-events-none">
                <Cloud className="h-40 w-40" />
            </div>

            <div className="flex flex-col h-full justify-between relative z-10">
                {/* Header: Location & Status */}
                <div className="flex justify-between items-start mb-2">
                    <div className="flex items-center space-x-1.5 bg-black/20 px-2.5 py-1 rounded-full backdrop-blur-sm -ml-1">
                        <MapPin className="h-3.5 w-3.5" />
                        <span className="text-xs font-medium truncate max-w-[120px]">
                            {weather.locationName}{weather.country ? `, ${weather.country}` : ''}
                        </span>
                    </div>
                </div>

                {/* Main Content: Temp & Icon */}
                <div className="flex items-center justify-between my-2">
                    <div>
                        <div className="flex items-start">
                            <span className="text-4xl sm:text-5xl font-bold tracking-tighter">
                                {Math.round(weather.temperature)}
                            </span>
                            <span className="text-xl font-medium mt-1 ml-0.5">&deg;F</span>
                        </div>
                        <p className="text-sm font-medium text-white/90 capitalize mt-1 leading-tight">
                            {weather.description}
                        </p>
                    </div>

                    <div className="bg-white/10 rounded-full backdrop-blur-sm shadow-inner shrink-0 w-16 h-16 flex items-center justify-center">
                        {weather.icon ? (
                            <img
                                src={getIconUrl(weather.icon)}
                                alt={weather.description}
                                className="w-20 h-20 object-contain drop-shadow-md"
                            />
                        ) : (
                            <Cloud className="h-8 w-8" />
                        )}
                    </div>
                </div>

                {/* Footer: Details */}
                <div className="grid grid-cols-2 gap-2 mt-auto pt-4 border-t border-white/20">
                    <div className="flex items-center space-x-1.5">
                        <Thermometer className="h-3.5 w-3.5 text-white/70" />
                        <div className="flex flex-col">
                            <span className="text-[10px] text-white/70 uppercase tracking-wider font-semibold leading-none">Feels Like</span>
                            <span className="text-xs font-medium leading-tight">{Math.round(weather.feelsLike)}&deg;</span>
                        </div>
                    </div>
                    <div className="flex items-center space-x-1.5">
                        <Droplets className="h-3.5 w-3.5 text-white/70" />
                        <div className="flex flex-col">
                            <span className="text-[10px] text-white/70 uppercase tracking-wider font-semibold leading-none">Humidity</span>
                            <span className="text-xs font-medium leading-tight">{weather.humidity}%</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default WeatherWidget;
