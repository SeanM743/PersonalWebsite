package com.personal.backend.config;

import com.personal.backend.dto.*;
import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaType;
import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
import com.personal.backend.service.MediaActivityService;
import com.personal.backend.service.QuickFactService;
import com.personal.backend.service.SocialMediaPostService;
import com.personal.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AIFunctionConfiguration {
    
    private final SocialMediaPostService postService;
    private final QuickFactService quickFactService;
    private final MediaActivityService mediaActivityService;
    private final TripService tripService;
    
    @Bean
    @Description("Create a social media post with AI-generated content")
    public Function<CreatePostRequest, String> createSocialMediaPost() {
        return request -> {
            try {
                log.info("AI function called: createSocialMediaPost with prompt: {}", request.prompt());
                
                ContentResponse<SocialMediaPostResponse> response = postService.createPostWithAI(request.prompt());
                
                if (response.isSuccess()) {
                    SocialMediaPostResponse post = response.getData();
                    return String.format("Successfully created social media post with ID %d. Content: %s", 
                            post.getId(), post.getContent());
                } else {
                    return "Failed to create social media post: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function createSocialMediaPost", e);
                return "Error creating social media post: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Update or create a quick fact with a key-value pair")
    public Function<UpdateQuickFactRequest, String> updateQuickFact() {
        return request -> {
            try {
                log.info("AI function called: updateQuickFact for key: {}", request.key());
                
                QuickFactRequest factRequest = QuickFactRequest.builder()
                        .key(request.key())
                        .value(request.value())
                        .category(request.category())
                        .description(request.description())
                        .build();
                
                ContentResponse<com.personal.backend.model.QuickFact> response = 
                        quickFactService.createOrUpdateQuickFact(factRequest);
                
                if (response.isSuccess()) {
                    return String.format("Successfully updated quick fact '%s' with value: %s", 
                            request.key(), request.value());
                } else {
                    return "Failed to update quick fact: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function updateQuickFact", e);
                return "Error updating quick fact: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Add a new media activity (book, movie, TV show, podcast, music, magazine)")
    public Function<AddMediaActivityRequest, String> addMediaActivity() {
        return request -> {
            try {
                log.info("AI function called: addMediaActivity for: {} ({})", request.title(), request.mediaType());
                
                MediaActivityRequest activityRequest = MediaActivityRequest.builder()
                        .title(request.title())
                        .mediaType(request.mediaType())
                        .creator(request.creator())
                        .status(request.status() != null ? request.status() : ActivityStatus.PLANNED)
                        .rating(request.rating())
                        .startDate(request.startDate())
                        .completionDate(request.completionDate())
                        .notes(request.notes())
                        .externalId(request.externalId())
                        .build();
                
                ContentResponse<MediaActivityResponse> response = 
                        mediaActivityService.createMediaActivity(activityRequest);
                
                if (response.isSuccess()) {
                    MediaActivityResponse activity = response.getData();
                    return String.format("Successfully added %s '%s' by %s with ID %d", 
                            request.mediaType().name().toLowerCase(), 
                            activity.getTitle(), 
                            activity.getCreator(), 
                            activity.getId());
                } else {
                    return "Failed to add media activity: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function addMediaActivity", e);
                return "Error adding media activity: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Update the status of a media activity (PLANNED, CURRENTLY_ENGAGED, COMPLETED, ON_HOLD, DROPPED)")
    public Function<UpdateActivityStatusRequest, String> updateMediaActivityStatus() {
        return request -> {
            try {
                log.info("AI function called: updateMediaActivityStatus for ID: {} to status: {}", 
                        request.activityId(), request.status());
                
                ContentResponse<MediaActivityResponse> response = 
                        mediaActivityService.updateActivityStatus(request.activityId(), request.status());
                
                if (response.isSuccess()) {
                    MediaActivityResponse activity = response.getData();
                    return String.format("Successfully updated status of '%s' to %s", 
                            activity.getTitle(), request.status().name());
                } else {
                    return "Failed to update activity status: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function updateMediaActivityStatus", e);
                return "Error updating activity status: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Create a new trip with destination, dates, and details")
    public Function<CreateTripRequest, String> createTrip() {
        return request -> {
            try {
                log.info("AI function called: createTrip to: {}", request.destination());
                
                TripRequest tripRequest = TripRequest.builder()
                        .destination(request.destination())
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .tripType(request.tripType() != null ? request.tripType() : TripType.VACATION)
                        .description(request.description())
                        .plannedActivities(request.plannedActivities())
                        .status(request.status() != null ? request.status() : TripStatus.PLANNED)
                        .build();
                
                ContentResponse<TripResponse> response = tripService.createTrip(tripRequest);
                
                if (response.isSuccess()) {
                    TripResponse trip = response.getData();
                    return String.format("Successfully created trip to %s from %s to %s with ID %d", 
                            trip.getDestination(), 
                            trip.getStartDate(), 
                            trip.getEndDate(), 
                            trip.getId());
                } else {
                    return "Failed to create trip: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function createTrip", e);
                return "Error creating trip: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get current activities that are in progress")
    public Function<Void, String> getCurrentActivities() {
        return request -> {
            try {
                log.info("AI function called: getCurrentActivities");
                
                ContentResponse<List<MediaActivityResponse>> response = 
                        mediaActivityService.getCurrentlyEngagedActivities();
                
                if (response.isSuccess()) {
                    List<MediaActivityResponse> activities = response.getData();
                    if (activities.isEmpty()) {
                        return "No activities currently in progress.";
                    }
                    
                    StringBuilder result = new StringBuilder("Currently engaged activities:\n");
                    for (MediaActivityResponse activity : activities) {
                        result.append(String.format("- %s: %s by %s\n", 
                                activity.getMediaType().name(), 
                                activity.getTitle(), 
                                activity.getCreator()));
                    }
                    return result.toString();
                } else {
                    return "Failed to retrieve current activities: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getCurrentActivities", e);
                return "Error retrieving current activities: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get upcoming trips")
    public Function<Void, String> getUpcomingTrips() {
        return request -> {
            try {
                log.info("AI function called: getUpcomingTrips");
                
                ContentResponse<List<TripResponse>> response = tripService.getUpcomingTrips();
                
                if (response.isSuccess()) {
                    List<TripResponse> trips = response.getData();
                    if (trips.isEmpty()) {
                        return "No upcoming trips planned.";
                    }
                    
                    StringBuilder result = new StringBuilder("Upcoming trips:\n");
                    for (TripResponse trip : trips) {
                        result.append(String.format("- %s: %s to %s (%s)\n", 
                                trip.getDestination(), 
                                trip.getStartDate(), 
                                trip.getEndDate(), 
                                trip.getStatus().name()));
                    }
                    return result.toString();
                } else {
                    return "Failed to retrieve upcoming trips: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getUpcomingTrips", e);
                return "Error retrieving upcoming trips: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get quick facts by category or search term")
    public Function<GetQuickFactsRequest, String> getQuickFacts() {
        return request -> {
            try {
                log.info("AI function called: getQuickFacts with category: {}, search: {}", 
                        request.category(), request.searchTerm());
                
                ContentResponse<List<com.personal.backend.model.QuickFact>> response;
                
                if (request.category() != null && !request.category().trim().isEmpty()) {
                    response = quickFactService.getQuickFactsByCategory(request.category());
                } else if (request.searchTerm() != null && !request.searchTerm().trim().isEmpty()) {
                    response = quickFactService.searchQuickFacts(request.searchTerm());
                } else {
                    response = quickFactService.getAllQuickFacts();
                }
                
                if (response.isSuccess()) {
                    List<com.personal.backend.model.QuickFact> facts = response.getData();
                    if (facts.isEmpty()) {
                        return "No quick facts found.";
                    }
                    
                    StringBuilder result = new StringBuilder("Quick facts:\n");
                    for (com.personal.backend.model.QuickFact fact : facts) {
                        result.append(String.format("- %s: %s\n", fact.getKey(), fact.getValue()));
                    }
                    return result.toString();
                } else {
                    return "Failed to retrieve quick facts: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getQuickFacts", e);
                return "Error retrieving quick facts: " + e.getMessage();
            }
        };
    }
    
    // Request record classes for AI function parameters
    public record CreatePostRequest(String prompt) {}
    
    public record UpdateQuickFactRequest(String key, String value, String category, String description) {}
    
    public record AddMediaActivityRequest(
            String title, 
            MediaType mediaType, 
            String creator, 
            ActivityStatus status, 
            Integer rating, 
            LocalDate startDate, 
            LocalDate completionDate, 
            String notes, 
            String externalId
    ) {}
    
    public record UpdateActivityStatusRequest(Long activityId, ActivityStatus status) {}
    
    public record CreateTripRequest(
            String destination, 
            LocalDate startDate, 
            LocalDate endDate, 
            TripType tripType, 
            String description, 
            String plannedActivities, 
            TripStatus status
    ) {}
    
    public record GetQuickFactsRequest(String category, String searchTerm) {}
}