package com.personal.backend.config;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.SocialMediaPostRequest;
import com.personal.backend.dto.SocialMediaPostResponse;
import com.personal.backend.model.QuickFact;
import com.personal.backend.service.QuickFactService;
import com.personal.backend.service.SocialMediaPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminFunctionConfiguration {
    
    private final SocialMediaPostService postService;
    private final QuickFactService quickFactService;
    
    @Bean
    @Description("Create a new social media post with content and optional image")
    public Function<CreatePostRequest, String> createPost() {
        return request -> {
            try {
                log.info("AI function called: createPost with content length: {}", 
                        request.content() != null ? request.content().length() : 0);
                
                if (request.content() == null || request.content().trim().isEmpty()) {
                    return "Error: Post content cannot be empty.";
                }
                
                SocialMediaPostRequest postRequest = SocialMediaPostRequest.builder()
                        .content(request.content())
                        .caption(request.imageUrl()) // Using imageUrl as caption for now
                        .build();
                
                ContentResponse<SocialMediaPostResponse> response = postService.createPost(postRequest);
                
                if (response.isSuccess()) {
                    SocialMediaPostResponse post = response.getData();
                    return String.format("Successfully created post with ID: %d. Content: %s", 
                            post.getId(), 
                            post.getContent().length() > 100 
                                ? post.getContent().substring(0, 100) + "..." 
                                : post.getContent());
                } else {
                    return "Failed to create post: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function createPost", e);
                return "Error creating post: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Add or update a quick fact with key-value information")
    public Function<AddQuickFactRequest, String> addQuickFact() {
        return request -> {
            try {
                log.info("AI function called: addQuickFact with key: {}", request.key());
                
                if (request.key() == null || request.key().trim().isEmpty()) {
                    return "Error: Quick fact key cannot be empty.";
                }
                
                if (request.value() == null || request.value().trim().isEmpty()) {
                    return "Error: Quick fact value cannot be empty.";
                }
                
                QuickFact quickFact = QuickFact.builder()
                        .key(request.key().trim())
                        .value(request.value().trim())
                        .category(request.category() != null ? request.category().trim() : "general")
                        .build();
                
                ContentResponse<QuickFact> response = quickFactService.createQuickFact(quickFact);
                
                if (response.isSuccess()) {
                    QuickFact savedFact = response.getData();
                    return String.format("Successfully added quick fact: %s = %s (Category: %s)", 
                            savedFact.getKey(), savedFact.getValue(), savedFact.getCategory());
                } else {
                    return "Failed to add quick fact: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function addQuickFact", e);
                return "Error adding quick fact: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Delete a social media post by ID")
    public Function<DeletePostRequest, String> deletePost() {
        return request -> {
            try {
                log.info("AI function called: deletePost with ID: {}", request.postId());
                
                if (request.postId() == null || request.postId() <= 0) {
                    return "Error: Invalid post ID provided.";
                }
                
                ContentResponse<Void> response = postService.deletePost(request.postId());
                
                if (response.isSuccess()) {
                    return "Successfully deleted post with ID: " + request.postId();
                } else {
                    return "Failed to delete post: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function deletePost", e);
                return "Error deleting post: " + e.getMessage();
            }
        };
    }
    
    @Bean
    @Description("Update an existing quick fact by key")
    public Function<UpdateQuickFactRequest, String> updateQuickFactAdmin() {
        return request -> {
            try {
                log.info("AI function called: updateQuickFact with key: {}", request.key());
                
                if (request.key() == null || request.key().trim().isEmpty()) {
                    return "Error: Quick fact key cannot be empty.";
                }
                
                ContentResponse<QuickFact> response = quickFactService.updateQuickFact(
                        request.key(), request.newValue(), request.newCategory());
                
                if (response.isSuccess()) {
                    QuickFact updatedFact = response.getData();
                    return String.format("Successfully updated quick fact: %s = %s (Category: %s)", 
                            updatedFact.getKey(), updatedFact.getValue(), updatedFact.getCategory());
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
    @Description("Delete a quick fact by key")
    public Function<DeleteQuickFactRequest, String> deleteQuickFact() {
        return request -> {
            try {
                log.info("AI function called: deleteQuickFact with key: {}", request.key());
                
                if (request.key() == null || request.key().trim().isEmpty()) {
                    return "Error: Quick fact key cannot be empty.";
                }
                
                ContentResponse<Void> response = quickFactService.deleteQuickFact(request.key());
                
                if (response.isSuccess()) {
                    return "Successfully deleted quick fact with key: " + request.key();
                } else {
                    return "Failed to delete quick fact: " + response.getMessage();
                }
                
            } catch (Exception e) {
                log.error("Error in AI function deleteQuickFact", e);
                return "Error deleting quick fact: " + e.getMessage();
            }
        };
    }
    
    // Placeholder functions for future calendar integration
    @Bean
    @Description("Create a calendar event (placeholder - will be implemented with Google Calendar integration)")
    public Function<CreateCalendarEventRequest, String> createCalendarEvent() {
        return request -> {
            log.info("AI function called: createCalendarEvent (placeholder)");
            return "Calendar event creation is not yet implemented. This feature will be available after Google Calendar integration is complete.";
        };
    }
    
    @Bean
    @Description("Update a calendar event (placeholder - will be implemented with Google Calendar integration)")
    public Function<UpdateCalendarEventRequest, String> updateCalendarEvent() {
        return request -> {
            log.info("AI function called: updateCalendarEvent (placeholder)");
            return "Calendar event updates are not yet implemented. This feature will be available after Google Calendar integration is complete.";
        };
    }
    
    // Request record classes for AI function parameters
    public record CreatePostRequest(String content, String imageUrl) {}
    public record AddQuickFactRequest(String key, String value, String category) {}
    public record DeletePostRequest(Long postId) {}
    public record UpdateQuickFactRequest(String key, String newValue, String newCategory) {}
    public record DeleteQuickFactRequest(String key) {}
    public record CreateCalendarEventRequest(String title, String description, String startTime, String endTime) {}
    public record UpdateCalendarEventRequest(String eventId, String title, String description, String startTime, String endTime) {}
}