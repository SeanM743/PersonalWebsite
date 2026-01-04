package com.personal.backend.repository;

import com.personal.backend.model.TripStatus;
import com.personal.backend.model.TripType;
import com.personal.backend.model.UpcomingTrip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<UpcomingTrip, Long> {
    
    // Find by status
    List<UpcomingTrip> findByStatusOrderByStartDateAsc(TripStatus status);
    Page<UpcomingTrip> findByStatusOrderByStartDateAsc(TripStatus status, Pageable pageable);
    
    // Find by trip type
    List<UpcomingTrip> findByTripTypeOrderByStartDateAsc(TripType tripType);
    
    // Find by date ranges
    List<UpcomingTrip> findByStartDateBetweenOrderByStartDateAsc(LocalDate startDate, LocalDate endDate);
    List<UpcomingTrip> findByEndDateBetweenOrderByStartDateAsc(LocalDate startDate, LocalDate endDate);
    
    // Find upcoming trips (future start dates)
    @Query("SELECT t FROM UpcomingTrip t WHERE t.startDate >= :currentDate ORDER BY t.startDate ASC")
    List<UpcomingTrip> findUpcomingTrips(@Param("currentDate") LocalDate currentDate);
    
    // Find current trips (ongoing)
    @Query("SELECT t FROM UpcomingTrip t WHERE t.startDate <= :currentDate AND t.endDate >= :currentDate ORDER BY t.startDate ASC")
    List<UpcomingTrip> findCurrentTrips(@Param("currentDate") LocalDate currentDate);
    
    // Find past trips
    @Query("SELECT t FROM UpcomingTrip t WHERE t.endDate < :currentDate ORDER BY t.endDate DESC")
    List<UpcomingTrip> findPastTrips(@Param("currentDate") LocalDate currentDate);
    
    // Find trips by destination
    @Query("SELECT t FROM UpcomingTrip t WHERE LOWER(t.destination) LIKE LOWER(CONCAT('%', :destination, '%')) ORDER BY t.startDate ASC")
    List<UpcomingTrip> findByDestinationContainingIgnoreCase(@Param("destination") String destination);
    
    // Search across multiple fields
    @Query("SELECT t FROM UpcomingTrip t WHERE " +
           "LOWER(t.destination) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.plannedActivities) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.startDate ASC")
    List<UpcomingTrip> searchTrips(@Param("searchTerm") String searchTerm);
    
    // Find trips with date conflicts (overlapping dates)
    @Query("SELECT t FROM UpcomingTrip t WHERE " +
           "t.id != :excludeId AND " +
           "((t.startDate <= :endDate AND t.endDate >= :startDate)) " +
           "ORDER BY t.startDate ASC")
    List<UpcomingTrip> findConflictingTrips(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate, 
                                           @Param("excludeId") Long excludeId);
    
    // Find trips by year - TEMPORARILY DISABLED DUE TO HIBERNATE ISSUE
    // @Query("SELECT t FROM UpcomingTrip t WHERE YEAR(t.startDate) = :year ORDER BY t.startDate ASC")
    // List<UpcomingTrip> findTripsByYear(@Param("year") int year);
    
    // Statistics queries
    @Query("SELECT COUNT(t) FROM UpcomingTrip t WHERE t.status = :status")
    long countByStatus(@Param("status") TripStatus status);
    
    @Query("SELECT COUNT(t) FROM UpcomingTrip t WHERE t.tripType = :tripType")
    long countByTripType(@Param("tripType") TripType tripType);
    
    @Query("SELECT t.status, COUNT(t) FROM UpcomingTrip t GROUP BY t.status")
    List<Object[]> getTripCountByStatus();
    
    @Query("SELECT t.tripType, COUNT(t) FROM UpcomingTrip t GROUP BY t.tripType")
    List<Object[]> getTripCountByType();
    
    // Recent trips
    @Query("SELECT t FROM UpcomingTrip t ORDER BY t.updatedAt DESC")
    List<UpcomingTrip> findRecentTrips(Pageable pageable);
    
    // Find trips updated after a certain date
    List<UpcomingTrip> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime date);
}