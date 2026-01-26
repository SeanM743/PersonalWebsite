package com.personal.backend.repository;

import com.personal.backend.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    
    // Find by name
    Optional<FamilyMember> findByNameIgnoreCase(String name);
    
    // Find by primary activity
    List<FamilyMember> findByPrimaryActivityContainingIgnoreCase(String activity);
    
    // Find by status
    List<FamilyMember> findByStatusContainingIgnoreCase(String status);
    
    // Find recently updated members
    List<FamilyMember> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime date);
    
    // Find all ordered by name
    List<FamilyMember> findAllByOrderByName();
    
    // Find all ordered by last updated
    List<FamilyMember> findAllByOrderByUpdatedAtDesc();
    
    // Search across multiple fields
    @Query("SELECT fm FROM FamilyMember fm WHERE " +
           "LOWER(fm.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(fm.primaryActivity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(fm.status) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(fm.notes) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY fm.name")
    List<FamilyMember> searchFamilyMembers(@Param("searchTerm") String searchTerm);
    
    // Check if member exists by name
    boolean existsByNameIgnoreCase(String name);
}