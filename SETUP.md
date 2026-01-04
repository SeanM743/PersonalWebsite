# Personal Dashboard Setup Guide

This guide will help you set up and run the complete Personal Dashboard application with all features enabled.

## Prerequisites

- **Java 21+** (for Spring Boot backend)
- **Node.js 18+** and npm (for React frontend)
- **PostgreSQL 12+** (database)
- **Docker** (optional, for PostgreSQL)

## Environment Variables

Create these environment variables or add them to your system:

### Required API Keys

```bash
# Finnhub API (Portfolio Dashboard) - CONFIGURED
export FINNHUB_API_KEY="d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0"

# Google Cloud (Gemini AI for Chat)
export GOOGLE_CLOUD_PROJECT_ID="your-google-cloud-project-id"
export GOOGLE_CLOUD_LOCATION="us-central1"

# Google Calendar API
export GOOGLE_CALENDAR_CREDENTIALS_PATH="C:/Users/seanm/.config/google/google-calendar-credentials.json"
```

### Optional API Keys (for enhanced features)

```bash
# TMDB API (for better movie/TV data)
export TMDB_API_KEY="your-tmdb-api-key"
```

## Database Setup

### Option 1: Using Docker (Recommended)

```bash
# Start PostgreSQL with Docker
docker run --name personal-dashboard-db \
  -e POSTGRES_DB=personal_platform \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

### Option 2: Local PostgreSQL Installation

1. Install PostgreSQL
2. Create database and user:

```sql
CREATE DATABASE personal_platform;
CREATE USER admin WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE personal_platform TO admin;
```

## API Keys Setup Guide

### 1. Finnhub API (✅ Already Configured)
Your Finnhub API key is already configured: `d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0`

### 2. Google Cloud (Gemini AI) Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable the Vertex AI API
4. Set up authentication (Application Default Credentials)
5. Note your project ID and location

### 3. Google Calendar API Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable the Google Calendar API
3. Create a Service Account
4. Download the JSON credentials file
5. Place it at: `C:/Users/seanm/.config/google/google-calendar-credentials.json`

## Running the Application

### 1. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will be available at: `http://localhost:8080`

### 2. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at: `http://localhost:5173`

## Testing the Setup

### 1. Login
- Navigate to `http://localhost:5173`
- Login with:
  - Username: `admin`
  - Password: `password`

### 2. Test Portfolio (✅ Ready)
- Go to Portfolio section
- Add a stock (e.g., AAPL, MSFT, GOOGL)
- Verify real-time price data loads

### 3. Test Chat (Requires Google Cloud setup)
- Go to Chat section
- Ask: "What's my portfolio performance?"
- Verify AI responses

### 4. Test Calendar (Requires Google Calendar setup)
- Go to Calendar section
- Verify events load from your Google Calendar

## Feature Status

| Feature | Status | Requirements |
|---------|--------|--------------|
| ✅ Authentication | Ready | None |
| ✅ Portfolio Dashboard | Ready | Finnhub API (configured) |
| ⚠️ AI Chat | Needs Setup | Google Cloud credentials |
| ⚠️ Calendar | Needs Setup | Google Calendar credentials |
| ✅ Content Management | Ready | None |
| ✅ Dashboard Overview | Ready | None |

## Troubleshooting

### Backend Issues

1. **Database Connection Error**
   - Ensure PostgreSQL is running
   - Check connection details in `application.properties`

2. **API Key Issues**
   - Verify environment variables are set
   - Check API key validity

### Frontend Issues

1. **API Connection Error**
   - Ensure backend is running on port 8080
   - Check CORS configuration

2. **Build Issues**
   - Run `npm install` to ensure dependencies are installed
   - Clear node_modules and reinstall if needed

## Development Tips

### Hot Reload
- Backend: Use `./mvnw spring-boot:run` for automatic restart
- Frontend: Vite provides instant hot reload

### API Testing
- Backend provides Swagger UI at: `http://localhost:8080/swagger-ui.html`
- Test endpoints directly before frontend integration

### Database Management
- Use pgAdmin or similar tool to inspect database
- Tables are auto-created by Hibernate

## Production Deployment

### Environment Variables for Production

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/personal_platform
SPRING_DATASOURCE_USERNAME=your-db-user
SPRING_DATASOURCE_PASSWORD=your-db-password

# API Keys
FINNHUB_API_KEY=d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0
GOOGLE_CLOUD_PROJECT_ID=your-project-id
GOOGLE_CLOUD_LOCATION=us-central1
GOOGLE_CALENDAR_CREDENTIALS_PATH=/path/to/credentials.json

# Security
JWT_SECRET=your-secure-jwt-secret-key-here
```

### Build Commands

```bash
# Backend
cd backend
./mvnw clean package

# Frontend
cd frontend
npm run build
```

## Next Steps

1. **Set up Google Cloud** for AI chat functionality
2. **Configure Google Calendar** for calendar integration
3. **Add your stock positions** to test portfolio features
4. **Customize the dashboard** with your personal data

## Support

If you encounter any issues:

1. Check the logs in the backend console
2. Verify all environment variables are set
3. Ensure all services are running
4. Check API key validity and quotas

The application is now ready to use with your Finnhub API key configured!