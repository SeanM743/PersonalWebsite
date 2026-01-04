# Personal Dashboard Frontend

A modern React TypeScript frontend for the Personal Dashboard application.

## Features

- **Authentication**: JWT-based login system
- **Portfolio Management**: Real-time stock portfolio tracking with Finnhub integration
- **Calendar Integration**: Google Calendar events display and management
- **Content Management**: Social media-style posts, media tracking, and quick facts
- **AI Chat**: Gemini-powered chat assistant for natural language queries
- **Responsive Design**: Mobile-first design with Tailwind CSS

## Tech Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **React Router** for navigation
- **Axios** for API communication
- **Lucide React** for icons
- **React Hook Form** for form handling

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on `http://localhost:8080`

### Installation

1. Install dependencies:
```bash
npm install
```

2. Create environment file:
```bash
cp .env.example .env
```

3. Update `.env` with your configuration:
```env
VITE_API_BASE_URL=http://localhost:8080
```

### Development

Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173`

### Building

Build for production:
```bash
npm run build
```

## Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── Layout/         # Layout components (Header, Sidebar)
│   └── UI/             # Basic UI components (LoadingSpinner, etc.)
├── contexts/           # React contexts (Auth, Notifications)
├── pages/              # Page components
├── services/           # API services
├── App.tsx             # Main app component
├── main.tsx           # Entry point
└── index.css          # Global styles
```

## API Integration

The frontend integrates with the following backend APIs:

- **Authentication**: `/api/auth/*`
- **Portfolio**: `/api/portfolio/*`
- **Calendar**: `/api/calendar/*`
- **Content**: `/api/content/*`
- **Chat**: `/api/chat/*`

## Authentication

The app uses JWT tokens stored in localStorage. Users can log in with:
- Username: `admin`
- Password: `password`

## Features Overview

### Dashboard
- Portfolio overview with real-time data
- Upcoming calendar events
- Recent posts and content
- Quick facts display
- Quick action buttons

### Portfolio
- Real-time stock price tracking
- Portfolio performance metrics
- Add/edit/delete stock holdings
- Gain/loss calculations
- Market data refresh

### Calendar
- Google Calendar integration
- Event display by date
- Upcoming events list
- Event creation (UI ready)

### Content
- Social media-style posts
- Media activity tracking (books, movies, music, trips)
- Quick facts management
- Tabbed interface

### Chat
- AI-powered chat assistant
- Natural language queries about portfolio and calendar
- Chat history
- Real-time responses

## Styling

The app uses Tailwind CSS with custom components defined in `index.css`:

- `.btn-primary`, `.btn-secondary`, `.btn-danger` - Button styles
- `.card` - Card container style
- `.input-field` - Form input style
- `.sidebar-item` - Navigation item style

## Error Handling

- Global error handling with notification system
- API error interceptors
- User-friendly error messages
- Loading states for all async operations

## Responsive Design

- Mobile-first approach
- Responsive navigation with mobile sidebar
- Adaptive layouts for different screen sizes
- Touch-friendly interface elements