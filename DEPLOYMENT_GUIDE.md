# Personal Dashboard - Deployment & Development Guide

## Table of Contents
- [Quick Development Setup](#quick-development-setup)
- [Production Deployment](#production-deployment)
- [Environment Configuration](#environment-configuration)
- [Troubleshooting](#troubleshooting)

---

## Quick Development Setup

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL 15+
- Maven 3.8+

### 1. Database Setup
```bash
# Start PostgreSQL database
docker run --name personal-dashboard-db \
  -e POSTGRES_DB=personal_platform \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  --rm postgres:15
```

### 2. Backend Setup
```bash
# Navigate to backend directory
cd backend

# Start the Spring Boot application
mvn spring-boot:run
```

The backend will be available at: http://localhost:8080

### 3. Frontend Setup
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

The frontend will be available at: http://localhost:5173

### 4. Login Credentials
- **Username**: `admin`
- **Password**: `default123`

### Development Notes
- Backend auto-reloads on Java file changes
- Frontend has hot-reload enabled
- Database data persists until container is removed
- No Docker required for development (direct process execution)

---

## Production Deployment

### Prerequisites
- Docker & Docker Compose
- Domain name (optional)
- SSL certificates (for HTTPS)

### 1. Environment Configuration

Create production environment files:

**`.env.prod`**:
```env
# Database
POSTGRES_DB=personal_platform
POSTGRES_USER=admin
POSTGRES_PASSWORD=your_secure_password_here

# API Keys
FINNHUB_API_KEY=your_finnhub_api_key
GOOGLE_CLOUD_PROJECT_ID=your_project_id
GOOGLE_CLOUD_LOCATION=us-central1

# Security
JWT_SECRET=your_jwt_secret_key_here

# External URLs (if different from localhost)
FRONTEND_URL=https://yourdomain.com
BACKEND_URL=https://api.yourdomain.com
```

### 2. Production Build & Deploy

```bash
# Build and start all services
docker-compose -f docker-compose.prod.yml up -d --build

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Stop services
docker-compose -f docker-compose.prod.yml down
```

### 3. Production Services

The production deployment includes:

- **PostgreSQL Database**: Port 5432 (internal)
- **Spring Boot Backend**: Port 8080 (internal)
- **React Frontend + Nginx**: Port 80/443 (external)

### 4. Production URLs

- **Frontend**: http://localhost (or your domain)
- **Backend API**: http://localhost/api (proxied through Nginx)
- **Health Check**: http://localhost/actuator/health

### 5. SSL/HTTPS Setup (Optional)

To enable HTTPS in production:

1. Obtain SSL certificates (Let's Encrypt recommended)
2. Update `frontend/nginx.conf` with SSL configuration
3. Update `docker-compose.prod.yml` to mount certificates
4. Update environment variables to use HTTPS URLs

---

## Environment Configuration

### Required API Keys

1. **Finnhub API** (Stock Data):
   - Sign up at https://finnhub.io/
   - Get free API key
   - Set `FINNHUB_API_KEY` environment variable

2. **Google Cloud** (AI & Calendar):
   - Create project at https://console.cloud.google.com/
   - Enable Vertex AI API
   - Set `GOOGLE_CLOUD_PROJECT_ID` and `GOOGLE_CLOUD_LOCATION`
   - Place service account JSON at: `~/.config/google/google-calendar-credentials.json`

### Optional Configuration

- **TMDB API**: For enhanced movie/TV data
- **Custom JWT Secret**: For production security
- **Database Credentials**: Change default passwords

---

## Troubleshooting

### Common Issues

#### Backend Won't Start
```bash
# Check if port 8080 is in use
lsof -i :8080

# Kill process using port
kill -9 <PID>
```

#### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check database logs
docker logs personal-dashboard-db
```

#### Frontend Build Issues
```bash
# Clear node modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

#### Google Credentials Not Found
```bash
# Check file exists and permissions
ls -la ~/.config/google/google-calendar-credentials.json

# Verify file format (should be valid JSON)
cat ~/.config/google/google-calendar-credentials.json | jq .
```

### Performance Optimization

#### Production Optimizations
- Enable Nginx gzip compression
- Configure database connection pooling
- Set up Redis for caching (optional)
- Configure log rotation

#### Development Optimizations
- Increase JVM heap size: `export MAVEN_OPTS="-Xmx2g"`
- Use development profiles: `spring.profiles.active=dev`

---

## Monitoring & Maintenance

### Health Checks
- **Backend**: http://localhost:8080/actuator/health
- **Database**: Check connection in backend logs
- **Frontend**: Verify page loads correctly

### Log Locations
- **Backend**: Console output or configured log files
- **Frontend**: Browser developer console
- **Database**: Docker container logs
- **Nginx**: `/var/log/nginx/` (in container)

### Backup Strategy
```bash
# Database backup
docker exec personal-dashboard-db pg_dump -U admin personal_platform > backup.sql

# Database restore
docker exec -i personal-dashboard-db psql -U admin personal_platform < backup.sql
```

---

## Security Considerations

### Production Security Checklist
- [ ] Change default database passwords
- [ ] Use strong JWT secret
- [ ] Enable HTTPS/SSL
- [ ] Configure firewall rules
- [ ] Regular security updates
- [ ] Backup encryption
- [ ] API rate limiting (if needed)

### Development Security
- [ ] Don't commit API keys to version control
- [ ] Use `.env` files for sensitive data
- [ ] Keep dependencies updated
- [ ] Use HTTPS for external API calls

---

## Support & Updates

### Updating the Application
```bash
# Pull latest changes
git pull origin main

# Rebuild and restart (production)
docker-compose -f docker-compose.prod.yml up -d --build

# Restart development servers
# Backend: Ctrl+C and mvn spring-boot:run
# Frontend: Ctrl+C and npm run dev
```

### Getting Help
- Check application logs first
- Verify all environment variables are set
- Ensure all required services are running
- Check network connectivity for external APIs