package com.personal.backend.controller;

import com.personal.backend.dto.*;
import com.personal.backend.service.SignalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalsController {
    
    private final SignalsService signalsService;
    
    // Bears Tracker endpoints
    @GetMapping("/bears")
    public ResponseEntity<ContentResponse<BearsTrackerResponse>> getBearsGameInfo() {
        ContentResponse<BearsTrackerResponse> response = signalsService.getBearsTrackerData();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/bears")
    public ResponseEntity<ContentResponse<BearsTrackerResponse>> updateBearsGameInfo(@RequestBody BearsTrackerResponse data) {
        ContentResponse<BearsTrackerResponse> response = signalsService.updateBearsTrackerData(data);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @GetMapping("/bears/next")
    public ResponseEntity<ContentResponse<BearsGameInfo>> getNextBearsGame() {
        ContentResponse<BearsGameInfo> response = signalsService.getNextGame();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/bears/last")
    public ResponseEntity<ContentResponse<BearsGameInfo>> getLastBearsGame() {
        ContentResponse<BearsGameInfo> response = signalsService.getLastGameResult();
        return ResponseEntity.ok(response);
    }
    
    // Berkeley Countdown endpoint
    @GetMapping("/countdown")
    public ResponseEntity<ContentResponse<CountdownInfo>> getBerkeleyCountdown() {
        ContentResponse<CountdownInfo> response = signalsService.getBerkeleyCountdown();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/countdown/berkeley")
    public ResponseEntity<ContentResponse<CountdownInfo>> getBerkeleyCountdownAlias() {
        ContentResponse<CountdownInfo> response = signalsService.getBerkeleyCountdown();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/countdown/berkeley")
    public ResponseEntity<ContentResponse<CountdownInfo>> updateBerkeleyCountdown(@RequestBody String date) {
        // Simple string body (YYYY-MM-DD), might need trimming if JSON quotes included
        String cleanDate = date.replace("\"", "");
        ContentResponse<CountdownInfo> response = signalsService.updateBerkeleyGraduationDate(cleanDate);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/countdown/nfl-draft")
    public ResponseEntity<ContentResponse<CountdownInfo>> getNFLDraftCountdown() {
        ContentResponse<CountdownInfo> response = signalsService.getNFLDraftCountdown();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/countdown/nfl-draft")
    public ResponseEntity<ContentResponse<CountdownInfo>> updateNFLDraftCountdown(@RequestBody String date) {
        String cleanDate = date.replace("\"", "");
        ContentResponse<CountdownInfo> response = signalsService.updateNFLDraftDate(cleanDate);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    // Family Pulse endpoints
    @GetMapping("/family")
    public ResponseEntity<ContentResponse<List<FamilyMemberResponse>>> getFamilyPulse() {
        ContentResponse<List<FamilyMemberResponse>> response = signalsService.getFamilyPulse();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/family/{id}")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> getFamilyMember(@PathVariable Long id) {
        ContentResponse<FamilyMemberResponse> response = signalsService.getFamilyMemberById(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/family/name/{name}")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> getFamilyMemberByName(@PathVariable String name) {
        ContentResponse<FamilyMemberResponse> response = signalsService.getFamilyMemberByName(name);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/family/{id}")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> updateFamilyMember(
            @PathVariable Long id, @Valid @RequestBody FamilyMemberRequest request) {
        ContentResponse<FamilyMemberResponse> response = signalsService.updateFamilyMember(id, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/family/{name}")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> createFamilyMember(
            @PathVariable String name, @Valid @RequestBody FamilyMemberRequest request) {
        ContentResponse<FamilyMemberResponse> response = signalsService.createFamilyMember(name, request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @DeleteMapping("/family/{id}")
    public ResponseEntity<ContentResponse<Void>> deleteFamilyMember(@PathVariable Long id) {
        ContentResponse<Void> response = signalsService.deleteFamilyMember(id);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/family/search")
    public ResponseEntity<ContentResponse<List<FamilyMemberResponse>>> searchFamilyMembers(
            @RequestParam String q) {
        ContentResponse<List<FamilyMemberResponse>> response = signalsService.searchFamilyMembers(q);
        return ResponseEntity.ok(response);
    }
    
    // Specific family member endpoints for convenience
    @GetMapping("/family/madelyn")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> getMadelyn() {
        ContentResponse<FamilyMemberResponse> response = signalsService.getFamilyMemberByName("Madelyn");
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/family/evalyn")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> getEvalyn() {
        ContentResponse<FamilyMemberResponse> response = signalsService.getFamilyMemberByName("Evalyn");
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/family/nate")
    public ResponseEntity<ContentResponse<FamilyMemberResponse>> getNate() {
        ContentResponse<FamilyMemberResponse> response = signalsService.getFamilyMemberByName("Nate");
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
    
    // Initialize default family members (admin endpoint)
    @PostMapping("/family/initialize")
    public ResponseEntity<ContentResponse<Void>> initializeFamilyMembers() {
        try {
            signalsService.initializeDefaultFamilyMembers();
            return ResponseEntity.ok(ContentResponse.success(null, "Default family members initialized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ContentResponse.error("Failed to initialize family members: " + e.getMessage()));
        }
    }
}