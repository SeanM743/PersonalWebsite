package com.personal.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskService {
    
    private final AccountSnapshotService accountSnapshotService;
    
    /**
     * Create daily account snapshots at midnight every day
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledDailySnapshot() {
        log.info("Running scheduled daily account snapshot");
        try {
            accountSnapshotService.createDailySnapshots();
            log.info("Scheduled daily snapshot completed successfully");
        } catch (Exception e) {
            log.error("Failed to create scheduled daily snapshot: {}", e.getMessage(), e);
        }
    }
}
