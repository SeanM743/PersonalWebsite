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
    private final com.personal.backend.repository.GlobalSettingRepository globalSettingRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    @Value("${signals.berkeley.graduation-date:2026-06-15}")
    private String defaultBerkeleyGraduationDate;
    
    // Bears Tracker
    public ContentResponse<BearsTrackerResponse> getBearsTrackerData() {
        try {
            // Try to get from DB first
            String json = globalSettingRepository.findByKey("bears.tracker_data")
                    .map(com.personal.backend.model.GlobalSetting::getValue)
                    .orElse(null);
            
            if (json != null) {
                try {
                    BearsTrackerResponse savedData = objectMapper.readValue(json, BearsTrackerResponse.class);
                    // Ensure isApiAvailable is true for manual data
                    savedData.setApiAvailable(true); 
                    return ContentResponse.success(savedData, "Bears tracker data retrieved from settings");
                } catch (Exception e) {
                    log.error("Failed to parse saved Bears data", e);
                }
            }
            
            // Fallback to Mock Data (Default)
            // Mock Data for "Next Game"
            BearsGameInfo nextGame = BearsGameInfo.builder()
                    .opponent("Green Bay Packers")
                    .gameDate(LocalDateTime.now().plusDays(3).withHour(12).withMinute(0))
                    .gameTime("12:00 PM CT")
                    .location("Soldier Field")
                    .isCompleted(false)
                    .isHome(true)
                    .status("SCHEDULED")
                    .isStale(false)
                    .build();

            // Mock Data for "Last Game"
            BearsGameInfo lastGame = BearsGameInfo.builder()
                    .opponent("Minnesota Vikings")
                    .gameDate(LocalDateTime.now().minusDays(4)) // 4 days ago
                    .isCompleted(true)
                    .homeScore(24)
                    .awayScore(17)
                    .isHome(false)
                    .status("COMPLETED")
                    .isStale(false)
                    .build();
            
            // Mock Record
            BearsTrackerResponse.TeamRecord record = BearsTrackerResponse.TeamRecord.builder()
                    .wins(10)
                    .losses(7)
                    .ties(0)
                    .build();

            BearsTrackerResponse response = BearsTrackerResponse.builder()
                    .nextGame(nextGame)
                    .lastGame(lastGame)
                    .record(record)
                    .isApiAvailable(true) 
                    .lastUpdated(LocalDateTime.now())
                    .isCachedData(false)
                    .build();
            
            return ContentResponse.success(response, "Bears tracker data retrieved successfully (default)");
            
        } catch (Exception e) {
            log.error("Error retrieving Bears tracker data", e);
            return ContentResponse.error("Failed to retrieve Bears data: " + e.getMessage());
        }
    }

    @Transactional
    public ContentResponse<BearsTrackerResponse> updateBearsTrackerData(BearsTrackerResponse data) {
        try {
            // Update timestamp
            data.setLastUpdated(LocalDateTime.now());
            data.setApiAvailable(true); // Always true for manual entry
            data.setCachedData(false);
            
            String json = objectMapper.writeValueAsString(data);
            
            com.personal.backend.model.GlobalSetting setting = globalSettingRepository.findByKey("bears.tracker_data")
                    .orElse(com.personal.backend.model.GlobalSetting.builder()
                            .key("bears.tracker_data")
                            .build());
            
            setting.setValue(json);
            setting.setUpdatedAt(LocalDateTime.now());
            globalSettingRepository.save(setting);
            
            return ContentResponse.success(data, "Bears data updated successfully");
            
        } catch (Exception e) {
            log.error("Error updating Bears tracker data", e);
            return ContentResponse.error("Failed to update Bears data: " + e.getMessage());
        }
    }

    public ContentResponse<BearsGameInfo> getNextGame() {
        return ContentResponse.success(getBearsTrackerData().getData().getNextGame());
    }
    
    public ContentResponse<BearsGameInfo> getLastGameResult() {
         return ContentResponse.success(getBearsTrackerData().getData().getLastGame());
    }
    
    // Berkeley Countdown
    public ContentResponse<CountdownInfo> getBerkeleyCountdown() {
        return getCountdown("berkeley.graduation_date", defaultBerkeleyGraduationDate, "Berkeley Graduation", 
            "Kanika graduated from UC Berkeley!", "Days until Kanika's graduation from UC Berkeley");
    }

    // NFL Draft Countdown
    public ContentResponse<CountdownInfo> getNFLDraftCountdown() {
        return getCountdown("nfl.draft_date", "2026-04-23", "NFL Draft", 
            "The NFL Draft has arrived!", "Days until the NFL Draft");
    }

    private ContentResponse<CountdownInfo> getCountdown(String key, String defaultValue, String label, String pastDesc, String futureDesc) {
        try {
            // Try to get from DB first, fall back to property
            String dateStr = globalSettingRepository.findByKey(key)
                    .map(com.personal.backend.model.GlobalSetting::getValue)
                    .orElse(defaultValue);
            
            LocalDate targetDate = LocalDate.parse(dateStr);
            LocalDate currentDate = LocalDate.now();
            
            long daysRemaining = ChronoUnit.DAYS.between(currentDate, targetDate);
            boolean isPast = targetDate.isBefore(currentDate);
            
            String description = isPast ? pastDesc : futureDesc;
            
            CountdownInfo countdown = CountdownInfo.builder()
                    .targetDate(targetDate)
                    .daysRemaining(Math.abs(daysRemaining))
                    .label(label)
                    .isPast(isPast)
                    .description(description)
                    .build();
            
            return ContentResponse.success(countdown);
            
        } catch (Exception e) {
            log.error("Error calculating countdown for " + label, e);
            return ContentResponse.error("Failed to calculate countdown: " + e.getMessage());
        }
    }

    @Transactional
    public ContentResponse<CountdownInfo> updateBerkeleyGraduationDate(String dateStr) {
        return updateCountdownDate("berkeley.graduation_date", dateStr);
    }

    @Transactional
    public ContentResponse<CountdownInfo> updateNFLDraftDate(String dateStr) {
        return updateCountdownDate("nfl.draft_date", dateStr);
    }
    
    private ContentResponse<CountdownInfo> updateCountdownDate(String key, String dateStr) {
        try {
            // Validate date format
            LocalDate.parse(dateStr); // Throws exception if invalid
            
            com.personal.backend.model.GlobalSetting setting = globalSettingRepository.findByKey(key)
                    .orElse(com.personal.backend.model.GlobalSetting.builder()
                            .key(key)
                            .build());
            
            setting.setValue(dateStr);
            setting.setUpdatedAt(LocalDateTime.now());
            globalSettingRepository.save(setting);
            
            // Return the updated countdown. We need to know WHICH one to return based on key
            // Ideally we'd pass the getter or simpler, just call one based on key.
            // For now, simpler branching:
            if (key.contains("berkeley")) return getBerkeleyCountdown();
            if (key.contains("nfl")) return getNFLDraftCountdown();
            return ContentResponse.error("Unknown countdown key");
            
        } catch (Exception e) {
            log.error("Error updating date for " + key, e);
            return ContentResponse.error("Failed to update date: " + e.getMessage());
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