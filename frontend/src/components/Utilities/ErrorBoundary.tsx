import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
    errorInfo: ErrorInfo | null;
}

class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null,
        errorInfo: null
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error, errorInfo: null };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error("Uncaught error:", error, errorInfo);
        this.setState({
            error,
            errorInfo
        });
    }

    public render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                    <div className="bg-white rounded-lg shadow-xl p-8 max-w-lg w-full border border-red-200">
                        <div className="flex items-center space-x-3 text-red-600 mb-4">
                            <div className="p-3 bg-red-100 rounded-full">
                                <AlertTriangle className="h-8 w-8" />
                            </div>
                            <h1 className="text-2xl font-bold">Something went wrong</h1>
                        </div>

                        <div className="bg-gray-100 p-4 rounded-md overflow-x-auto mb-6 text-sm font-mono text-gray-800 border border-gray-200 shadow-inner">
                            <p className="font-bold mb-2 text-red-700">{this.state.error?.toString()}</p>
                            <pre className="text-xs text-gray-600 whitespace-pre-wrap">
                                {this.state.errorInfo?.componentStack}
                            </pre>
                        </div>

                        <div className="flex space-x-4">
                            <button
                                onClick={() => window.location.reload()}
                                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-medium transition-colors flex items-center justify-center"
                            >
                                <RefreshCw className="h-4 w-4 mr-2" />
                                Reload Page
                            </button>
                            <button
                                onClick={() => {
                                    localStorage.clear();
                                    window.location.reload();
                                }}
                                className="flex-1 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 px-4 py-2 rounded-lg font-medium transition-colors"
                            >
                                Clear Cache & Reload
                            </button>
                        </div>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
