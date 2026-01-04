package com.personal.backend.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarTokenManager {
    
    private final GoogleCalendarAuthManager authManager;
    
    private final AtomicReference<TokenInfo> currentTokenInfo = new AtomicReference<>();
    private final AtomicLong refreshCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    
    // Refresh token when it expires within this many minutes
    private static final int REFRESH_BUFFER_MINUTES = 5;
    
    /**
     * Get current token with automatic refresh if needed
     */
    public AccessToken getCurrentToken() throws IOException {
        GoogleCredentials credentials = authManager.getCredentials();
        if (credentials == null) {
            throw new IOException("No credentials available");
        }
        
        AccessToken token = credentials.getAccessToken();
        
        // Check if token needs refresh
        if (token == null || isTokenExpiringSoon(token)) {
            synchronized (this) {
                // Double-check pattern
                token = credentials.getAccessToken();
                if (token == null || isTokenExpiringSoon(token)) {
                    token = refreshToken(credentials);
                }
            }
        }
        
        // Update token info
        updateTokenInfo(token);
        
        return token;
    }
    
    /**
     * Force refresh the current token
     */
    public AccessToken forceRefreshToken() throws IOException {
        GoogleCredentials credentials = authManager.getCredentials();
        if (credentials == null) {
            throw new IOException("No credentials available for token refresh");
        }
        
        synchronized (this) {
            log.info("Force refreshing Google Calendar access token");
            return refreshToken(credentials);
        }
    }
    
    /**
     * Check if current token is valid and not expiring soon
     */
    public boolean isTokenValid() {
        try {
            AccessToken token = getCurrentToken();
            return token != null && !isTokenExpiringSoon(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get token information for monitoring
     */
    public TokenInfo getTokenInfo() {
        TokenInfo info = currentTokenInfo.get();
        if (info != null) {
            return info;
        }
        
        try {
            GoogleCredentials credentials = authManager.getCredentials();
            if (credentials != null) {
                AccessToken token = credentials.getAccessToken();
                if (token != null) {
                    return createTokenInfo(token);
                }
            }
        } catch (Exception e) {
            log.debug("Error getting token info: {}", e.getMessage());
        }
        
        return TokenInfo.builder()
                .available(false)
                .refreshCount(refreshCount.get())
                .failureCount(failureCount.get())
                .build();
    }
    
    /**
     * Get token statistics for monitoring
     */
    public TokenStatistics getTokenStatistics() {
        TokenInfo info = getTokenInfo();
        
        return TokenStatistics.builder()
                .tokenAvailable(info.isAvailable())
                .tokenValid(isTokenValid())
                .totalRefreshes(refreshCount.get())
                .totalFailures(failureCount.get())
                .lastRefresh(info.getLastRefresh())
                .expiresAt(info.getExpiresAt())
                .minutesUntilExpiry(info.getMinutesUntilExpiry())
                .needsRefresh(info.isNeedsRefresh())
                .build();
    }
    
    /**
     * Scheduled task to proactively refresh tokens
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void proactiveTokenRefresh() {
        try {
            if (!authManager.areCredentialsAvailable()) {
                return;
            }
            
            GoogleCredentials credentials = authManager.getCredentials();
            if (credentials == null) {
                return;
            }
            
            AccessToken token = credentials.getAccessToken();
            if (token != null && isTokenExpiringSoon(token)) {
                log.info("Proactively refreshing Google Calendar token (expires soon)");
                refreshToken(credentials);
            }
            
        } catch (Exception e) {
            log.warn("Proactive token refresh failed: {}", e.getMessage());
            failureCount.incrementAndGet();
        }
    }
    
    /**
     * Scheduled task to log token statistics
     */
    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    public void logTokenStatistics() {
        try {
            TokenStatistics stats = getTokenStatistics();
            
            if (stats.isTokenAvailable()) {
                log.info("Token statistics: valid={}, expires in {} minutes, refreshes={}, failures={}", 
                        stats.isTokenValid(), 
                        stats.getMinutesUntilExpiry(),
                        stats.getTotalRefreshes(),
                        stats.getTotalFailures());
            }
            
        } catch (Exception e) {
            log.debug("Error logging token statistics: {}", e.getMessage());
        }
    }
    
    private AccessToken refreshToken(GoogleCredentials credentials) throws IOException {
        try {
            log.debug("Refreshing Google Calendar access token");
            
            credentials.refresh();
            AccessToken newToken = credentials.getAccessToken();
            
            if (newToken == null) {
                throw new IOException("Token refresh returned null token");
            }
            
            refreshCount.incrementAndGet();
            updateTokenInfo(newToken);
            
            log.info("Successfully refreshed Google Calendar access token (expires: {})", 
                    newToken.getExpirationTime());
            
            return newToken;
            
        } catch (IOException e) {
            failureCount.incrementAndGet();
            log.error("Failed to refresh Google Calendar access token: {}", e.getMessage());
            throw e;
        }
    }
    
    private boolean isTokenExpiringSoon(AccessToken token) {
        if (token == null || token.getExpirationTime() == null) {
            return true;
        }
        
        Date expirationTime = token.getExpirationTime();
        Date bufferTime = new Date(System.currentTimeMillis() + (REFRESH_BUFFER_MINUTES * 60 * 1000));
        
        return expirationTime.before(bufferTime);
    }
    
    private void updateTokenInfo(AccessToken token) {
        if (token != null) {
            currentTokenInfo.set(createTokenInfo(token));
        }
    }
    
    private TokenInfo createTokenInfo(AccessToken token) {
        LocalDateTime expiresAt = null;
        long minutesUntilExpiry = 0;
        boolean needsRefresh = true;
        
        if (token.getExpirationTime() != null) {
            expiresAt = LocalDateTime.ofInstant(
                    token.getExpirationTime().toInstant(), 
                    ZoneId.systemDefault()
            );
            
            long millisUntilExpiry = token.getExpirationTime().getTime() - System.currentTimeMillis();
            minutesUntilExpiry = millisUntilExpiry / (60 * 1000);
            needsRefresh = isTokenExpiringSoon(token);
        }
        
        return TokenInfo.builder()
                .available(true)
                .tokenValue(token.getTokenValue() != null ? "***" + token.getTokenValue().substring(Math.max(0, token.getTokenValue().length() - 4)) : null)
                .expiresAt(expiresAt)
                .minutesUntilExpiry(minutesUntilExpiry)
                .needsRefresh(needsRefresh)
                .lastRefresh(LocalDateTime.now())
                .refreshCount(refreshCount.get())
                .failureCount(failureCount.get())
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TokenInfo {
        private boolean available;
        private String tokenValue; // Masked for security
        private LocalDateTime expiresAt;
        private long minutesUntilExpiry;
        private boolean needsRefresh;
        private LocalDateTime lastRefresh;
        private long refreshCount;
        private long failureCount;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TokenStatistics {
        private boolean tokenAvailable;
        private boolean tokenValid;
        private long totalRefreshes;
        private long totalFailures;
        private LocalDateTime lastRefresh;
        private LocalDateTime expiresAt;
        private long minutesUntilExpiry;
        private boolean needsRefresh;
        private LocalDateTime timestamp = LocalDateTime.now();
    }
}