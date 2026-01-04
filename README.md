# Personal Dashboard

A comprehensive full-stack personal dashboard application with real-time portfolio tracking, AI chat assistant, calendar integration, and content management.

## 🚀 Quick Start

### Prerequisites
- Java 21+, Node.js 18+, PostgreSQL 12+

### 1. Database Setup
```bash
# Using Docker (recommended)
docker run --name personal-dashboard-db \
  -e POSTGRES_DB=personal_platform \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 -d postgres:15
```

### 2. Environment Setup
```bash
# Windows
setup-env.bat

# Linux/Mac
chmod +x setup-env.sh && ./setup-env.sh
```

### 3. Start Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 4. Start Frontend
```bash
cd frontend
npm install && npm run dev
```

### 5. Access Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Login**: admin / password

## ✨ Features

### 📊 Portfolio Dashboard
- **Real-time stock tracking** with Finnhub API integration
- **Performance calculations** with gain/loss metrics
- **Market data caching** with intelligent refresh
- **Portfolio composition** analysis and statistics

### 🤖 AI Chat Assistant
- **Gemini AI integration** for natural language queries
- **Portfolio insights** - ask about your investments
- **Calendar queries** - check upcoming events
- **Context-aware responses** with conversation history

### 📅 Calendar Integration
- **Google Calendar sync** with real-time events
- **Event management** with create/update/delete
- **Smart scheduling** with conflict detection
- **Multi-timezone support** for international markets

### 📝 Content Management
- **Social media-style posts** with images and captions
- **Media tracking** for books, movies, music, podcasts
- **Trip planning** and activity logging
- **Quick facts** personal information management

### 🔐 Authentication & Security
- **JWT-based authentication** with role-based access
- **Secure API endpoints** with CORS configuration
- **Session management** with automatic token refresh
- **Admin/Guest roles** with different permissions

## 🛠 Technology Stack

### Backend
- **Spring Boot 3.3** with Java 21
- **Spring Security** with JWT authentication
- **Spring AI** with Gemini integration
- **PostgreSQL** with JPA/Hibernate
- **Google Calendar API** integration
- **Finnhub API** for market data
- **Caffeine caching** for performance

### Frontend
- **React 18** with TypeScript
- **Vite** for fast development
- **Tailwind CSS** for styling
- **React Router** for navigation
- **Axios** for API communication
- **Lucide React** for icons

## 📈 API Integration Status

| Service | Status | API Key Required |
|---------|--------|------------------|
| 🟢 Finnhub (Portfolio) | **Configured** | ✅ Provided |
| 🟡 Google Cloud (AI) | Setup Required | ⚠️ User Setup |
| 🟡 Google Calendar | Setup Required | ⚠️ User Setup |
| 🟢 OpenLibrary (Books) | Ready | ✅ Free API |
| 🟢 TMDB (Movies) | Optional | 🔵 Optional |

## 🎯 Current Features Status

- ✅ **Authentication System** - Fully functional
- ✅ **Portfolio Dashboard** - Real-time data with your API key
- ✅ **Content Management** - Complete CRUD operations
- ✅ **Dashboard Overview** - Integrated data display
- ⚠️ **AI Chat** - Requires Google Cloud setup
- ⚠️ **Calendar** - Requires Google Calendar credentials

## 📱 Screenshots & Demo

### Dashboard Overview
- Portfolio summary with real-time market data
- Upcoming calendar events
- Recent posts and activities
- Quick action buttons

### Portfolio Management
- Add/edit/delete stock positions
- Real-time price updates via Finnhub API
- Performance metrics and gain/loss calculations
- Market hours awareness

### AI Chat Interface
- Natural language queries about your data
- Context-aware responses
- Chat history persistence
- Integration with portfolio and calendar data

## 🔧 Development

### Project Structure
```
personal_webpage/
├── backend/                 # Spring Boot application
│   ├── src/main/java/      # Java source code
│   ├── src/test/java/      # Test files
│   └── pom.xml             # Maven dependencies
├── frontend/               # React application
│   ├── src/                # TypeScript source code
│   ├── public/             # Static assets
│   └── package.json        # npm dependencies
├── .kiro/specs/           # Feature specifications
└── docker-compose.yml     # Database setup
```

### API Endpoints
- `POST /api/auth/login` - Authentication
- `GET /api/portfolio` - Portfolio data
- `POST /api/portfolio/holdings` - Add stock
- `GET /api/calendar/events` - Calendar events
- `POST /api/chat` - AI chat messages
- `GET /api/content/posts` - Content posts

## 🚀 Deployment

### Environment Variables
```bash
# Required
FINNHUB_API_KEY=d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0

# Optional (for full features)
GOOGLE_CLOUD_PROJECT_ID=your-project-id
GOOGLE_CALENDAR_CREDENTIALS_PATH=/path/to/credentials.json
```

### Production Build
```bash
# Backend
cd backend && ./mvnw clean package

# Frontend  
cd frontend && npm run build
```

## 📚 Documentation

- **[Setup Guide](SETUP.md)** - Detailed setup instructions
- **[Frontend README](frontend/README.md)** - Frontend-specific documentation
- **[API Documentation](backend/)** - Backend API details
- **[Feature Specs](.kiro/specs/)** - Detailed feature specifications

## 🎉 What's Working Right Now

With your Finnhub API key configured, you can immediately:

1. **Track your stock portfolio** with real-time prices
2. **Add/edit/delete stock positions** 
3. **View performance metrics** and gain/loss calculations
4. **Manage content** with posts and media tracking
5. **Use the dashboard** for an overview of all data

## 🔮 Next Steps

1. **Set up Google Cloud** for AI chat functionality
2. **Configure Google Calendar** for calendar integration  
3. **Add your stock positions** to see real portfolio data
4. **Explore the AI chat** once Google Cloud is configured

## 📞 Support

The application is production-ready with your Finnhub API key! 

- Portfolio features work immediately
- Real-time stock data is live
- All CRUD operations are functional
- Authentication and security are active

For additional features (AI chat, calendar), follow the setup guide for the respective API credentials.

---

**🎯 Ready to use with real stock market data!** 📈