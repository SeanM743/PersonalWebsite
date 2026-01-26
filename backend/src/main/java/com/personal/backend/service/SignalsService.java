package com.personal.backend.service;

import com.personal.backend.dto.*;
import com.personal.backend.model.FamilyMember;
import com.personal.backend.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalsService {
    
    private final FamilyMemberRepository familyMemberRepository;
    private final ContentValidationService validationService;
    
    @Value("${signals.berkeley.graduation-date:2026-06-15}")
    private String berkeleyGraduationDate;
    
    // Bears Tracker - Placeholder implementation
    public ContentResponse<BearsGameInfo> getNextGame() {
        try {
            // TODO: Integrate with NFL API
            // For now, return a placeholder
            BearsGameInfo gameInfo = BearsGameInfo.builder()
                    .opponent("Green Bay Packers")
                    .gameDate(LocalDateTime.now().plusDays(3).withHour(12).withMinute(0))
                    .gameTime("12:00 PM CT")
                    .location("Soldier Field")
                    .isCompleted(false)
                    .isHome(true)
                    .status("SCHEDULED")
                    .isStale(false) 
                    .build();
            
            return ContentResponse.success(gameInfo, "Bears game info (placeholder data)");
            
        } catch (Exception e) {
            log.error("Error retrieving Bears game info", e);
            return ContentResponse.error("Failed to retrieve Bears game info: " + e.getMessage());
        }
    }
    
    public ContentResponse<BearsGameInfo> getLastGameResult() {
        try {
            // TODO: Integrate with NFL API
            // For now, return a placeholder
            BearsGameInfo gameInfo = BearsGameInfo.builder()
                    .opponent("Minnesota Vikings")
                    .gameDate(LocalDateTime.now().minusDays(4))
                    .isCompleted(true)
                    .homeScore(24)
                    .awayScore(17)
                    .isHome(false)
                    .status("COMPLETED")
                    .isStale(false)
                    .build();
            
            return ContentResponse.success(gameInfo, "Bears last game result (placeholder data)");
            
        } catch (Exception e) {
            log.error("Error retrieving Bears last game result", e);
            return ContentResponse.error("Failed to retrieve Bears last game result: " + e.getMessage());
        }
    }
    
    // Berkeley Countdown
    public ContentResponse<CountdownInfo> getBerkeleyCountdown() {
        try {
            LocalDate targetDate = LocalDate.parse(berkeleyGraduationDate);
            LocalDate currentDate = LocalDate.now();
            
            long daysRemaining = ChronoUnit.DAYS.between(currentDate, targetDate);
            boolean isPast = targetDate.isBefore(currentDate);
            
            String description = isPast ? 
                "Kanika graduated from UC Berkeley!" : 
                "Days until Kanika's graduation from UC Berkeley";
            
            CountdownInfo countdown = CountdownInfo.builder()
                    .targetDate(targetDate)
                    .daysRemaining(Math.abs(daysRemaining))
                    .label("Berkeley Graduation")
                    .isPast(isPast)
                    .description(description)
                    .build();
            
            return ContentResponse.success(countdown);
            
        } catch (Exception e) {
            log.error("Error calculating Berkeley countdown", e);
            return ContentResponse.error("Failed to calculate countdown: " + e.getMessage());
        }
    }
    
    // Family Pulse
    public ContentResponse<List<FamilyMemberResponse>> getFamilyPulse() {
        try {
            List<FamilyMember> members = familyMemberRepository.findAllByOrderByName();
            List<FamilyMemberResponse> responses = members.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error retrieving family pulse", e);
            return ContentResponse.error("Failed to retrieve family pulse: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<FamilyMemberResponse> updateFamilyMember(Long id, FamilyMemberRequest request) {
        log.debug("Updating family member with ID: {}", id);
        
        Optional<FamilyMember> existingMember = familyMemberRepository.findById(id);
        if (existingMember.isEmpty()) {
            return ContentResponse.error("Family member not found with ID: " + id);
        }
        
        try {
            FamilyMember member = existingMember.get();
            
            // Update fields
            if (request.getPrimaryActivity() != null) {
                member.setPrimaryActivity(validationService.sanitizeInput(request.getPrimaryActivity()));
            }
            if (request.getStatus() != null) {
                member.setStatus(validationService.sanitizeInput(request.getStatus()));
            }
            if (request.getNotes() != null) {
                member.setNotes(validationService.sanitizeInput(request.getNotes()));
            }
            
            FamilyMember updatedMember = familyMemberRepository.save(member);
            
            log.info("Updated family member: {} - {}", updatedMember.getName(), updatedMember.getPrimaryActivity());
            
            return ContentResponse.success(
                    mapToResponse(updatedMember),
                    "Family member updated successfully"
            );
            
        } catch (Exception e) {
            log.error("Error updating family member", e);
            return ContentResponse.error("Failed to update family member: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<FamilyMemberResponse> createFamilyMember(String name, FamilyMemberRequest request) {
        log.debug("Creating family member: {}", name);
        
        // Check if member already exists
        if (familyMemberRepository.existsByNameIgnoreCase(name)) {
            return ContentResponse.error("Family member already exists with name: " + name);
        }
        
        try {
            FamilyMember member = FamilyMember.builder()
                    .name(validationService.sanitizeInput(name))
                    .primaryActivity(validationService.sanitizeInput(request.getPrimaryActivity()))
                    .status(validationService.sanitizeInput(request.getStatus()))
                    .notes(validationService.sanitizeInput(request.getNotes()))
                    .build();
            
            FamilyMember savedMember = familyMemberRepository.save(member);
            
            log.info("Created family member: {} - {}", savedMember.getName(), savedMember.getPrimaryActivity());
            
            return ContentResponse.success(
                    mapToResponse(savedMember),
                    "Family member created successfully"
            );
            
        } catch (Exception e) {
            log.error("Error creating family member", e);
            return ContentResponse.error("Failed to create family member: " + e.getMessage());
        }
    }
    
    public ContentResponse<FamilyMemberResponse> getFamilyMemberById(Long id) {
        Optional<FamilyMember> member = familyMemberRepository.findById(id);
        
        if (member.isEmpty()) {
            return ContentResponse.error("Family member not found with ID: " + id);
        }
        
        return ContentResponse.success(mapToResponse(member.get()));
    }
    
    public ContentResponse<FamilyMemberResponse> getFamilyMemberByName(String name) {
        Optional<FamilyMember> member = familyMemberRepository.findByNameIgnoreCase(name);
        
        if (member.isEmpty()) {
            return ContentResponse.error("Family member not found with name: " + name);
        }
        
        return ContentResponse.success(mapToResponse(member.get()));
    }
    
    public ContentResponse<List<FamilyMemberResponse>> searchFamilyMembers(String searchTerm) {
        try {
            List<FamilyMember> members = familyMemberRepository.searchFamilyMembers(searchTerm);
            List<FamilyMemberResponse> responses = members.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            return ContentResponse.success(responses);
            
        } catch (Exception e) {
            log.error("Error searching family members", e);
            return ContentResponse.error("Failed to search family members: " + e.getMessage());
        }
    }
    
    @Transactional
    public ContentResponse<Void> deleteFamilyMember(Long id) {
        Optional<FamilyMember> member = familyMemberRepository.findById(id);
        
        if (member.isEmpty()) {
            return ContentResponse.error("Family member not found with ID: " + id);
        }
        
        try {
            familyMemberRepository.deleteById(id);
            
            log.info("Deleted family member: {}", member.get().getName());
            return ContentResponse.success(null, "Family member deleted successfully");
            
        } catch (Exception e) {
            log.error("Error deleting family member", e);
            return ContentResponse.error("Failed to delete family member: " + e.getMessage());
        }
    }
    
    // Utility method to initialize default family members
    @Transactional
    public void initializeDefaultFamilyMembers() {
        log.info("Initializing default family members");
        
        // Check if family members already exist
        if (familyMemberRepository.count() > 0) {
            log.info("Family members already exist, skipping initialization");
            return;
        }
        
        try {
            // Create default family members
            List<FamilyMember> defaultMembers = List.of(
                    FamilyMember.builder()
                            .name("Madelyn")
                            .primaryActivity("WSU")
                            .status("Student")
                            .notes("Washington State University")
                            .build(),
                    FamilyMember.builder()
                            .name("Evalyn")
                            .primaryActivity("Driving")
                            .status("Learning")
                            .notes("Getting driver's license")
                            .build(),
                    FamilyMember.builder()
                            .name("Nate")
                            .primaryActivity("School")
                            .status("Student")
                            .notes("School and sports activities")
                            .build()
            );
            
            familyMemberRepository.saveAll(defaultMembers);
            log.info("Initialized {} default family members", defaultMembers.size());
            
        } catch (Exception e) {
            log.error("Error initializing default family members", e);
        }
    }
    
    private FamilyMemberResponse mapToResponse(FamilyMember member) {
        return FamilyMemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .primaryActivity(member.getPrimaryActivity())
                .status(member.getStatus())
                .notes(member.getNotes())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}