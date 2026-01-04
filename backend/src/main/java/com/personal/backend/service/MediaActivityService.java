package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.MediaActivityRequest;
import com.personal.backend.dto.MediaActivityResponse;
import com.personal.backend.dto.ValidationResult;
import com.personal.backend.external.model.BookMetadata;
import com.personal.backend.external.model.MovieMetadata;
import com.personal.backend.external.model.TVShowMetadata;
import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaActivity;
import com.personal.backend.model.MediaType;
import com.personal.backend.repository.MediaActivityRepository;
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
public class MediaActivityService {
    
    private final MediaActivityRepository mediaActivityRepository;
    private final ContentValidationService validationService;
    private final MetadataFetcherService metadataFetcher;
    
    @Transactional
    public ContentResponse<MediaActivityResponse> createMediaActivity(MediaActivityRequest request) {
        log.debug("Creating media activity: {} - {}", request.getMediaType(), request.getTitle());
        
        // Validate the request
        ValidationResult validation = validationService.validateMediaActivity(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        try {
            // Check for potential duplicates
            List<MediaActivity> duplicates = mediaActivityRepository.findPotentialDuplicates(
                    request.getTitle(), request.getCreator(), request.getMediaType());
            
            if (!duplicates.isEmpty()) {
                log.warn("Potential duplicate found for: {} by {}", request.getTitle(), request.getCreator());
                // Continue anyway but log the warning
            }
            
            // Try to fetch metadata from external APIs
            MediaActivity.MediaActivityBuilder activityBuilder = MediaActivity.builder()
                    .title(validationService.sanitizeInput(request.getTitle()))
                    .mediaType(request.getMediaType())
                    .status(request.getStatus() != null ? request.getStatus() : ActivityStatus.PLANNED)
                    .creator(validationService.sanitizeInput(request.getCreator()))
                    .rating(request.getRating())
                    .startDate(request.getStartDate())
                    .completionDate(request.getCompletionDate())
                    .notes(validationService.sanitizeInput(request.getNotes()))
                    .externalId(request.getExternalId());
            
            // Fetch metadata if possible
            Optional<Object> metadata = metadataFetcher.fetchMetadataByType(
                    request.getTitle(), request.getMediaType(), request.getCreator(), request.getExternalId());
            
            if (metadata.isPresent()) {
                enrichActivityWithMetadata(activityBuilder, metadata.get(), request.getMediaType());
                log.info("Enriched activity with external metadata: {}", request.getTitle());
            } else {
                log.info("No external metadata found for: {} ({})", request.getTitle(), request.getMediaType());
            }
            
            MediaActivity activity = activityBuilder.build();
            MediaActivity savedActivity = mediaActivityRepository.save(activity);
            
            log.info("Created media activity with ID: {} - {} ({})", 
                    savedActivity.getId(), savedActivity.getTitle(), savedActivity.getMediaType());
            
            return ContentResponse.success(
                    mapToResponse(savedActivity),
                    "Media activity created successfully"
            );
            
        } catch (Exception e) {
            log.error("Error creating media activity", e);
            return ContentResponse.error("Failed to create media activity: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<MediaActivityResponse> updateMediaActivity(Long id, MediaActivityRequest request) {
        log.debug("Updating media activity with ID: {}", id);
        
        // Validate the request
        ValidationResult validation = validationService.validateMediaActivity(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        Optional<MediaActivity> existingActivity = mediaActivityRepository.findById(id);
        if (existingActivity.isEmpty()) {
            return ContentResponse.error("Media activity not found with ID: " + id);
        }
        
        try {
            MediaActivity activity = existingActivity.get();
            
            // Update fields
            activity.setTitle(validationService.sanitizeInput(request.getTitle()));
            activity.setMediaType(request.getMediaType());
            activity.setStatus(request.getStatus());
            activity.setCreator(validationService.sanitizeInput(request.getCreator()));
            activity.setRating(request.getRating());
            activity.setStartDate(request.getStartDate());
            activity.setCompletionDate(request.getCompletionDate());
            activity.setNotes(validationService.sanitizeInput(request.getNotes()));
            
            // If external ID changed, try to fetch new metadata
            if (request.getExternalId() != null && !request.getExternalId().equals(activity.getExternalId())) {
                activity.setExternalId(request.getExternalId());
                
                Optional<Object> metadata = metadataFetcher.fetchMetadataByType(
                        request.getTitle(), request.getMediaType(), request.getCreator(), request.getExternalId());
                
                if (metadata.isPresent()) {
                    MediaActivity.MediaActivityBuilder builder = activity.toBuilder();
                    enrichActivityWithMetadata(builder, metadata.get(), request.getMediaType());
                    activity = builder.build();
                    activity.setId(id); // Preserve the ID
                    log.info("Updated activity with new external metadata: {}", request.getTitle());
                }
            }
            
            MediaActivity updatedActivity = mediaActivityRepository.save(activity);
            
            log.info("Updated media activity with ID: {} - {}", updatedActivity.getId(), updatedActivity.getTitle());
            
            return ContentResponse.success(
                    mapToResponse(updatedActivity),
                    "Media activity updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating media activity", e);
            return ContentResponse.error("Failed to update media activity: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<MediaActivityResponse> updateActivityStatus(Long id, ActivityStatus newStatus) {
        log.debug("Updating activity status for ID: {} to {}", id, newStatus);
        
        Optional<MediaActivity> existingActivity = mediaActivityRepository.findById(id);
        if (existingActivity.isEmpty()) {
            return ContentResponse.error("Media activity not found with ID: " + id);
        }
        
        try {
            MediaActivity activity = existingActivity.get();
            ActivityStatus oldStatus = activity.getStatus();
            activity.setStatus(newStatus);
            
            // If marking as completed, set completion date if not already set
            if (newStatus == ActivityStatus.COMPLETED && activity.getCompletionDate() == null) {
                activity.setCompletionDate(LocalDate.now());
            }
            
            // If starting an activity, set start date if not already set
            if (newStatus == ActivityStatus.CURRENTLY_ENGAGED && activity.getStartDate() == null) {
                activity.setStartDate(LocalDate.now());
            }
            
            MediaActivity updatedActivity = mediaActivityRepository.save(activity);
            
            log.info("Updated activity status from {} to {} for: {}", oldStatus, newStatus, activity.getTitle());
            
            return ContentResponse.success(
                    mapToResponse(updatedActivity),
                    "Activity status updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating activity status", e);
            return ContentResponse.error("Failed to update activity status: " + e.getMessage());
        }
    }
    
    public ContentResponse<MediaActivityResponse> getMediaActivity(Long id) {
        Optional<MediaActivity> activity = mediaActivityRepository.findById(id);
        
        if (activity.isEmpty()) {
            return ContentResponse.error("Media activity not found with ID: " + id);
        }
        
        return ContentResponse.success(mapToResponse(activity.get()));
    }
    
    public ContentResponse<List<MediaActivityResponse>> getAllMediaActivities() {
        try {
            List<MediaActivity> activities = mediaActivityRepository.findAll();
            List<MediaActivityResponse> responses = activities.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving all media activities", e);
            return ContentResponse.error("Failed to retrieve media activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<MediaActivityResponse>> getActivitiesByType(MediaType mediaType) {
        try {
            List<MediaActivity> activities = mediaActivityRepository.findByMediaTypeOrderByUpdatedAtDesc(mediaType);
            List<MediaActivityResponse> responses = activities.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving activities by type: {}", mediaType, e);
            return ContentResponse.error("Failed to retrieve activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<MediaActivityResponse>> getActivitiesByStatus(ActivityStatus status) {
        try {
            List<MediaActivity> activities = mediaActivityRepository.findByStatusOrderByUpdatedAtDesc(status);
            List<MediaActivityResponse> responses = activities.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving activities by status: {}", status, e);
            return ContentResponse.error("Failed to retrieve activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<MediaActivityResponse>> getCurrentlyEngagedActivities() {
        try {
            List<MediaActivity> activities = mediaActivityRepository.findCurrentlyEngagedActivities();
            List<MediaActivityResponse> responses = activities.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving currently engaged activities", e);
            return ContentResponse.error("Failed to retrieve activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<MediaActivityResponse>> searchActivities(String searchTerm) {
        try {
            List<MediaActivity> activities = mediaActivityRepository.searchActivities(searchTerm);
            List<MediaActivityResponse> responses = activities.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error searching activities", e);
            return ContentResponse.error("Failed to search activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<Page<MediaActivityResponse>> getActivitiesPaginated(Pageable pageable) {
        try {
            Page<MediaActivity> activities = mediaActivityRepository.findAll(pageable);
            Page<MediaActivityResponse> responses = activities.map(this::mapToResponse);
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving paginated activities", e);
            return ContentResponse.error("Failed to retrieve activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<MediaActivityResponse>> getRecentActivities(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<MediaActivity> activities = mediaActivityRepository.findRecentActivities(pageable);
            List<MediaActivityResponse> responses = activities.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving recent activities", e);
            return ContentResponse.error("Failed to retrieve recent activities: " + e.getMessage());
        }
    }
    
    public ContentResponse<Map<String, Object>> getActivityStatistics() {
        try {
            long totalActivities = mediaActivityRepository.count();
            
            List<Object[]> typeStats = mediaActivityRepository.getActivityCountByMediaType();
            Map<String, Long> typeCountMap = typeStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((MediaType) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            List<Object[]> statusStats = mediaActivityRepository.getActivityCountByStatus();
            Map<String, Long> statusCountMap = statusStats.stream()
                    .collect(Collectors.toMap(
                            row -> ((ActivityStatus) row[0]).name(),
                            row -> (Long) row[1]
                    ));
            
            Map<String, Object> stats = Map.of(
                    "totalActivities", totalActivities,
                    "byMediaType", typeCountMap,
                    "byStatus", statusCountMap,
                    "currentlyEngaged", mediaActivityRepository.countByStatus(ActivityStatus.CURRENTLY_ENGAGED),
                    "completed", mediaActivityRepository.countByStatus(ActivityStatus.COMPLETED)
            );
            
            return ContentResponse.success(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving activity statistics", e);
            return ContentResponse.error("Failed to retrieve statistics: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteMediaActivity(Long id) {
        Optional<MediaActivity> activity = mediaActivityRepository.findById(id);
        
        if (activity.isEmpty()) {
            return ContentResponse.error("Media activity not found with ID: " + id);
        }
        
        try {
            mediaActivityRepository.deleteById(id);
            
            log.info("Deleted media activity with ID: {} - {}", id, activity.get().getTitle());
            return ContentResponse.success(null, "Media activity deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting media activity", e);
            return ContentResponse.error("Failed to delete media activity: " + e.getMessage());
        }
    }
    
    private void enrichActivityWithMetadata(MediaActivity.MediaActivityBuilder builder, Object metadata, MediaType mediaType) {
        switch (mediaType) {
            case BOOK:
                if (metadata instanceof BookMetadata) {
                    BookMetadata book = (BookMetadata) metadata;
                    builder.coverUrl(book.getCoverUrl())
                           .publisher(book.getPublisher())
                           .releaseYear(book.getPublishYear())
                           .genre(book.getSubjectsAsString())
                           .externalId(book.getIsbn());
                    
                    if (book.getAuthorsAsString() != null) {
                        builder.creator(book.getAuthorsAsString());
                    }
                }
                break;
                
            case MOVIE:
                if (metadata instanceof MovieMetadata) {
                    MovieMetadata movie = (MovieMetadata) metadata;
                    builder.coverUrl(movie.getPosterUrl())
                           .releaseYear(movie.getReleaseYear())
                           .genre(movie.getGenre())
                           .externalId(movie.getId() != null ? movie.getId().toString() : null);
                    
                    if (movie.getDirector() != null) {
                        builder.creator(movie.getDirector());
                    }
                }
                break;
                
            case TV_SHOW:
                if (metadata instanceof TVShowMetadata) {
                    TVShowMetadata tvShow = (TVShowMetadata) metadata;
                    builder.coverUrl(tvShow.getPosterUrl())
                           .releaseYear(tvShow.getFirstAirYear())
                           .genre(tvShow.getGenre())
                           .externalId(tvShow.getId() != null ? tvShow.getId().toString() : null);
                    
                    if (tvShow.getCreator() != null) {
                        builder.creator(tvShow.getCreator());
                    }
                }
                break;
                
            default:
                // No metadata enrichment for other types yet
                break;
        }
    }
    
    private MediaActivityResponse mapToResponse(MediaActivity activity) {
        return MediaActivityResponse.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .mediaType(activity.getMediaType())
                .status(activity.getStatus())
                .creator(activity.getCreator())
                .coverUrl(activity.getCoverUrl())
                .rating(activity.getRating())
                .startDate(activity.getStartDate())
                .completionDate(activity.getCompletionDate())
                .notes(activity.getNotes())
                .externalId(activity.getExternalId())
                .publisher(activity.getPublisher())
                .releaseYear(activity.getReleaseYear())
                .genre(activity.getGenre())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}