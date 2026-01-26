package com.personal.backend.repository;

import com.personal.backend.model.EntryStatus;
import com.personal.backend.model.LifeLogEntry;
import com.personal.backend.model.LifeLogType;
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
public interface LifeLogRepository extends JpaRepository<LifeLogEntry, Long> {
    
    // Find by type
    List<LifeLogEntry> findByTypeOrderByUpdatedAtDesc(LifeLogType type);
    Page<LifeLogEntry> findByTypeOrderByUpdatedAtDesc(LifeLogType type, Pageable pageable);
    
    // Find by status
    List<LifeLogEntry> findByStatusOrderByUpdatedAtDesc(EntryStatus status);
    Page<LifeLogEntry> findByStatusOrderByUpdatedAtDesc(EntryStatus status, Pageable pageable);
    
    // Find by type and status
    List<LifeLogEntry> findByTypeAndStatusOrderByUpdatedAtDesc(LifeLogType type, EntryStatus status);
    Page<LifeLogEntry> findByTypeAndStatusOrderByUpdatedAtDesc(LifeLogType type, EntryStatus status, Pageable pageable);
    
    // Find active entries (no endDate or endDate >= today)
    @Query("SELECT le FROM LifeLogEntry le WHERE le.endDate IS NULL OR le.endDate >= CURRENT_DATE ORDER BY le.updatedAt DESC")
    List<LifeLogEntry> findActiveEntries();
    
    @Query("SELECT le FROM LifeLogEntry le WHERE le.endDate IS NULL OR le.endDate >= CURRENT_DATE ORDER BY le.updatedAt DESC")
    Page<LifeLogEntry> findActiveEntries(Pageable pageable);
    
    // Find entries in date range
    @Query("SELECT le FROM LifeLogEntry le WHERE " +
           "(le.startDate IS NULL OR le.startDate <= :endDate) AND " +
           "(le.endDate IS NULL OR le.endDate >= :startDate) " +
           "ORDER BY le.startDate ASC, le.updatedAt DESC")
    List<LifeLogEntry> findEntriesInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Find entries by types (for filtering)
    @Query("SELECT le FROM LifeLogEntry le WHERE le.type IN :types ORDER BY le.startDate ASC, le.updatedAt DESC")
    List<LifeLogEntry> findByTypesInOrderByStartDateAsc(@Param("types") List<LifeLogType> types);
    
    @Query("SELECT le FROM LifeLogEntry le WHERE le.type IN :types AND " +
           "(le.startDate IS NULL OR le.startDate <= :endDate) AND " +
           "(le.endDate IS NULL OR le.endDate >= :startDate) " +
           "ORDER BY le.startDate ASC, le.updatedAt DESC")
    List<LifeLogEntry> findByTypesInDateRange(@Param("types") List<LifeLogType> types, 
                                             @Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate);
    
    // Find by rating
    List<LifeLogEntry> findByRatingOrderByUpdatedAtDesc(Integer rating);
    List<LifeLogEntry> findByRatingGreaterThanEqualOrderByUpdatedAtDesc(Integer minRating);
    
    // Find by intensity (HOBBY entries)
    @Query("SELECT le FROM LifeLogEntry le WHERE le.type = 'HOBBY' AND le.intensity = :intensity ORDER BY le.updatedAt DESC")
    List<LifeLogEntry> findHobbiesByIntensity(@Param("intensity") Integer intensity);
    
    @Query("SELECT le FROM LifeLogEntry le WHERE le.type = 'HOBBY' AND le.intensity >= :minIntensity ORDER BY le.intensity DESC, le.updatedAt DESC")
    List<LifeLogEntry> findHobbiesByMinIntensity(@Param("minIntensity") Integer minIntensity);
    
    // Find by title (search)
    @Query("SELECT le FROM LifeLogEntry le WHERE LOWER(le.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY le.updatedAt DESC")
    List<LifeLogEntry> findByTitleContainingIgnoreCase(@Param("title") String title);
    
    // Find by key takeaway (search)
    @Query("SELECT le FROM LifeLogEntry le WHERE LOWER(le.keyTakeaway) LIKE LOWER(CONCAT('%', :takeaway, '%')) ORDER BY le.updatedAt DESC")
    List<LifeLogEntry> findByKeyTakeawayContainingIgnoreCase(@Param("takeaway") String takeaway);
    
    // Find entries updated after a certain date
    List<LifeLogEntry> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime date);
    
    // Find by external ID
    Optional<LifeLogEntry> findByExternalId(String externalId);
    
    // Statistics queries
    @Query("SELECT COUNT(le) FROM LifeLogEntry le WHERE le.type = :type")
    long countByType(@Param("type") LifeLogType type);
    
    @Query("SELECT COUNT(le) FROM LifeLogEntry le WHERE le.status = :status")
    long countByStatus(@Param("status") EntryStatus status);
    
    @Query("SELECT AVG(le.rating) FROM LifeLogEntry le WHERE le.rating IS NOT NULL AND le.type = :type")
    Double getAverageRatingByType(@Param("type") LifeLogType type);
    
    @Query("SELECT AVG(le.intensity) FROM LifeLogEntry le WHERE le.intensity IS NOT NULL AND le.type = 'HOBBY'")
    Double getAverageHobbyIntensity();
    
    @Query("SELECT le.type, COUNT(le) FROM LifeLogEntry le GROUP BY le.type")
    List<Object[]> getEntryCountByType();
    
    @Query("SELECT le.status, COUNT(le) FROM LifeLogEntry le GROUP BY le.status")
    List<Object[]> getEntryCountByStatus();
    
    // Recent entries
    @Query("SELECT le FROM LifeLogEntry le ORDER BY le.updatedAt DESC")
    List<LifeLogEntry> findRecentEntries(Pageable pageable);
    
    // Top rated entries
    @Query("SELECT le FROM LifeLogEntry le WHERE le.rating IS NOT NULL ORDER BY le.rating DESC, le.updatedAt DESC")
    List<LifeLogEntry> findTopRatedEntries(Pageable pageable);
    
    // Search across multiple fields
    @Query("SELECT le FROM LifeLogEntry le WHERE " +
           "LOWER(le.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(le.keyTakeaway) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(le.metadata) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY le.updatedAt DESC")
    List<LifeLogEntry> searchEntries(@Param("searchTerm") String searchTerm);
    
    // Find overlapping entries (for timeline lane assignment)
    @Query("SELECT le FROM LifeLogEntry le WHERE " +
           "le.id != :excludeId AND " +
           "(le.startDate IS NULL OR le.startDate <= :endDate) AND " +
           "(le.endDate IS NULL OR le.endDate >= :startDate)")
    List<LifeLogEntry> findOverlappingEntries(@Param("excludeId") Long excludeId, 
                                             @Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate);
    
    // Find entries with no end date (ongoing)
    @Query("SELECT le FROM LifeLogEntry le WHERE le.endDate IS NULL ORDER BY le.startDate DESC, le.updatedAt DESC")
    List<LifeLogEntry> findOngoingEntries();
    
    // Find completed entries in date range
    @Query("SELECT le FROM LifeLogEntry le WHERE le.status = 'COMPLETED' AND le.endDate BETWEEN :startDate AND :endDate ORDER BY le.endDate DESC")
    List<LifeLogEntry> findCompletedEntriesInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}