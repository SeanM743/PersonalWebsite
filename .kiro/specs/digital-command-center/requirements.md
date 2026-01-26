# Requirements Document

## Introduction

The Digital Command Center is a comprehensive personal life-tracking dashboard that transforms the existing website into Sean Mahoney's private "Digital Ledger." The system functions as a life-tracking command center capturing professional focus, media consumption, hobbies, milestones, and a chronological timeline of personal activities. The AI chat widget will be removed from the main interface (code preserved for future use).

## Glossary

- **Digital_Command_Center**: The main dashboard interface serving as a personal life-tracking hub
- **Now_Section**: High-visibility Bento-style card displaying real-time personal snapshot (Quick Facts)
- **Life_Log**: Unified polymorphic database tracking Books, Movies, TV Shows, Music, Albums, and Hobbies/Activities
- **Media_Entry**: A tracked item of type BOOK, MOVIE, SHOW, or ALBUM with metadata and ratings
- **Hobby_Entry**: A tracked activity or hobby with title, type, date range, and intensity level
- **Mahoney_Rating**: Personal 1-5 rating scale with accompanying "Key Takeaway" text field
- **Timeline_View**: Horizontal or vertical chronological visualization of Life Log entries
- **Digital_Garden**: Non-linear knowledge base with notes tagged by growth stage
- **Growth_Stage**: Classification for Digital Garden notes (Seedling, Budding, Evergreen)
- **Life_Signals**: Real-time milestone trackers including sports, countdowns, and family status
- **Bears_Tracker**: NFL API integration for Chicago Bears game tracking
- **Berkeley_Countdown**: Days remaining until Kanika's graduation (Summer 2026)
- **Family_Pulse**: Status tracker for family members (Madelyn, Evalyn, Nate)
- **Intensity_Level**: 1-5 scale indicating time/focus consumption for hobbies
- **Bento_Card**: High-visibility card component in the Bento grid layout style

## Requirements

### Requirement 1: The "Now" Section (Quick Facts Display)

**User Story:** As a user, I want to see a real-time snapshot of my current status on the homepage, so that I can quickly view my work focus, location, active hobby, and mood at a glance.

#### Acceptance Criteria

1. THE Digital_Command_Center SHALL display a high-visibility Bento-style card on the index page showing current status information
2. WHEN the Now_Section loads, THE Digital_Command_Center SHALL display Work Focus (e.g., "Amazon Prime"), Location (e.g., "Lower Queen Anne"), Active Hobby, and Current Mood data points
3. THE Digital_Command_Center SHALL render the Now_Section using a Bento grid layout with visually distinct cards for each data point
4. WHEN Quick Facts data is updated, THE Digital_Command_Center SHALL reflect changes in the Now_Section without requiring page refresh
5. THE Digital_Command_Center SHALL provide visual icons and color coding appropriate to each Quick Fact category

### Requirement 2: Unified Media & Activity Ledger (Life Log) Data Model

**User Story:** As a user, I want a unified system to track all my media consumption and hobbies, so that I can maintain a comprehensive life log of my activities and interests.

#### Acceptance Criteria

1. THE Life_Log SHALL support polymorphic entries with types: BOOK, MOVIE, SHOW, ALBUM, and HOBBY
2. WHEN creating a Life_Log entry, THE Digital_Command_Center SHALL store title, type, startDate, endDate, and optional metadata
3. THE Life_Log SHALL support an intensity field (1-5 scale) for HOBBY entries indicating time/focus consumption
4. THE Life_Log SHALL support the Mahoney_Rating system with a 1-5 scale and a "Key Takeaway" text field for all entry types
5. WHEN a Life_Log entry spans multiple dates, THE Digital_Command_Center SHALL track both startDate and endDate for timeline visualization
6. THE Life_Log SHALL allow multiple concurrent active entries to support overlapping hobbies and media consumption

### Requirement 3: Life Log Entry Management

**User Story:** As a user, I want to create, update, and manage my Life Log entries, so that I can track my media consumption and hobby activities over time.

#### Acceptance Criteria

1. WHEN adding a new Life_Log entry, THE Digital_Command_Center SHALL validate required fields (title, type) and optional fields (dates, rating, intensity)
2. THE Digital_Command_Center SHALL provide a form interface for creating and editing Life_Log entries with appropriate field types
3. WHEN a HOBBY entry is created, THE Digital_Command_Center SHALL require an intensity level (1-5) selection
4. THE Digital_Command_Center SHALL support updating entry status (in-progress, completed, planned) for tracking purposes
5. WHEN deleting a Life_Log entry, THE Digital_Command_Center SHALL confirm the action and remove the entry from all views including the Timeline

### Requirement 4: Timeline View (Hobby & Media Chronology)

**User Story:** As a user, I want to visualize my Life Log entries on a timeline, so that I can see my hobbies and media consumption chronologically over the calendar year.

#### Acceptance Criteria

1. THE Timeline_View SHALL display Life_Log entries as visual elements positioned according to their startDate and endDate
2. WHEN rendering the timeline, THE Digital_Command_Center SHALL use color-coding based on entry type (BOOK, MOVIE, SHOW, ALBUM, HOBBY)
3. THE Timeline_View SHALL support filtering by entry type to show specific categories (e.g., "Show me only Hobbies and Books")
4. WHEN multiple entries overlap in time, THE Timeline_View SHALL display them in parallel lanes or stacked format
5. THE Timeline_View SHALL support both horizontal and vertical orientation options
6. WHEN hovering or clicking a timeline entry, THE Digital_Command_Center SHALL display entry details including title, dates, rating, and key takeaway

### Requirement 5: Digital Garden (Knowledge Base)

