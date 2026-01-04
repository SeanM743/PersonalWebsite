package com.personal.backend.service;

import com.personal.backend.dto.QuickFactRequest;
import com.personal.backend.dto.SocialMediaPostRequest;
import com.personal.backend.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentValidationServiceTest {
    
    private ContentValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ContentValidationService();
    }
    
    @Test
    void testValidSocialMediaPost() {
        SocialMediaPostRequest request = SocialMediaPostRequest.builder()
                .content("This is a valid post content")
                .caption("Valid caption")
                .build();
        
        ValidationResult result = validationService.validateSocialMediaPost(request);
        
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }
    
    @Test
    void testSocialMediaPostWithoutContentOrImages() {
        SocialMediaPostRequest request = SocialMediaPostRequest.builder()
                .caption("Just a caption")
                .build();
        
        ValidationResult result = validationService.validateSocialMediaPost(request);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.hasErrorsForField("content"));
    }
    
    @Test
    void testSocialMediaPostWithTooLongContent() {
        String longContent = "a".repeat(2001); // Exceeds 2000 character limit
        
        SocialMediaPostRequest request = SocialMediaPostRequest.builder()
                .content(longContent)
                .build();
        
        ValidationResult result = validationService.validateSocialMediaPost(request);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrorsForField("content"));
    }
    
    @Test
    void testValidQuickFact() {
        QuickFactRequest request = QuickFactRequest.builder()
                .key("test-key")
                .value("test-value")
                .category("test")
                .build();
        
        ValidationResult result = validationService.validateQuickFact(request);
        
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }
    
    @Test
    void testQuickFactWithEmptyKey() {
        QuickFactRequest request = QuickFactRequest.builder()
                .key("")
                .value("test-value")
                .build();
        
        ValidationResult result = validationService.validateQuickFact(request);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrorsForField("key"));
    }
    
    @Test
    void testQuickFactWithEmptyValue() {
        QuickFactRequest request = QuickFactRequest.builder()
                .key("test-key")
                .value("")
                .build();
        
        ValidationResult result = validationService.validateQuickFact(request);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrorsForField("value"));
    }
    
    @Test
    void testInputSanitization() {
        String maliciousInput = "<script>alert('xss')</script>Hello <b>World</b>";
        String sanitized = validationService.sanitizeInput(maliciousInput);
        
        assertNotNull(sanitized);
        assertFalse(sanitized.contains("<script>"));
        assertTrue(sanitized.contains("<b>World</b>")); // Basic HTML should be preserved
    }
    
    @Test
    void testHtmlSanitization() {
        String htmlInput = "<p>Hello</p><script>alert('xss')</script><strong>World</strong>";
        String sanitized = validationService.sanitizeHtml(htmlInput);
        
        assertNotNull(sanitized);
        assertFalse(sanitized.contains("<script>"));
        assertTrue(sanitized.contains("<p>Hello</p>"));
        assertTrue(sanitized.contains("<strong>World</strong>"));
    }
}