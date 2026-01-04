package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.TripRequest;
import com.personal.backend.dto.TripResponse;
import com.personal.backend.dto.ValidationResult;
import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
import com.personal.backend.model.UpcomingTrip;
import com.personal.backend.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    
    private final TripRepository tripRepository;
    private final ContentValidationService validationService;
    
    @Transactional
    public ContentResponse<TripResponse> createTrip(TripRequest request) {
        log.debug("Creating trip to: {}", request.getDestination());
        
        // Validate the request
        ValidationResult validation = validationService.validateTrip(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        try {
            // Check for date conflicts if dates are provided
            if (request.getStartDate() != null && request.getEndDate() != null) {
                List<UpcomingTrip> conflicts = tripRepository.findConflictingTrips(
                        request.getStartDate(), request.getEndDate(), -1L);
                
                if (!conflicts.isEmpty()) {
                    log.warn("Date conflict detected for trip to: {} ({} - {})", 
                            request.getDestination(), request.getStartDate(), request.getEndDate());
                    // Continue anyway but log the warning
                }
            }
            
            UpcomingTrip trip = UpcomingTrip.builder()
                    .destination(validationService.sanitizeInput(request.getDestination()))
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .tripType(request.getTripType() != null ? request.getTripType() : TripType.VACATION)
                    .description(validationService.sanitizeInput(request.getDescription()))
                    .plannedActivities(validationService.sanitizeInput(request.getPlannedActivities()))
                    .status(request.getStatus() != null ? request.getStatus() : TripStatus.PLANNED)
                    .build();
            
            UpcomingTrip savedTrip = tripRepository.save(trip);
            
            log.info("Created trip with ID: {} - {} ({} - {})", 
                    savedTrip.getId(), savedTrip.getDestination(), 
                    savedTrip.getStartDate(), savedTrip.getEndDate());
            
            return ContentResponse.success(
                    mapToResponse(savedTrip),
                    "Trip created successfully"
            );
            
        } catch (Exception e) {
            log.error("Error creating trip", e);
            return ContentResponse.error("Failed to create trip: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<TripResponse> updateTrip(Long id, TripRequest request) {
        log.debug("Updating trip with ID: {}", id);
        
        // Validate the request
        ValidationResult validation = validationService.validateTrip(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        Optional<UpcomingTrip> existingTrip = tripRepository.findById(id);
        if (existingTrip.isEmpty()) {
            return ContentResponse.error("Trip not found with ID: " + id);
        }
        
        try {
            UpcomingTrip trip = existingTrip.get();
            
            // Check for date conflicts if dates are changing
            if (request.getStartDate() != null && request.getEndDate() != null) {
                List<UpcomingTrip> conflicts = tripRepository.findConflictingTrips(
                        request.getStartDate(), request.getEndDate(), id);
                
                if (!conflicts.isEmpty()) {
                    log.warn("Date conflict detected for updated trip to: {} ({} - {})", 
                            request.getDestination(), request.getStartDate(), request.getEndDate());
                    // Continue anyway but log the warning
                }
            }
            
            // Update fields
            trip.setDestination(validationService.sanitizeInput(request.getDestination()));
            trip.setStartDate(request.getStartDate());
            trip.setEndDate(request.getEndDate());
            trip.setTripType(request.getTripType());
            trip.setDescription(validationService.sanitizeInput(request.getDescription()));
            trip.setPlannedActivities(validationService.sanitizeInput(request.getPlannedActivities()));
            trip.setStatus(request.getStatus());
            
            UpcomingTrip updatedTrip = tripRepository.save(trip);
            
            log.info("Updated trip with ID: {} - {}", updatedTrip.getId(), updatedTrip.getDestination());
            
            return ContentResponse.success(
                    mapToResponse(updatedTrip),
                    "Trip updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating trip", e);
            return ContentResponse.error("Failed to update trip: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<TripResponse> updateTripStatus(Long id, TripStatus newStatus) {
        log.debug("Updating trip status for ID: {} to {}", id, newStatus);
        
        Optional<UpcomingTrip> existingTrip = tripRepository.findById(id);
        if (existingTrip.isEmpty()) {
            return ContentResponse.error("Trip not found with ID: " + id);
        }
        
        try {
            UpcomingTrip trip = existingTrip.get();
            TripStatus oldStatus = trip.getStatus();
            trip.setStatus(newStatus);
            
            UpcomingTrip updatedTrip = tripRepository.save(trip);
            
            log.info("Updated trip status from {} to {} for: {}", oldStatus, newStatus, trip.getDestination());
            
            return ContentResponse.success(
                    mapToResponse(updatedTrip),
                    "Trip status updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating trip status", e);
            return ContentResponse.error("Failed to update trip status: " + e.getMessage());
        }
    }
    
    public ContentResponse<TripResponse> getTrip(Long id) {
        Optional<UpcomingTrip> trip = tripRepository.findById(id);
        
        if (trip.isEmpty()) {
            return ContentResponse.error("Trip not found with ID: " + id);
        }
        
        return ContentResponse.success(mapToResponse(trip.get()));
    }
    
    public ContentResponse<List<TripResponse>> getAllTrips() {
        try {
            List<UpcomingTrip> trips = tripRepository.findAll();
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving all trips", e);
            return ContentResponse.error("Failed to retrieve trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getTripsByStatus(TripStatus status) {
        try {
            List<UpcomingTrip> trips = tripRepository.findByStatusOrderByStartDateAsc(status);
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving trips by status: {}", status, e);
            return ContentResponse.error("Failed to retrieve trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getTripsByType(TripType tripType) {
        try {
            List<UpcomingTrip> trips = tripRepository.findByTripTypeOrderByStartDateAsc(tripType);
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving trips by type: {}", tripType, e);
            return ContentResponse.error("Failed to retrieve trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getUpcomingTrips() {
        try {
            List<UpcomingTrip> trips = tripRepository.findUpcomingTrips(LocalDate.now());
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving upcoming trips", e);
            return ContentResponse.error("Failed to retrieve upcoming trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getCurrentTrips() {
        try {
            List<UpcomingTrip> trips = tripRepository.findCurrentTrips(LocalDate.now());
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving current trips", e);
            return ContentResponse.error("Failed to retrieve current trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getPastTrips() {
        try {
            List<UpcomingTrip> trips = tripRepository.findPastTrips(LocalDate.now());
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving past trips", e);
            return ContentResponse.error("Failed to retrieve past trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> searchTrips(String searchTerm) {
        try {
            List<UpcomingTrip> trips = tripRepository.searchTrips(searchTerm);
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error searching trips", e);
            return ContentResponse.error("Failed to search trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getTripsByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            List<UpcomingTrip> trips = tripRepository.findByStartDateBetweenOrderByStartDateAsc(startDate, endDate);
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving trips by date range", e);
            return ContentResponse.error("Failed to retrieve trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> checkDateConflicts(LocalDate startDate, LocalDate endDate, Long excludeId) {
        try {
            Long excludeIdSafe = excludeId != null ? excludeId : -1L;
            List<UpcomingTrip> conflicts = tripRepository.findConflictingTrips(startDate, endDate, excludeIdSafe);
            List<TripResponse> responses = conflicts.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error checking date conflicts", e);
            return ContentResponse.error("Failed to check conflicts: " + e.getMessage());
        }
    }
    
    public ContentResponse<Page<TripResponse>> getTripsPaginated(Pageable pageable) {
        try {
            Page<UpcomingTrip> trips = tripRepository.findAll(pageable);
            Page<TripResponse> responses = trips.map(this::mapToResponse);
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving paginated trips", e);
            return ContentResponse.error("Failed to retrieve trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<TripResponse>> getRecentTrips(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<UpcomingTrip> trips = tripRepository.findRecentTrips(pageable);
            List<TripResponse> responses = trips.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving recent trips", e);
            return ContentResponse.error("Failed to retrieve recent trips: " + e.getMessage());
        }
    }
    
    public ContentResponse<Map<String, Object>> getTripStatistics() {
        try {
            long totalTrips = tripRepository.count();
            
            List<Object[]> statusStats = tripRepository.getTripCountByStatus();
            Map<String, Long> statusCountMap = statusStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((TripStatus) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            List<Object[]> typeStats = tripRepository.getTripCountByType();
            Map<String, Long> typeCountMap = typeStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((TripType) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            Map<String, Object> stats = Map.of(
                    "totalTrips", totalTrips,
                    "byStatus", statusCountMap,
                    "byType", typeCountMap,
                    "upcoming", tripRepository.countByStatus(TripStatus.PLANNED) + tripRepository.countByStatus(TripStatus.CONFIRMED),
                    "completed", tripRepository.countByStatus(TripStatus.COMPLETED)
            );
            
            return ContentResponse.success(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving trip statistics", e);
            return ContentResponse.error("Failed to retrieve statistics: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteTrip(Long id) {
        Optional<UpcomingTrip> trip = tripRepository.findById(id);
        
        if (trip.isEmpty()) {
            return ContentResponse.error("Trip not found with ID: " + id);
        }
        
        try {
            tripRepository.deleteById(id);
            
            log.info("Deleted trip with ID: {} - {}", id, trip.get().getDestination());
            return ContentResponse.success(null, "Trip deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting trip", e);
            return ContentResponse.error("Failed to delete trip: " + e.getMessage());
        }
    }
    
    private TripResponse mapToResponse(UpcomingTrip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .destination(trip.getDestination())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .tripType(trip.getTripType())
                .description(trip.getDescription())
                .plannedActivities(trip.getPlannedActivities())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}