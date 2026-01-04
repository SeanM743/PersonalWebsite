package com.personal.backend.config;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.MediaActivityResponse;
import com.personal.backend.dto.SocialMediaPostResponse;
import com.personal.backend.dto.TripResponse;
import com.personal.backend.model.QuickFact;
import com.personal.backend.service.MediaActivityService;
import com.personal.backend.service.QuickFactService;
import com.personal.backend.service.SocialMediaPostService;
import com.personal.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GuestFunctionConfiguration {
    
    private final SocialMediaPostService postService;
    private final QuickFactService quickFactService;
    private final MediaActivityService mediaActivityService;
    private final TripService tripService;
    
    @Bean
    @Description("Get recent social media posts from the personal blog")
    public Function<GetRecentPostsRequest, String> getRecentPosts() {
        return request -> {
            try {
                log.info("AI function called: getRecentPosts with limit: {}", request.limit());
                
                ContentResponse<List<SocialMediaPostResponse>> response = postService.getAllPosts();
                
                if (response.isSuccess()) {
                    List<SocialMediaPostResponse> posts = response.getData();
                    
                    // Limit the results
                    int limit = Math.min(request.limit() > 0 ? request.limit() : 5, 10);
                    List<SocialMediaPostResponse> limitedPosts = posts.stream()
                            .limit(limit)
                            .toList();
                    
                    if (limitedPosts.isEmpty()) {
                        return "No recent posts found.";
                    }
                    
                    StringBuilder result = new StringBuilder("Recent posts:\n");
                    for (SocialMediaPostResponse post : limitedPosts) {
                        result.append(String.format("- %s (Posted: %s)\n", 
                                post.getContent().length() > 100 
                                    ? post.getContent().substring(0, 100) + "..." 
                                    : post.getContent(),
                                post.getCreatedAt().toLocalDate()));
                    }
                    return result.toString();
                } else {
                    return "Unable to retrieve recent posts: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getRecentPosts", e);
                return "Error retrieving recent posts: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get quick facts and personal information")
    public Function<GetQuickFactsRequest, String> getQuickFactsGuest() {
        return request -> {
            try {
                log.info("AI function called: getQuickFacts with category: {}", request.category());
                
                ContentResponse<List<QuickFact>> response;
                
                if (request.category() != null && !request.category().trim().isEmpty()) {
                    response = quickFactService.getQuickFactsByCategory(request.category());
                } else {
                    response = quickFactService.getAllQuickFacts();
                }
                
                if (response.isSuccess()) {
                    List<QuickFact> facts = response.getData();
                    
                    if (facts.isEmpty()) {
                        return request.category() != null 
                            ? "No quick facts found in category: " + request.category()
                            : "No quick facts available.";
                    }
                    
                    StringBuilder result = new StringBuilder();
                    if (request.category() != null) {
                        result.append("Quick facts in category '").append(request.category()).append("':\n");
                    } else {
                        result.append("Quick facts:\n");
                    }
                    
                    for (QuickFact fact : facts) {
                        result.append(String.format("- %s: %s\n", fact.getKey(), fact.getValue()));
                    }
                    return result.toString();
                } else {
                    return "Unable to retrieve quick facts: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getQuickFacts", e);
                return "Error retrieving quick facts: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get current media activities (books, movies, TV shows, etc.)")
    public Function<GetCurrentActivitiesRequest, String> getCurrentActivities() {
        return request -> {
            try {
                log.info("AI function called: getCurrentActivities");
                
                ContentResponse<List<MediaActivityResponse>> response = 
                        mediaActivityService.getCurrentlyEngagedActivities();
                
                if (response.isSuccess()) {
                    List<MediaActivityResponse> activities = response.getData();
                    
                    if (activities.isEmpty()) {
                        return "No current activities in progress.";
                    }
                    
                    StringBuilder result = new StringBuilder("Currently engaged activities:\n");
                    for (MediaActivityResponse activity : activities) {
                        result.append(String.format("- %s: %s by %s (%s)\n", 
                                activity.getMediaType().name(), 
                                activity.getTitle(), 
                                activity.getCreator(),
                                activity.getStatus().name()));
                    }
                    return result.toString();
                } else {
                    return "Unable to retrieve current activities: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getCurrentActivities", e);
                return "Error retrieving current activities: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Get upcoming trips and travel plans")
    public Function<GetUpcomingTripsRequest, String> getUpcomingTrips() {
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
                        
                        if (trip.getDescription() != null && !trip.getDescription().trim().isEmpty()) {
                            result.append(String.format("  Description: %s\n", trip.getDescription()));
                        }
                    }
                    return result.toString();
                } else {
                    return "Unable to retrieve upcoming trips: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function getUpcomingTrips", e);
                return "Error retrieving upcoming trips: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Search through media activities by title or creator")
    public Function<SearchActivitiesRequest, String> searchActivities() {
        return request -> {
            try {
                log.info("AI function called: searchActivities with query: {}", request.query());
                
                ContentResponse<List<MediaActivityResponse>> response = 
                        mediaActivityService.searchActivities(request.query());
                
                if (response.isSuccess()) {
                    List<MediaActivityResponse> activities = response.getData();
                    
                    if (activities.isEmpty()) {
                        return "No activities found matching: " + request.query();
                    }
                    
                    StringBuilder result = new StringBuilder("Activities matching '")
                            .append(request.query()).append("':\n");
                    
                    for (MediaActivityResponse activity : activities.stream().limit(10).toList()) {
                        result.append(String.format("- %s: %s by %s (%s", 
                                activity.getMediaType().name(), 
                                activity.getTitle(), 
                                activity.getCreator(),
                                activity.getStatus().name()));
                        
                        if (activity.getRating() != null) {
                            result.append(", Rating: ").append(activity.getRating()).append("/5");
                        }
                        result.append(")\n");
                    }
                    return result.toString();
                } else {
                    return "Unable to search activities: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function searchActivities", e);
                return "Error searching activities: " + e.getMessage();
            }
        };
    }
    
    // Request record classes for AI function parameters
    public record GetRecentPostsRequest(int limit) {}
    public record GetQuickFactsRequest(String category) {}
    public record GetCurrentActivitiesRequest() {}
    public record GetUpcomingTripsRequest() {}
    public record SearchActivitiesRequest(String query) {}
}