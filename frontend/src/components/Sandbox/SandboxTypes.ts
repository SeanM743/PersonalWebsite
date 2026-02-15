export interface SandboxPortfolio {
    id: number;
    userId: number;
    name: string;
    description: string;
    initialBalance: number;
    currentBalance: number;
    holdingsValue?: number;
    totalValue?: number;
    totalGainLoss?: number;
    totalGainLossPercentage?: number;
    createdAt: string;
    updatedAt: string;
}

export interface SandboxHolding {
    id: number;
    symbol: string;
    quantity: number;
    averageCost: number;
    currentPrice: number;
    marketValue: number;
    totalGainLoss: number;
    totalGainLossPercentage: number;
}

export interface SandboxTransaction {
    id: number;
    symbol: string;
    type: 'BUY' | 'SELL';
    quantity: number;
    price: number;
    totalCost: number;
    transactionDate: string;
    createdAt: string;
}

export interface SandboxPortfolioDetail extends SandboxPortfolio {
    holdings: SandboxHolding[];
    recentTransactions: SandboxTransaction[];
}
