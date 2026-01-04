package com.personal.backend.service;

import com.personal.backend.dto.*;
import com.personal.backend.model.SocialMediaPost;
import com.personal.backend.repository.SocialMediaPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialMediaPostService {
    
    private final SocialMediaPostRepository postRepository;
    private final ContentValidationService validationService;
    private final ContentGeneratorService contentGenerator;
    private final ImageService imageService;
    
    @Transactional
    public ContentResponse<SocialMediaPostResponse> createPost(SocialMediaPostRequest request) {
        log.debug("Creating social media post with content: {}", request.getContent());
        
        // Validate the request
        ValidationResult validation = validationService.validateSocialMediaPost(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        try {
            // Process images if present
            List<String> imageUrls = null;
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                imageUrls = imageService.processAndStoreImages(request.getImages());
            }
            
            // Sanitize content
            String sanitizedContent = validationService.sanitizeInput(request.getContent());
            String sanitizedComments = validationService.sanitizeInput(request.getComments());
            String sanitizedCaption = validationService.sanitizeInput(request.getCaption());
            
            // Create the post entity
            SocialMediaPost post = SocialMediaPost.builder()
                    .content(sanitizedContent)
                    .imageUrls(imageUrls)
                    .comments(sanitizedComments)
                    .caption(sanitizedCaption)
                    .build();
            
            // Save the post
            SocialMediaPost savedPost = postRepository.save(post);
            
            log.info("Created social media post with ID: {}", savedPost.getId());
            
            return ContentResponse.success(
                    mapToResponse(savedPost),
                    "Post created successfully"
            );
            
        } catch (Exception e) {
            log.error("Error creating social media post", e);
            return ContentResponse.error("Failed to create post: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<SocialMediaPostResponse> createPostWithAI(String prompt) {
        log.debug("Creating AI-generated social media post with prompt: {}", prompt);
        
        try {
            // Generate content using AI
            String generatedContent = contentGenerator.generatePostContent(prompt);
            String generatedCaption = contentGenerator.generatePostCaption(generatedContent);
            
            // Create request with generated content
            SocialMediaPostRequest request = SocialMediaPostRequest.builder()
                    .content(generatedContent)
                    .caption(generatedCaption)
                    .build();
            
            return createPost(request);
            
        } catch (Exception e) {
            log.error("Error creating AI-generated post", e);
            return ContentResponse.error("Failed to create AI-generated post: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<SocialMediaPostResponse> updatePost(Long id, SocialMediaPostRequest request) {
        log.debug("Updating social media post with ID: {}", id);
        
        // Validate the request
        ValidationResult validation = validationService.validateSocialMediaPost(request);
        if (validation.hasErrors()) {
            return ContentResponse.error("Validation failed", validation.toErrorMetadata());
        }
        
        Optional<SocialMediaPost> existingPost = postRepository.findById(id);
        if (existingPost.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + id);
        }
        
        try {
            SocialMediaPost post = existingPost.get();
            
            // Process new images if present
            List<String> imageUrls = post.getImageUrls();
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                // TODO: Clean up old images
                imageUrls = imageService.processAndStoreImages(request.getImages());
            }
            
            // Update fields
            post.setContent(validationService.sanitizeInput(request.getContent()));
            post.setImageUrls(imageUrls);
            post.setComments(validationService.sanitizeInput(request.getComments()));
            post.setCaption(validationService.sanitizeInput(request.getCaption()));
            
            SocialMediaPost updatedPost = postRepository.save(post);
            
            log.info("Updated social media post with ID: {}", updatedPost.getId());
            
            return ContentResponse.success(
                    mapToResponse(updatedPost),
                    "Post updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating social media post", e);
            return ContentResponse.error("Failed to update post: " + e.getMessage());
        }
    }
    
    public ContentResponse<SocialMediaPostResponse> getPost(Long id) {
        Optional<SocialMediaPost> post = postRepository.findById(id);
        
        if (post.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + id);
        }
        
        return ContentResponse.success(mapToResponse(post.get()));
    }
    
    public ContentResponse<List<SocialMediaPostResponse>> getAllPosts() {
        try {
            List<SocialMediaPost> posts = postRepository.findAllByOrderByPublishedAtDesc();
            List<SocialMediaPostResponse> responses = posts.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving all posts", e);
            return ContentResponse.error("Failed to retrieve posts: " + e.getMessage());
        }
    }
    
    public ContentResponse<Page<SocialMediaPostResponse>> getPostsPaginated(Pageable pageable) {
        try {
            Page<SocialMediaPost> posts = postRepository.findAllByOrderByPublishedAtDesc(pageable);
            Page<SocialMediaPostResponse> responses = posts.map(this::mapToResponse);
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving paginated posts", e);
            return ContentResponse.error("Failed to retrieve posts: " + e.getMessage());
        }
    }
    
    public ContentResponse<List<SocialMediaPostResponse>> getRecentPosts(int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<SocialMediaPost> posts = postRepository.findByPublishedAtAfterOrderByPublishedAtDesc(since);
            List<SocialMediaPostResponse> responses = posts.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving recent posts", e);
            return ContentResponse.error("Failed to retrieve recent posts: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deletePost(Long id) {
        Optional<SocialMediaPost> post = postRepository.findById(id);
        
        if (post.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + id);
        }
        
        try {
            // TODO: Clean up associated images
            postRepository.deleteById(id);
            
            log.info("Deleted social media post with ID: {}", id);
            return ContentResponse.success(null, "Post deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting post", e);
            return ContentResponse.error("Failed to delete post: " + e.getMessage());
        }
    }
    
    private SocialMediaPostResponse mapToResponse(SocialMediaPost post) {
        return SocialMediaPostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .imageUrls(post.getImageUrls())
                .comments(post.getComments())
                .caption(post.getCaption())
                .build();
    }
}