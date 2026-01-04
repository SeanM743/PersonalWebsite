package com.personal.backend.repository;

import com.personal.backend.model.ActivityStatus;
import com.personal.backend.model.MediaActivity;
import com.personal.backend.model.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaActivityRepository extends JpaRepository<MediaActivity, Long> {
    
    // Find by media type
    List<MediaActivity> findByMediaTypeOrderByUpdatedAtDesc(MediaType mediaType);
    Page<MediaActivity> findByMediaTypeOrderByUpdatedAtDesc(MediaType mediaType, Pageable pageable);
    
    // Find by status
    List<MediaActivity> findByStatusOrderByUpdatedAtDesc(ActivityStatus status);
    Page<MediaActivity> findByStatusOrderByUpdatedAtDesc(ActivityStatus status, Pageable pageable);
    
    // Find by media type and status
    List<MediaActivity> findByMediaTypeAndStatusOrderByUpdatedAtDesc(MediaType mediaType, ActivityStatus status);
    
    // Find currently engaged activities
    @Query("SELECT ma FROM MediaActivity ma WHERE ma.status = 'CURRENTLY_ENGAGED' ORDER BY ma.updatedAt DESC")
    List<MediaActivity> findCurrentlyEngagedActivities();
    
    // Find completed activities
    @Query("SELECT ma FROM MediaActivity ma WHERE ma.status = 'COMPLETED' ORDER BY ma.completionDate DESC, ma.updatedAt DESC")
    List<MediaActivity> findCompletedActivities();
    
    // Find activities by rating
    List<MediaActivity> findByRatingOrderByUpdatedAtDesc(Integer rating);
    List<MediaActivity> findByRatingGreaterThanEqualOrderByUpdatedAtDesc(Integer minRating);
    
    // Find activities by creator
    @Query("SELECT ma FROM MediaActivity ma WHERE LOWER(ma.creator) LIKE LOWER(CONCAT('%', :creator, '%')) ORDER BY ma.updatedAt DESC")
    List<MediaActivity> findByCreatorContainingIgnoreCase(@Param("creator") String creator);
    
    // Find activities by title
    @Query("SELECT ma FROM MediaActivity ma WHERE LOWER(ma.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY ma.updatedAt DESC")
    List<MediaActivity> findByTitleContainingIgnoreCase(@Param("title") String title);
    
    // Find activities by date range
    List<MediaActivity> findByStartDateBetweenOrderByStartDateDesc(LocalDate startDate, LocalDate endDate);
    List<MediaActivity> findByCompletionDateBetweenOrderByCompletionDateDesc(LocalDate startDate, LocalDate endDate);
    
    // Find activities updated after a certain date
    List<MediaActivity> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime date);
    
    // Find activities by external ID
    Optional<MediaActivity> findByExternalId(String externalId);
    List<MediaActivity> findByExternalIdIsNotNullOrderByUpdatedAtDesc();
    
    // Statistics queries
    @Query("SELECT COUNT(ma) FROM MediaActivity ma WHERE ma.mediaType = :mediaType")
    long countByMediaType(@Param("mediaType") MediaType mediaType);
    
    @Query("SELECT COUNT(ma) FROM MediaActivity ma WHERE ma.status = :status")
    long countByStatus(@Param("status") ActivityStatus status);
    
    @Query("SELECT AVG(ma.rating) FROM MediaActivity ma WHERE ma.rating IS NOT NULL AND ma.mediaType = :mediaType")
    Double getAverageRatingByMediaType(@Param("mediaType") MediaType mediaType);
    
    @Query("SELECT ma.mediaType, COUNT(ma) FROM MediaActivity ma GROUP BY ma.mediaType")
    List<Object[]> getActivityCountByMediaType();
    
    @Query("SELECT ma.status, COUNT(ma) FROM MediaActivity ma GROUP BY ma.status")
    List<Object[]> getActivityCountByStatus();
    
    // Recent activities
    @Query("SELECT ma FROM MediaActivity ma ORDER BY ma.updatedAt DESC")
    List<MediaActivity> findRecentActivities(Pageable pageable);
    
    // Top rated activities
    @Query("SELECT ma FROM MediaActivity ma WHERE ma.rating IS NOT NULL ORDER BY ma.rating DESC, ma.updatedAt DESC")
    List<MediaActivity> findTopRatedActivities(Pageable pageable);
    
    // Activities by year - TEMPORARILY DISABLED DUE TO HIBERNATE ISSUE
    // @Query("SELECT ma FROM MediaActivity ma WHERE YEAR(ma.startDate) = :year ORDER BY ma.startDate DESC")
    // List<MediaActivity> findActivitiesByYear(@Param("year") int year);
    
    // @Query("SELECT ma FROM MediaActivity ma WHERE YEAR(ma.completionDate) = :year ORDER BY ma.completionDate DESC")
    // List<MediaActivity> findActivitiesCompletedInYear(@Param("year") int year);
    
    // Search across multiple fields
    @Query("SELECT ma FROM MediaActivity ma WHERE " +
           "LOWER(ma.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ma.creator) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ma.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(ma.genre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY ma.updatedAt DESC")
    List<MediaActivity> searchActivities(@Param("searchTerm") String searchTerm);
    
    // Find duplicates (same title and creator)
    @Query("SELECT ma FROM MediaActivity ma WHERE " +
           "LOWER(ma.title) = LOWER(:title) AND " +
           "LOWER(ma.creator) = LOWER(:creator) AND " +
           "ma.mediaType = :mediaType")
    List<MediaActivity> findPotentialDuplicates(@Param("title") String title, 
                                               @Param("creator") String creator, 
                                               @Param("mediaType") MediaType mediaType);
}