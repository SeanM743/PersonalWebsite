package com.personal.backend.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {
    
    @Test
    void testSocialMediaPostCreation() {
        SocialMediaPost post = SocialMediaPost.builder()
                .content("Test content")
                .caption("Test caption")
                .comments("Test comments")
                .imageUrls(Arrays.asList("/images/test1.jpg", "/images/test2.jpg"))
                .build();
        
        assertNotNull(post);
        assertEquals("Test content", post.getContent());
        assertEquals("Test caption", post.getCaption());
        assertEquals("Test comments", post.getComments());
        assertEquals(2, post.getImageUrls().size());
    }
    
    @Test
    void testQuickFactCreation() {
        QuickFact fact = QuickFact.builder()
                .key("test-key")
                .value("test-value")
                .category("test")
                .description("Test description")
                .build();
        
        assertNotNull(fact);
        assertEquals("test-key", fact.getKey());
        assertEquals("test-value", fact.getValue());
        assertEquals("test", fact.getCategory());
        assertEquals("Test description", fact.getDescription());
    }
    
    @Test
    void testMediaActivityCreation() {
        MediaActivity activity = MediaActivity.builder()
                .title("Test Book")
                .mediaType(MediaType.BOOK)
                .status(ActivityStatus.CURRENTLY_ENGAGED)
                .creator("Test Author")
                .rating(5)
                .startDate(LocalDate.now())
                .notes("Great book!")
                .build();
        
        assertNotNull(activity);
        assertEquals("Test Book", activity.getTitle());
        assertEquals(MediaType.BOOK, activity.getMediaType());
        assertEquals(ActivityStatus.CURRENTLY_ENGAGED, activity.getStatus());
        assertEquals("Test Author", activity.getCreator());
        assertEquals(5, activity.getRating());
        assertEquals("Great book!", activity.getNotes());
    }
    
    @Test
    void testUpcomingTripCreation() {
        UpcomingTrip trip = UpcomingTrip.builder()
                .destination("Tokyo, Japan")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(37))
                .tripType(TripType.VACATION)
                .status(TripStatus.PLANNED)
                .description("Cherry blossom season trip")
                .plannedActivities("Visit temples, try local food, see cherry blossoms")
                .build();
        
        assertNotNull(trip);
        assertEquals("Tokyo, Japan", trip.getDestination());
        assertEquals(TripType.VACATION, trip.getTripType());
        assertEquals(TripStatus.PLANNED, trip.getStatus());
        assertEquals("Cherry blossom season trip", trip.getDescription());
        assertTrue(trip.getStartDate().isBefore(trip.getEndDate()));
    }
    
    @Test
    void testEnumValues() {
        // Test MediaType enum
        assertEquals(7, MediaType.values().length);
        assertTrue(Arrays.asList(MediaType.values()).contains(MediaType.BOOK));
        assertTrue(Arrays.asList(MediaType.values()).contains(MediaType.MOVIE));
        assertTrue(Arrays.asList(MediaType.values()).contains(MediaType.TV_SHOW));
        
        // Test ActivityStatus enum
        assertEquals(4, ActivityStatus.values().length);
        assertTrue(Arrays.asList(ActivityStatus.values()).contains(ActivityStatus.CURRENTLY_ENGAGED));
        assertTrue(Arrays.asList(ActivityStatus.values()).contains(ActivityStatus.COMPLETED));
        
        // Test TripType enum
        assertEquals(4, TripType.values().length);
        assertTrue(Arrays.asList(TripType.values()).contains(TripType.VACATION));
        assertTrue(Arrays.asList(TripType.values()).contains(TripType.BUSINESS));
        
        // Test TripStatus enum
        assertEquals(5, TripStatus.values().length);
        assertTrue(Arrays.asList(TripStatus.values()).contains(TripStatus.PLANNED));
        assertTrue(Arrays.asList(TripStatus.values()).contains(TripStatus.COMPLETED));
    }
}