# Implementation Plan: Authentication System

## Overview

This implementation plan converts the authentication system design into discrete coding tasks for a Spring Boot application. The tasks build incrementally from basic project setup through complete JWT authentication with Spring Security integration. Each task focuses on specific components while maintaining integration with the existing project structure.

## Tasks

- [x] 1. Set up authentication dependencies and configuration
  - Add Spring Security, JWT, and BCrypt dependencies to backend/pom.xml
  - Configure PostgreSQL database properties in application.properties
  - Create basic security configuration class structure
  - _Requirements: 4.5, 6.1_

- [ ] 2. Create core data models and entities
  - [x] 2.1 Create User JPA entity with username, passwordHash, and role fields
    - Implement User entity with proper JPA annotations
    - Create Role enum with GUEST and ADMIN values
    - Add database constraints and validation annotations
    - _Requirements: 4.1, 4.2_

  - [ ] 2.2 Write property test for User entity persistence
    - **Property 10: Username uniqueness enforcement**
    - **Validates: Requirements 4.2**

  - [x] 2.3 Create UserRepository interface extending JpaRepository
    - Define findByUsername method for authentication
    - Add any custom query methods needed
    - _Requirements: 4.1_

- [ ] 3. Implement JWT utility and token management
  - [x] 3.1 Create JwtUtil class for token operations
    - Implement token generation with expiration and signing
    - Add token validation and parsing methods
    - Include role information in JWT claims
    - _Requirements: 2.5, 3.1_

  - [ ] 3.2 Write property test for JWT token structure
    - **Property 8: JWT token structure**
    - **Validates: Requirements 2.5, 3.1**

  - [ ] 3.3 Write property test for token validation consistency
    - **Property 7: Token validation consistency**
    - **Validates: Requirements 2.3, 2.4, 3.3**

- [ ] 4. Create authentication service layer
  - [x] 4.1 Implement AuthenticationService class
    - Add user credential validation logic
    - Integrate password encoding with BCrypt
    - Handle authentication success and failure cases
    - _Requirements: 1.1, 1.2, 1.4_

  - [ ] 4.2 Write property test for password hashing security
    - **Property 4: Password hashing security**
    - **Validates: Requirements 1.4, 1.5, 4.3**

  - [x] 4.3 Create UserDetailsServiceImpl for Spring Security integration
    - Implement UserDetailsService interface
    - Load user details from UserRepository
    - Map User entity to Spring Security UserDetails
    - _Requirements: 6.3_

- [ ] 5. Implement authentication DTOs and controllers
  - [x] 5.1 Create LoginRequest and AuthResponse DTOs
    - Define request/response data structures
    - Add validation annotations for input validation
    - _Requirements: 5.1, 5.5_

  - [x] 5.2 Create AuthenticationController with login endpoint
    - Implement POST /api/auth/login endpoint
    - Handle authentication success and failure responses
    - Add proper HTTP status codes and error handling
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 5.3 Add /me endpoint for authentication persistence
    - Implement GET /api/auth/me endpoint
    - Extract username from JWT token and return user information
    - Handle invalid/missing tokens with 401 responses
    - _Requirements: 7.2, 7.3, 7.4_

  - [ ] 5.4 Write property test for valid credential authentication
    - **Property 1: Valid credential authentication**
    - **Validates: Requirements 1.1, 5.2**

  - [ ] 5.5 Write property test for invalid credential rejection
    - **Property 2: Invalid credential rejection**
    - **Validates: Requirements 1.2, 5.3**

  - [ ] 5.6 Write property test for malformed request validation
    - **Property 3: Malformed request validation**
    - **Validates: Requirements 1.3, 5.4**

  - [ ] 5.7 Write property test for token-based user information retrieval
    - **Property 5: Token-based user information retrieval**
    - **Validates: Requirements 7.2, 7.3**

  - [ ] 5.8 Write property test for invalid token rejection for user info
    - **Property 6: Invalid token rejection for user info**
    - **Validates: Requirements 7.4**

- [ ] 6. Checkpoint - Basic authentication functionality complete
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement JWT authentication filter
  - [x] 7.1 Create JwtAuthenticationFilter extending OncePerRequestFilter
    - Extract JWT tokens from Authorization header
    - Validate tokens and set Spring Security authentication context
    - Handle filter exceptions and invalid tokens
    - _Requirements: 2.3, 2.4, 6.3_

  - [ ] 7.2 Write property test for authentication context availability
    - **Property 12: Authentication context availability**
    - **Validates: Requirements 6.3**

- [ ] 8. Configure Spring Security filter chain
  - [x] 8.1 Complete SecurityConfig class with filter chain configuration
    - Configure JWT authentication filter in security chain
    - Define public and protected endpoint patterns
    - Set up CORS and CSRF policies
    - Configure password encoder bean
    - _Requirements: 6.1, 6.2_

  - [ ] 8.2 Write property test for guest user access control
    - **Property 8: Guest user access control**
    - **Validates: Requirements 2.1, 6.2**

  - [ ] 8.3 Write property test for admin user access control
    - **Property 9: Admin user access control**
    - **Validates: Requirements 2.2, 6.2**

- [ ] 9. Add comprehensive error handling and validation
  - [ ] 9.1 Implement global exception handler for authentication errors
    - Handle authentication failures with proper HTTP status codes
    - Add validation error responses for malformed requests
    - Include security logging for authentication attempts
    - _Requirements: 1.2, 1.3, 8.1, 8.2, 8.5_

  - [ ] 9.2 Write property test for API format consistency
    - **Property 12: API format consistency**
    - **Validates: Requirements 5.1, 5.5**

  - [ ] 9.3 Write property test for password hash confidentiality
    - **Property 14: Password hash confidentiality**
    - **Validates: Requirements 4.4**

- [ ] 10. Frontend integration for authentication persistence
  - [x] 10.1 Verify frontend AuthContext integration
    - Ensure frontend calls /api/auth/me on app initialization
    - Verify token storage and retrieval from localStorage
    - Test authentication state persistence across page refreshes
    - _Requirements: 7.1, 7.5_

  - [ ] 10.2 Write property test for authentication state persistence
    - **Property 7: Authentication state persistence**
    - **Validates: Requirements 7.1, 7.5**

- [ ] 11. Integration and database setup
  - [x] 11.1 Create database initialization and default user setup
    - Add data.sql or configuration for default admin user
    - Ensure PostgreSQL database persistence is working correctly
    - Test database connectivity and schema creation
    - _Requirements: 4.1, 4.5_

  - [x] 11.2 Write integration tests for complete authentication flow
    - Test end-to-end login process from HTTP request to JWT response
    - Verify database persistence and PostgreSQL storage
    - Test Spring Security filter chain integration

- [x] 12. Final checkpoint - Complete authentication system
  - Ensure all tests pass, ask the user if questions arise.
  - Verify JWT authentication works with protected endpoints
  - Test guest vs admin access differentiation
  - Verify authentication persistence across page refreshes

## Notes

- All tasks are required for comprehensive authentication system implementation
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties from the design
- Unit tests validate specific examples and integration points
- The implementation builds incrementally with checkpoints for validation