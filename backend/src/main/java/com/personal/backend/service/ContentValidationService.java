package com.personal.backend.service;

import com.personal.backend.dto.*;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class ContentValidationService {
    
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );
    
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    
    public ValidationResult validateSocialMediaPost(SocialMediaPostRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Content validation - must have either text or images
        if (!StringUtils.hasText(request.getContent()) && 
            (request.getImages() == null || request.getImages().isEmpty())) {
            result.addError("content", "Post must have either text content or images");
        }
        
        // Content length validation
        if (request.getContent() != null && request.getContent().length() > 2000) {
            result.addError("content", "Post content must be less than 2000 characters");
        }
        
        // Image validation
        if (request.getImages() != null) {
            if (request.getImages().size() > 10) {
                result.addError("images", "Maximum 10 images allowed per post");
            }
            
            for (int i = 0; i < request.getImages().size(); i++) {
                MultipartFile image = request.getImages().get(i);
                ValidationResult imageResult = validateImage(image);
                if (imageResult.hasErrors()) {
                    final int index = i; // Make effectively final for lambda
                    imageResult.getErrors().forEach((field, errors) -> {
                        errors.forEach(error -> result.addError("images[" + index + "]." + field, error));
                    });
                }
            }
        }
        
        // Comments validation
        if (request.getComments() != null && request.getComments().length() > 1000) {
            result.addError("comments", "Comments must be less than 1000 characters");
        }
        
        // Caption validation
        if (request.getCaption() != null && request.getCaption().length() > 500) {
            result.addError("caption", "Caption must be less than 500 characters");
        }
        
        return result;
    }
    
    public ValidationResult validateMediaActivity(MediaActivityRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Title validation
        if (!StringUtils.hasText(request.getTitle())) {
            result.addError("title", "Title is required");
        } else if (request.getTitle().length() > 500) {
            result.addError("title", "Title must be less than 500 characters");
        }
        
        // Media type validation
        if (request.getMediaType() == null) {
            result.addError("mediaType", "Media type is required");
        }
        
        // Rating validation
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            result.addError("rating", "Rating must be between 1 and 5");
        }
        
        // Date validation
        if (request.getStartDate() != null && request.getCompletionDate() != null && 
            request.getStartDate().isAfter(request.getCompletionDate())) {
            result.addError("dates", "Start date must be before completion date");
        }
        
        // Creator validation
        if (request.getCreator() != null && request.getCreator().length() > 300) {
            result.addError("creator", "Creator must be less than 300 characters");
        }
        
        return result;
    }
    
    public ValidationResult validateTrip(TripRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Destination validation
        if (!StringUtils.hasText(request.getDestination())) {
            result.addError("destination", "Destination is required");
        } else if (request.getDestination().length() > 300) {
            result.addError("destination", "Destination must be less than 300 characters");
        }
        
        // Date validation
        if (request.getStartDate() != null && request.getEndDate() != null && 
            request.getStartDate().isAfter(request.getEndDate())) {
            result.addError("dates", "Start date must be before end date");
        }
        
        // Description validation
        if (request.getDescription() != null && request.getDescription().length() > 2000) {
            result.addError("description", "Description must be less than 2000 characters");
        }
        
        // Planned activities validation
        if (request.getPlannedActivities() != null && request.getPlannedActivities().length() > 2000) {
            result.addError("plannedActivities", "Planned activities must be less than 2000 characters");
        }
        
        return result;
    }
    
    public ValidationResult validateQuickFact(QuickFactRequest request) {
        ValidationResult result = new ValidationResult();
        
        // Key validation
        if (!StringUtils.hasText(request.getKey())) {
            result.addError("key", "Key is required");
        } else if (request.getKey().length() > 100) {
            result.addError("key", "Key must be less than 100 characters");
        }
        
        // Value validation
        if (!StringUtils.hasText(request.getValue())) {
            result.addError("value", "Value is required");
        } else if (request.getValue().length() > 1000) {
            result.addError("value", "Value must be less than 1000 characters");
        }
        
        // Category validation
        if (request.getCategory() != null && request.getCategory().length() > 50) {
            result.addError("category", "Category must be less than 50 characters");
        }
        
        // Description validation
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            result.addError("description", "Description must be less than 500 characters");
        }
        
        return result;
    }
    
    private ValidationResult validateImage(MultipartFile image) {
        ValidationResult result = new ValidationResult();
        
        if (image == null || image.isEmpty()) {
            result.addError("file", "Image file is required");
            return result;
        }
        
        // File size validation
        if (image.getSize() > MAX_IMAGE_SIZE) {
            result.addError("size", "Image size exceeds maximum allowed size of 5MB");
        }
        
        // Content type validation
        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            result.addError("format", "Unsupported image format. Supported formats: JPEG, PNG, WebP");
        }
        
        // Filename validation
        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            result.addError("filename", "Image filename is required");
        }
        
        return result;
    }
    
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potentially harmful content while preserving basic formatting
        String cleaned = Jsoup.clean(input, Safelist.basicWithImages()
                .addTags("br", "p", "strong", "em", "u")
                .addAttributes("img", "alt", "title"));
        
        // Unescape entities to prevent double-escaping on frontend (e.g. &amp; -> &)
        return Parser.unescapeEntities(cleaned, true);
    }
    
    public String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }
        
        // More restrictive sanitization for HTML content
        return Jsoup.clean(html, Safelist.basic());
    }
}