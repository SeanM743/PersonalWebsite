package com.personal.backend.integration;

import com.personal.backend.dto.*;
import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaType;
import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
import com.personal.backend.service.MediaActivityService;
import com.personal.backend.service.QuickFactService;
import com.personal.backend.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ContentManagementIntegrationTest {
    
    @Autowired
    private MediaActivityService mediaActivityService;
    
    @Autowired
    private QuickFactService quickFactService;
    
    @Autowired
    private TripService tripService;
    
    @Test
    public void testMediaActivityWorkflow() {
        // Create a media activity
        MediaActivityRequest request = MediaActivityRequest.builder()
                .title("Test Book")
                .mediaType(MediaType.BOOK)
                .creator("Test Author")
                .status(ActivityStatus.PLANNED)
                .build();
        
        ContentResponse<MediaActivityResponse> createResponse = 
                mediaActivityService.createMediaActivity(request);
        
        assertTrue(createResponse.isSuccess());
        assertNotNull(createResponse.getData());
        assertEquals("Test Book", createResponse.getData().getTitle());
        
        // Update the activity status
        Long activityId = createResponse.getData().getId();
        ContentResponse<MediaActivityResponse> updateResponse = 
                mediaActivityService.updateActivityStatus(activityId, ActivityStatus.CURRENTLY_ENGAGED);
        
        assertTrue(updateResponse.isSuccess());
        assertEquals(ActivityStatus.CURRENTLY_ENGAGED, updateResponse.getData().getStatus());
        
        // Retrieve the activity
        ContentResponse<MediaActivityResponse> getResponse = 
                mediaActivityService.getMediaActivity(activityId);
        
        assertTrue(getResponse.isSuccess());
        assertEquals(ActivityStatus.CURRENTLY_ENGAGED, getResponse.getData().getStatus());
    }
    
    @Test
    public void testQuickFactWorkflow() {
        // Create a quick fact
        QuickFactRequest request = QuickFactRequest.builder()
                .key("test_key")
                .value("test_value")
                .category("test")
                .description("Test description")
                .build();
        
        ContentResponse<com.personal.backend.model.QuickFact> createResponse = 
                quickFactService.createOrUpdateQuickFact(request);
        
        assertTrue(createResponse.isSuccess());
        assertNotNull(createResponse.getData());
        assertEquals("test_key", createResponse.getData().getKey());
        assertEquals("test_value", createResponse.getData().getValue());
        
        // Update the quick fact
        ContentResponse<com.personal.backend.model.QuickFact> updateResponse = 
                quickFactService.updateQuickFactValue("test_key", "updated_value");
        
        assertTrue(updateResponse.isSuccess());
        assertEquals("updated_value", updateResponse.getData().getValue());
        
        // Retrieve the quick fact
        ContentResponse<com.personal.backend.model.QuickFact> getResponse = 
                quickFactService.getQuickFact("test_key");
        
        assertTrue(getResponse.isSuccess());
        assertEquals("updated_value", getResponse.getData().getValue());
    }
    
    @Test
    public void testTripWorkflow() {
        // Create a trip
        TripRequest request = TripRequest.builder()
                .destination("Test Destination")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(35))
                .tripType(TripType.VACATION)
                .description("Test trip description")
                .status(TripStatus.PLANNED)
                .build();
        
        ContentResponse<TripResponse> createResponse = tripService.createTrip(request);
        
        assertTrue(createResponse.isSuccess());
        assertNotNull(createResponse.getData());
        assertEquals("Test Destination", createResponse.getData().getDestination());
        
        // Update the trip status
        Long tripId = createResponse.getData().getId();
        ContentResponse<TripResponse> updateResponse = 
                tripService.updateTripStatus(tripId, TripStatus.CONFIRMED);
        
        assertTrue(updateResponse.isSuccess());
        assertEquals(TripStatus.CONFIRMED, updateResponse.getData().getStatus());
        
        // Retrieve the trip
        ContentResponse<TripResponse> getResponse = tripService.getTrip(tripId);
        
        assertTrue(getResponse.isSuccess());
        assertEquals(TripStatus.CONFIRMED, getResponse.getData().getStatus());
    }
    
    @Test
    public void testCrossContentTypeOperations() {
        // Test that different content types can coexist and be managed independently
        
        // Create media activity
        MediaActivityRequest activityRequest = MediaActivityRequest.builder()
                .title("Integration Test Movie")
                .mediaType(MediaType.MOVIE)
                .creator("Test Director")
                .status(ActivityStatus.PLANNED)
                .build();
        
        ContentResponse<MediaActivityResponse> activityResponse = 
                mediaActivityService.createMediaActivity(activityRequest);
        assertTrue(activityResponse.isSuccess());
        
        // Create quick fact
        QuickFactRequest factRequest = QuickFactRequest.builder()
                .key("integration_test")
                .value("cross_content_test")
                .category("testing")
                .build();
        
        ContentResponse<com.personal.backend.model.QuickFact> factResponse = 
                quickFactService.createOrUpdateQuickFact(factRequest);
        assertTrue(factResponse.isSuccess());
        
        // Create trip
        TripRequest tripRequest = TripRequest.builder()
                .destination("Integration Test City")
                .startDate(LocalDate.now().plusDays(60))
                .endDate(LocalDate.now().plusDays(65))
                .tripType(TripType.BUSINESS)
                .status(TripStatus.PLANNED)
                .build();
        
        ContentResponse<TripResponse> tripResponse = tripService.createTrip(tripRequest);
        assertTrue(tripResponse.isSuccess());
        
        // Verify all content types exist independently
        assertNotNull(mediaActivityService.getMediaActivity(activityResponse.getData().getId()).getData());
        assertNotNull(quickFactService.getQuickFact("integration_test").getData());
        assertNotNull(tripService.getTrip(tripResponse.getData().getId()).getData());
    }
}