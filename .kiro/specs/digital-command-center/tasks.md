# Implementation Plan: Digital Command Center

## Overview

This implementation plan transforms the existing dashboard into Sean Mahoney's Digital Command Center - a comprehensive life-tracking system with Bento-style layout, Life Log, Timeline View, Digital Garden, and Life Signals.

## Tasks

- [ ] 1. Create Life Log Backend Infrastructure
  - [x] 1.1 Create LifeLogEntry entity with polymorphic type support
    - Create entity class with id, title, type (BOOK, MOVIE, SHOW, ALBUM, HOBBY), startDate, endDate, status, rating, keyTakeaway, intensity, imageUrl, metadata fields
    - Create LifeLogType and EntryStatus enums
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 1.2 Create LifeLogRepository with custom query methods
    - Implement JpaRepository with filtering by type, status, and date range
    - Add query for active entries (no endDate or endDate >= today)
    - _Requirements: 2.6, 10.1_

  - [x] 1.3 Create LifeLogService with CRUD and validation logic
    - Implement create, read, update, delete operations
    - Add validation: HOBBY requires intensity 1-5, all entries require title and type
    - Add filtering and pagination support
    - _Requirements: 3.1, 3.3, 3.4, 3.5_

  - [ ]* 1.4 Write property test for Life Log entry round-trip
    - **Property 1: Life Log Entry Round-Trip**
    - **Validates: Requirements 2.2, 2.3, 2.4, 2.5, 10.2, 10.3**

  - [ ]* 1.5 Write property test for Life Log type-specific validation
    - **Property 2: Life Log Type-Specific Validation**
    - **Validates: Requirements 3.1, 3.3**

- [ ] 2. Create Life Log API Endpoints
  - [x] 2.1 Create LifeLogController with REST endpoints
    - GET /api/lifelog - list with filters and pagination
    - GET /api/lifelog/{id} - get by ID
    - POST /api/lifelog - create entry
    - PUT /api/lifelog/{id} - update entry
    - DELETE /api/lifelog/{id} - delete entry
    - GET /api/lifelog/timeline - get entries for timeline view
    - GET /api/lifelog/active - get currently active entries
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 2.2 Create LifeLogEntryRequest and LifeLogEntryResponse DTOs
    - Request DTO with validation annotations
    - Response DTO with all fields including timestamps
    - _Requirements: 10.1, 10.2_

  - [ ]* 2.3 Write property test for Life Log filtering correctness
    - **Property 3: Life Log Filtering Correctness**
    - **Validates: Requirements 4.3, 10.1**

  - [ ]* 2.4 Write property test for Life Log deletion completeness
    - **Property 9: Life Log Deletion Completeness**
    - **Validates: Requirements 3.5, 10.4**

- [x] 3. Checkpoint - Ensure Life Log backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Create Digital Garden Backend Infrastructure
  - [x] 4.1 Create GardenNote entity with growth stage support
    - Create entity with id, title, content, growthStage, linkedEntries, timestamps
    - Create GrowthStage enum (SEEDLING, BUDDING, EVERGREEN)
    - Create ManyToMany relationship with LifeLogEntry
    - _Requirements: 5.1, 5.2_

  - [x] 4.2 Create GardenRepository with filtering support
    - Implement JpaRepository with filtering by growth stage
    - Add query for notes linked to specific LifeLog entry
    - _Requirements: 5.5, 11.1_

  - [x] 4.3 Create GardenService with CRUD and linking logic
    - Implement create, read, update, delete operations
    - Add link/unlink operations for LifeLog entries
    - Add validation: growth stage required
    - _Requirements: 5.2, 5.3_

  - [x] 4.4 Create GardenController with REST endpoints
    - GET /api/garden - list with filters
    - GET /api/garden/{id} - get by ID
    - POST /api/garden - create note
    - PUT /api/garden/{id} - update note
    - DELETE /api/garden/{id} - delete note
    - POST /api/garden/{id}/link/{lifelogId} - link to LifeLog
    - DELETE /api/garden/{id}/link/{lifelogId} - unlink from LifeLog
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [ ]* 4.5 Write property test for Digital Garden note round-trip with linking
    - **Property 5: Digital Garden Note Round-Trip with Linking**
    - **Validates: Requirements 5.1, 5.3, 11.2, 11.5**

  - [ ]* 4.6 Write property test for Digital Garden filtering correctness
    - **Property 6: Digital Garden Filtering Correctness**
    - **Validates: Requirements 5.5, 11.1**

- [ ] 5. Create Life Signals Backend Infrastructure
  - [x] 5.1 Create FamilyMember entity and repository
    - Create entity with id, name, primaryActivity, status, notes, updatedAt
    - Implement JpaRepository
    - _Requirements: 8.1, 8.2_

  - [x] 5.2 Create SignalsService for Life Signals data
    - Implement Berkeley countdown calculation (target: Summer 2026)
    - Implement Family Pulse CRUD operations
    - Add placeholder for Bears tracker (NFL API integration)
    - _Requirements: 6.1, 7.1, 7.2, 8.3_

  - [x] 5.3 Create SignalsController with REST endpoints
    - GET /api/signals/bears - get Bears game info
    - GET /api/signals/countdown - get Berkeley countdown
    - GET /api/signals/family - get family pulse data
    - PUT /api/signals/family/{id} - update family member
    - _Requirements: 6.2, 7.1, 8.1_

  - [ ]* 5.4 Write property test for countdown calculation correctness
    - **Property 7: Countdown Calculation Correctness**
    - **Validates: Requirements 7.1, 7.2**

  - [ ]* 5.5 Write property test for Family Pulse update persistence
    - **Property 8: Family Pulse Update Persistence**
    - **Validates: Requirements 8.3, 8.5**

