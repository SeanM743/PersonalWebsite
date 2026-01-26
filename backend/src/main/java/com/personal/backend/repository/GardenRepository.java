package com.personal.backend.repository;

import com.personal.backend.model.GardenNote;
import com.personal.backend.model.GrowthStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GardenRepository extends JpaRepository<GardenNote, Long> {
    
    // Find by growth stage
    List<GardenNote> findByGrowthStageOrderByUpdatedAtDesc(GrowthStage growthStage);
    Page<GardenNote> findByGrowthStageOrderByUpdatedAtDesc(GrowthStage growthStage, Pageable pageable);
    
    // Find by title (search)
    @Query("SELECT gn FROM GardenNote gn WHERE LOWER(gn.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY gn.updatedAt DESC")
    List<GardenNote> findByTitleContainingIgnoreCase(@Param("title") String title);
    
    // Find by content (search)
    @Query("SELECT gn FROM GardenNote gn WHERE LOWER(gn.content) LIKE LOWER(CONCAT('%', :content, '%')) ORDER BY gn.updatedAt DESC")
    List<GardenNote> findByContentContainingIgnoreCase(@Param("content") String content);
    
    // Find notes linked to specific LifeLog entry
    @Query("SELECT gn FROM GardenNote gn JOIN gn.linkedEntries le WHERE le.id = :lifeLogId ORDER BY gn.updatedAt DESC")
    List<GardenNote> findNotesLinkedToEntry(@Param("lifeLogId") Long lifeLogId);
    
    // Find notes with any linked entries
    @Query("SELECT gn FROM GardenNote gn WHERE SIZE(gn.linkedEntries) > 0 ORDER BY gn.updatedAt DESC")
    List<GardenNote> findNotesWithLinkedEntries();
    
    // Find notes without any linked entries
    @Query("SELECT gn FROM GardenNote gn WHERE SIZE(gn.linkedEntries) = 0 ORDER BY gn.updatedAt DESC")
    List<GardenNote> findNotesWithoutLinkedEntries();
    
    // Find notes updated after a certain date
    List<GardenNote> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime date);
    
    // Statistics queries
    @Query("SELECT COUNT(gn) FROM GardenNote gn WHERE gn.growthStage = :growthStage")
    long countByGrowthStage(@Param("growthStage") GrowthStage growthStage);
    
    @Query("SELECT gn.growthStage, COUNT(gn) FROM GardenNote gn GROUP BY gn.growthStage")
    List<Object[]> getNoteCountByGrowthStage();
    
    @Query("SELECT COUNT(gn) FROM GardenNote gn WHERE SIZE(gn.linkedEntries) > 0")
    long countNotesWithLinks();
    
    @Query("SELECT COUNT(gn) FROM GardenNote gn WHERE SIZE(gn.linkedEntries) = 0")
    long countNotesWithoutLinks();
    
    // Recent notes
    @Query("SELECT gn FROM GardenNote gn ORDER BY gn.updatedAt DESC")
    List<GardenNote> findRecentNotes(Pageable pageable);
    
    // Search across multiple fields
    @Query("SELECT gn FROM GardenNote gn WHERE " +
           "LOWER(gn.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(gn.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY gn.updatedAt DESC")
    List<GardenNote> searchNotes(@Param("searchTerm") String searchTerm);
    
    // Find notes by growth stage and search term
    @Query("SELECT gn FROM GardenNote gn WHERE " +
           "gn.growthStage = :growthStage AND " +
           "(LOWER(gn.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(gn.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY gn.updatedAt DESC")
    List<GardenNote> findByGrowthStageAndSearchTerm(@Param("growthStage") GrowthStage growthStage, 
                                                   @Param("searchTerm") String searchTerm);
    
    // Find notes linked to entries of specific type
    @Query("SELECT DISTINCT gn FROM GardenNote gn JOIN gn.linkedEntries le WHERE le.type = :entryType ORDER BY gn.updatedAt DESC")
    List<GardenNote> findNotesLinkedToEntryType(@Param("entryType") com.personal.backend.model.LifeLogType entryType);
    
    // Find notes by growth stage with linked entries
    @Query("SELECT gn FROM GardenNote gn WHERE gn.growthStage = :growthStage AND SIZE(gn.linkedEntries) > 0 ORDER BY gn.updatedAt DESC")
    List<GardenNote> findByGrowthStageWithLinks(@Param("growthStage") GrowthStage growthStage);
    
    // Find notes by growth stage without linked entries
    @Query("SELECT gn FROM GardenNote gn WHERE gn.growthStage = :growthStage AND SIZE(gn.linkedEntries) = 0 ORDER BY gn.updatedAt DESC")
    List<GardenNote> findByGrowthStageWithoutLinks(@Param("growthStage") GrowthStage growthStage);
    
    // Find potential duplicates (same title)
    @Query("SELECT gn FROM GardenNote gn WHERE LOWER(gn.title) = LOWER(:title) AND gn.id != :excludeId")
    List<GardenNote> findPotentialDuplicates(@Param("title") String title, @Param("excludeId") Long excludeId);
    
    // Find notes created in date range
    @Query("SELECT gn FROM GardenNote gn WHERE gn.createdAt BETWEEN :startDate AND :endDate ORDER BY gn.createdAt DESC")
    List<GardenNote> findNotesCreatedInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    // Find notes updated in date range
    @Query("SELECT gn FROM GardenNote gn WHERE gn.updatedAt BETWEEN :startDate AND :endDate ORDER BY gn.updatedAt DESC")
    List<GardenNote> findNotesUpdatedInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
}