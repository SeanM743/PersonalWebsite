# Personal Dashboard - Docker Setup

This document explains how to run the Personal Dashboard application using Docker containers.

## 🐳 Docker Configurations

### Production Setup (`docker-compose.prod.yml`)
- **Frontend**: Nginx serving optimized React build on port 80
- **Backend**: Spring Boot JAR in production mode on port 8080  
- **Database**: PostgreSQL on port 5432
- **Features**: Optimized builds, health checks, proper networking

### Development Setup (`docker-compose.dev.yml`)
- **Frontend**: Vite dev server with hot reload on port 5173
- **Backend**: Maven with Spring DevTools hot reload on port 8080
- **Database**: PostgreSQL on port 5432
- **Features**: Hot reload, development tools, volume mounts

## 🚀 Quick Start

### Production Deployment
```bash
# Build and start all services
docker-compose -f docker-compose.prod.yml up --build

# Access the application
# Frontend: http://localhost
# Backend API: http://localhost:8080
# Database: localhost:5432
```

### Development Environment
```bash
# Build and start all services with hot reload
docker-compose -f docker-compose.dev.yml up --build

# Access the application
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080
# Database: localhost:5432
```

## 🔧 Configuration

### Environment Variables
Create a `.env` file in the root directory:

```env
# Google Cloud Configuration (Optional)
GOOGLE_CLOUD_PROJECT_ID=your-project-id
GOOGLE_CLOUD_LOCATION=us-central1

# Google Calendar Credentials Path (Optional)
GOOGLE_CALENDAR_CREDENTIALS_PATH=/path/to/your/credentials.json
```

### API Keys
The following API keys are pre-configured:
- **Finnhub API**: `d56snvhr01qkvkasbedgd56snvhr01qkvkasbee0`

To enable additional features, add these to your `.env` file:
- `GOOGLE_CLOUD_PROJECT_ID` - For AI chat functionality
- `GOOGLE_CALENDAR_CREDENTIALS_PATH` - For calendar integration

## 📊 Default Login Credentials

**Admin User:**
- Username: `admin`
- Password: `password`

**Guest User:**
- Username: `guest`
- Password: `password`

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   Database      │
│   (React/Nginx) │◄──►│  (Spring Boot)  │◄──►│  (PostgreSQL)   │
│   Port 80/5173  │    │   Port 8080     │    │   Port 5432     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔍 Health Checks

All services include health checks:
- **Frontend**: HTTP check on port 80/5173
- **Backend**: Spring Actuator health endpoint
- **Database**: PostgreSQL ready check

## 📝 Available Services

### Frontend (React + Vite/Nginx)
- **Development**: Hot reload, source maps, dev tools
- **Production**: Optimized build, Nginx reverse proxy, caching

### Backend (Spring Boot)
- **Development**: Hot reload with Spring DevTools
- **Production**: Optimized JAR with health monitoring

### Database (PostgreSQL)
- Persistent data storage
- Automatic schema creation
- Health monitoring

## 🛠️ Development Commands

```bash
# Start development environment
docker-compose -f docker-compose.dev.yml up

# Rebuild specific service
docker-compose -f docker-compose.dev.yml up --build frontend

# View logs
docker-compose -f docker-compose.dev.yml logs -f backend

# Stop all services
docker-compose -f docker-compose.dev.yml down

# Stop and remove volumes
docker-compose -f docker-compose.dev.yml down -v
```

## 🚀 Production Commands

```bash
# Start production environment
docker-compose -f docker-compose.prod.yml up -d

# Scale services (if needed)
docker-compose -f docker-compose.prod.yml up -d --scale backend=2

# Update and restart
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# View production logs
docker-compose -f docker-compose.prod.yml logs -f
```

## 🔒 Security Notes

- Change default passwords in production
- Use environment variables for sensitive data
- Configure proper CORS origins
- Use HTTPS in production (add reverse proxy like Traefik)

## 📈 Monitoring

Health check endpoints:
- **Frontend**: `http://localhost/` (or `:5173` in dev)
- **Backend**: `http://localhost:8080/actuator/health`
- **Database**: Automatic health checks via Docker

## 🐛 Troubleshooting

### Common Issues

1. **Port conflicts**: Change ports in docker-compose files
2. **Build failures**: Clear Docker cache with `docker system prune`
3. **Database connection**: Ensure database is healthy before backend starts
4. **Hot reload not working**: Check volume mounts in dev configuration

### Logs
```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs backend

# Follow logs in real-time
docker-compose logs -f frontend
```