- [x] 6. Checkpoint - Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Create Frontend Dashboard Layout
  - [x] 7.1 Refactor Dashboard.tsx to Bento grid layout
    - Remove AI chat widget from main page (preserve code)
    - Create Bento-style grid layout with CSS Grid
    - Add section placeholders for Now, LifeLog, Timeline, Garden, Signals
    - _Requirements: 9.1, 9.2, 9.3_

  - [x] 7.2 Create NowSection component (Quick Facts Bento cards)
    - Display Work Focus, Location, Active Hobby, Current Mood
    - Use existing QuickFacts data with Bento card styling
    - Add icons and color coding per category
    - _Requirements: 1.1, 1.2, 1.3, 1.5_

  - [x] 7.3 Create apiService methods for new endpoints
    - Add Life Log CRUD methods
    - Add Digital Garden CRUD methods
    - Add Life Signals methods
    - _Requirements: 10.1, 11.1_

- [ ] 8. Create Life Log Frontend Components
  - [x] 8.1 Create LifeLogView component
    - Display list of Life Log entries with type icons and colors
    - Show Mahoney Rating and Key Takeaway
    - Add filter controls for type and status
    - _Requirements: 2.1, 2.4_

  - [x] 8.2 Create LifeLogEntryForm component
    - Form for creating/editing entries
    - Dynamic fields based on type (intensity for HOBBY)
    - Rating selector (1-5) and Key Takeaway input
    - _Requirements: 3.2, 3.3_

  - [x] 8.3 Create LifeLogEntryCard component
    - Display entry details in card format
    - Show type badge, dates, rating, intensity
    - Edit and delete actions
    - _Requirements: 2.1, 3.4, 3.5_

- [ ] 9. Create Timeline Frontend Component
  - [x] 9.1 Create Timeline component with framer-motion
    - Horizontal/vertical timeline layout
    - Position entries based on startDate/endDate
    - Color-code by entry type
    - _Requirements: 4.1, 4.2, 4.5, 12.1_

  - [x] 9.2 Implement timeline lane assignment algorithm
    - Assign overlapping entries to different lanes
    - Optimize lane usage for visual clarity
    - _Requirements: 4.4_

  - [x] 9.3 Add timeline filtering and interactions
    - Filter controls by entry type
    - Click/hover to show entry details
    - Smooth filter transitions with framer-motion
    - _Requirements: 4.3, 4.6, 12.2_

  - [ ]* 9.4 Write property test for timeline lane assignment
    - **Property 4: Timeline Lane Assignment for Overlapping Entries**
    - **Validates: Requirements 4.1, 4.4**

  - [ ]* 9.5 Write property test for entry type color consistency
    - **Property 10: Entry Type Color Consistency**
    - **Validates: Requirements 4.2**

- [ ] 10. Create Digital Garden Frontend Components
  - [x] 10.1 Create DigitalGardenView component
    - Display notes with growth stage icons (🌱 Seedling, 🌿 Budding, 🌲 Evergreen)
    - Filter by growth stage
    - Show linked Life Log entries
    - _Requirements: 5.4, 5.5_

  - [x] 10.2 Create GardenNoteForm component
    - Form for creating/editing notes
    - Growth stage selector
    - Life Log entry linking interface
    - _Requirements: 5.2, 5.3_

  - [x] 10.3 Create GardenNoteCard component
    - Display note content with growth stage badge
    - Show linked entries as chips
    - Edit and delete actions
    - _Requirements: 5.1, 5.4_

- [ ] 11. Create Life Signals Frontend Components
  - [x] 11.1 Create LifeSignalsPanel component
    - Container for all Life Signals cards
    - Bento-style layout
    - _Requirements: 9.1_

  - [x] 11.2 Create BearsTracker component
    - Display next game info (opponent, date, time)
    - Show last game result if available
    - Handle API unavailable state with cached data indicator
    - _Requirements: 6.2, 6.3, 6.5_

  - [x] 11.3 Create BerkeleyCountdown component
    - Display days remaining until Summer 2026
    - Show celebration message when date passes
    - Prominent visual styling
    - _Requirements: 7.1, 7.3, 7.4_

  - [x] 11.4 Create FamilyPulse component
    - Display family members with status
    - Edit interface for updating status
    - Card format for each family member
    - _Requirements: 8.1, 8.2, 8.4_

- [x] 12. Checkpoint - Ensure all frontend components render correctly
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Integration and Data Initialization
  - [x] 13.1 Create DataInitializer for Life Signals seed data
    - Initialize Family Pulse with Madelyn (WSU), Evalyn (Driving), Nate (school/sports)
    - Set Berkeley countdown target date (Summer 2026)
    - _Requirements: 7.2, 8.1_

  - [x] 13.2 Wire all components together in Dashboard
    - Integrate NowSection, LifeLogView, Timeline, DigitalGarden, LifeSignals
    - Add responsive layout adjustments
    - Ensure data flows correctly between components
    - _Requirements: 9.4_

  - [x] 13.3 Add error handling and loading states
    - Toast notifications for API errors
    - Loading spinners during data fetches
    - Graceful degradation for optional features
    - _Requirements: 10.5_

- [x] 14. Final Checkpoint - Full system integration test
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using jqwik (Java) and fast-check (TypeScript)
- Unit tests validate specific examples and edge cases
- The AI chat widget code is preserved but removed from the main dashboard display
