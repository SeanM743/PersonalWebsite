package com.personal.backend.controller;

import com.personal.backend.service.AccountSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio/snapshots")
@RequiredArgsConstructor
@Slf4j
public class AccountSnapshotController {
    
    private final AccountSnapshotService accountSnapshotService;
    
    /**
     * Manually trigger daily snapshot creation
     */
    @PostMapping("/create")
    public ResponseEntity<?> createDailySnapshots() {
        log.info("Manual trigger: creating daily snapshots");
        try {
            accountSnapshotService.createDailySnapshots();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily snapshots created successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to create daily snapshots: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to create snapshots: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Backfill historical snapshots
     */
    @PostMapping("/backfill")
    public ResponseEntity<?> backfillSnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Manual trigger: backfilling snapshots from {} to {}", startDate, endDate);
        try {
            accountSnapshotService.backfillHistoricalSnapshots(startDate, endDate);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Backfilled snapshots from %s to %s", startDate, endDate)
            ));
        } catch (Exception e) {
            log.error("Failed to backfill snapshots: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to backfill: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Fill missing snapshots
     */
    @PostMapping("/fill-missing")
    public ResponseEntity<?> fillMissingSnapshots() {
        log.info("Manual trigger: filling missing snapshots");
        try {
            accountSnapshotService.fillMissingSnapshots();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Missing snapshots filled successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to fill missing snapshots: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fill missing snapshots: " + e.getMessage()
            ));
        }
    }
}
