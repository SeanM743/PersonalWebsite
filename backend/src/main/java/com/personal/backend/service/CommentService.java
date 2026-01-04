package com.personal.backend.service;

import com.personal.backend.dto.ContentResponse;
import com.personal.backend.dto.ValidationResult;
import com.personal.backend.model.SocialMediaPost;
import com.personal.backend.repository.SocialMediaPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final SocialMediaPostRepository postRepository;
    private final ContentValidationService validationService;
    
    @Transactional
    public ContentResponse<String> addComment(Long postId, String comment) {
        log.debug("Adding comment to post ID: {}", postId);
        
        // Validate comment
        ValidationResult validation = validateComment(comment);
        if (validation.hasErrors()) {
            return ContentResponse.error("Comment validation failed", validation.toErrorMetadata());
        }
        
        Optional<SocialMediaPost> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + postId);
        }
        
        try {
            SocialMediaPost post = postOpt.get();
            String sanitizedComment = validationService.sanitizeInput(comment);
            
            // Append to existing comments or create new
            String existingComments = post.getComments();
            String updatedComments;
            
            if (StringUtils.hasText(existingComments)) {
                updatedComments = existingComments + "\n\n" + sanitizedComment;
            } else {
                updatedComments = sanitizedComment;
            }
            
            post.setComments(updatedComments);
            postRepository.save(post);
            
            log.info("Added comment to post ID: {}", postId);
            return ContentResponse.success(updatedComments, "Comment added successfully");
            
        } catch (Exception e) {
            log.error("Error adding comment to post", e);
            return ContentResponse.error("Failed to add comment: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<String> updateComment(Long postId, String newComment) {
        log.debug("Updating comment for post ID: {}", postId);
        
        // Validate comment
        ValidationResult validation = validateComment(newComment);
        if (validation.hasErrors()) {
            return ContentResponse.error("Comment validation failed", validation.toErrorMetadata());
        }
        
        Optional<SocialMediaPost> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + postId);
        }
        
        try {
            SocialMediaPost post = postOpt.get();
            String sanitizedComment = validationService.sanitizeInput(newComment);
            
            post.setComments(sanitizedComment);
            postRepository.save(post);
            
            log.info("Updated comment for post ID: {}", postId);
            return ContentResponse.success(sanitizedComment, "Comment updated successfully");
            
        } catch (Exception e) {
            log.error("Error updating comment for post", e);
            return ContentResponse.error("Failed to update comment: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteComment(Long postId) {
        log.debug("Deleting comment from post ID: {}", postId);
        
        Optional<SocialMediaPost> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + postId);
        }
        
        try {
            SocialMediaPost post = postOpt.get();
            post.setComments(null);
            postRepository.save(post);
            
            log.info("Deleted comment from post ID: {}", postId);
            return ContentResponse.success(null, "Comment deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting comment from post", e);
            return ContentResponse.error("Failed to delete comment: " + e.getMessage());
        }
    }
    
    public ContentResponse<String> getComment(Long postId) {
        Optional<SocialMediaPost> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ContentResponse.error("Post not found with ID: " + postId);
        }
        
        String comments = postOpt.get().getComments();
        return ContentResponse.success(comments);
    }
    
    private ValidationResult validateComment(String comment) {
        ValidationResult result = new ValidationResult();
        
        if (!StringUtils.hasText(comment)) {
            result.addError("comment", "Comment cannot be empty");
        } else if (comment.length() > 1000) {
            result.addError("comment", "Comment must be less than 1000 characters");
        }
        
        return result;
    }
    
    public boolean supportsRichText(String comment) {
        // Check if comment contains HTML tags or markdown
        return comment != null && (comment.contains("<") || comment.contains("*") || comment.contains("_"));
    }
    
    public String formatComment(String comment) {
        if (comment == null) {
            return null;
        }
        
        // Basic formatting for rich text support
        String formatted = comment
                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>") // Bold
                .replaceAll("\\*(.*?)\\*", "<em>$1</em>") // Italic
                .replaceAll("_(.*?)_", "<u>$1</u>") // Underline
                .replaceAll("\\n", "<br>"); // Line breaks
        
        return validationService.sanitizeHtml(formatted);
    }
}