**User Story:** As a user, I want to maintain a non-linear knowledge base with growth-stage tagged notes, so that I can track my learning and ideas as they develop.

#### Acceptance Criteria

1. THE Digital_Garden SHALL store notes with title, content, growth stage, and optional Life_Log links
2. WHEN creating a Digital_Garden note, THE Digital_Command_Center SHALL require a growth stage selection (Seedling, Budding, Evergreen)
3. THE Digital_Garden SHALL support linking notes directly to Life_Log entries (e.g., a "Seedling" note on AI Agents linked to "LLM Prototyping" hobby)
4. WHEN displaying the Digital_Garden, THE Digital_Command_Center SHALL visually distinguish notes by growth stage using icons or colors
5. THE Digital_Garden SHALL support searching and filtering notes by growth stage, linked entries, and content keywords

### Requirement 6: Social & Life Signals - Bears Tracker

**User Story:** As a user, I want to track Chicago Bears game information, so that I can stay updated on my favorite NFL team's schedule and results.

#### Acceptance Criteria

1. THE Digital_Command_Center SHALL integrate with an NFL API to fetch Chicago Bears game data
2. WHEN displaying Bears_Tracker, THE Digital_Command_Center SHALL show upcoming game date, opponent, and time
3. WHEN a game is completed, THE Bears_Tracker SHALL display the final score and win/loss result
4. THE Bears_Tracker SHALL update game information automatically during the NFL season
5. IF NFL API is unavailable, THE Digital_Command_Center SHALL display cached data with a staleness indicator

### Requirement 7: Social & Life Signals - Berkeley Countdown

**User Story:** As a user, I want to see a countdown to Kanika's graduation, so that I can track this important family milestone.

#### Acceptance Criteria

1. THE Digital_Command_Center SHALL display a countdown showing days remaining until Kanika's graduation (Summer 2026)
2. WHEN calculating the countdown, THE Digital_Command_Center SHALL use a configurable target date
3. THE Berkeley_Countdown SHALL display in a visually prominent card format consistent with the Bento layout
4. WHEN the graduation date passes, THE Digital_Command_Center SHALL display a celebration message or milestone achieved indicator

### Requirement 8: Social & Life Signals - Family Pulse

**User Story:** As a user, I want to track family member status updates, so that I can maintain awareness of important family activities and milestones.

#### Acceptance Criteria

1. THE Family_Pulse SHALL display status information for family members: Madelyn (WSU), Evalyn (Driving), and Nate (school/sports)
2. WHEN displaying Family_Pulse, THE Digital_Command_Center SHALL show each family member's current primary activity or status
3. THE Family_Pulse SHALL support manual updates to family member status through an edit interface
4. THE Digital_Command_Center SHALL display Family_Pulse in a card format consistent with the Bento layout
5. WHEN a family member status is updated, THE Digital_Command_Center SHALL reflect changes immediately

### Requirement 9: Dashboard Layout and Navigation

**User Story:** As a user, I want a clean, organized dashboard layout, so that I can easily navigate between different sections of my Digital Command Center.

#### Acceptance Criteria

1. THE Digital_Command_Center SHALL use a Bento-style grid layout for the main dashboard view
2. THE Digital_Command_Center SHALL remove the AI chat widget from the main page (code preserved but not displayed)
3. WHEN navigating the dashboard, THE Digital_Command_Center SHALL provide clear section headers and visual hierarchy
4. THE Digital_Command_Center SHALL be responsive and display appropriately on desktop and tablet devices
5. THE Digital_Command_Center SHALL use Tailwind CSS and shadcn/ui components for consistent styling

### Requirement 10: Life Log API Endpoints

**User Story:** As a frontend developer, I want API endpoints for Life Log operations, so that I can integrate the Life Log functionality into the dashboard interface.

#### Acceptance Criteria

1. THE Digital_Command_Center SHALL provide GET endpoints for retrieving Life_Log entries with filtering and pagination
2. THE Digital_Command_Center SHALL provide POST endpoints for creating new Life_Log entries
3. THE Digital_Command_Center SHALL provide PUT endpoints for updating existing Life_Log entries
4. THE Digital_Command_Center SHALL provide DELETE endpoints for removing Life_Log entries
5. WHEN API requests fail, THE Digital_Command_Center SHALL return appropriate HTTP status codes and error messages

### Requirement 11: Digital Garden API Endpoints

**User Story:** As a frontend developer, I want API endpoints for Digital Garden operations, so that I can integrate the knowledge base functionality into the dashboard interface.

#### Acceptance Criteria

1. THE Digital_Command_Center SHALL provide GET endpoints for retrieving Digital_Garden notes with filtering by growth stage
2. THE Digital_Command_Center SHALL provide POST endpoints for creating new Digital_Garden notes
3. THE Digital_Command_Center SHALL provide PUT endpoints for updating existing notes including growth stage changes
4. THE Digital_Command_Center SHALL provide DELETE endpoints for removing Digital_Garden notes
5. THE Digital_Command_Center SHALL provide endpoints for linking and unlinking notes to Life_Log entries

### Requirement 12: Timeline Component Implementation

**User Story:** As a frontend developer, I want a smooth, interactive timeline component, so that I can display the Life Log chronology with animations and user interactions.

#### Acceptance Criteria

1. THE Timeline_View SHALL use framer-motion for smooth transitions and animations
2. WHEN entries are filtered, THE Timeline_View SHALL animate the transition smoothly
3. THE Timeline_View SHALL support zoom and pan interactions for navigating different time ranges
4. WHEN rendering many entries, THE Timeline_View SHALL maintain performance through virtualization or lazy loading
5. THE Timeline_View SHALL provide visual indicators for the current date and time range being viewed
