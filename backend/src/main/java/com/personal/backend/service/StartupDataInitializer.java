package com.personal.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupDataInitializer {
    
    private final AccountSnapshotService accountSnapshotService;
    
    /**
     * Run backfill on application startup if needed
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - checking if account snapshots need backfilling");
        
        try {
            // Fill any missing snapshots from Jan 1, 2026 to today
            accountSnapshotService.fillMissingSnapshots();
            log.info("Account snapshot backfill check complete");
        } catch (Exception e) {
            log.error("Failed to backfill account snapshots: {}", e.getMessage(), e);
        }
    }
}
