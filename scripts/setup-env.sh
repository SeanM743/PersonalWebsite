#!/bin/bash

# Personal Dashboard Environment Setup Script

echo "🚀 Setting up Personal Dashboard environment variables..."

# Load environment variables from .env if it exists
if [ -f .env ]; then
  echo "📄 Loading variables from .env..."
  set -a
  source .env
  set +a
  echo "✅ Loaded .env variables"
fi

# Finnhub API Key (fallback if not in .env)
if [ -z "$FINNHUB_API_KEY" ]; then
    export FINNHUB_API_KEY="d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0"
    echo "Using default Finnhub API Key"
fi

# Google Calendar credentials path
export GOOGLE_CALENDAR_CREDENTIALS_PATH="C:/Users/seanm/.config/google/google-calendar-credentials.json"
echo "✅ Google Calendar credentials path set"

# Create the credentials directory if it doesn't exist
mkdir -p "C:/Users/seanm/.config/google/"
echo "✅ Created credentials directory"

echo ""
echo "📋 Environment variables set:"
echo "FINNHUB_API_KEY=d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0"
echo "GOOGLE_CALENDAR_CREDENTIALS_PATH=C:/Users/seanm/.config/google/google-calendar-credentials.json"

echo ""
echo "⚠️  Still needed (optional):"
echo "GOOGLE_CLOUD_PROJECT_ID=your-google-cloud-project-id"
echo "GOOGLE_CLOUD_LOCATION=us-central1"

echo ""
echo "🎯 Next steps:"
echo "1. Start PostgreSQL database"
echo "2. Run: cd backend && ./mvnw spring-boot:run"
echo "3. Run: cd frontend && npm install && npm run dev"
echo "4. Visit: http://localhost:5173"

echo ""
echo "✨ Your Finnhub API key is ready for real-time stock data!"