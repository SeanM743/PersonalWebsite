package com.personal.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringAccessControl {

    /**
     * Check if current user has monitoring access
     */
    public boolean hasMonitoringAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Allow authenticated users to view monitoring data
        return true;
    }

    /**
     * Check if current user has admin access for monitoring
     */
    public boolean hasMonitoringAdminAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if current user can reset metrics
     */
    public boolean canResetMetrics() {
        return hasMonitoringAdminAccess();
    }

    /**
     * Check if current user can trigger health checks
     */
    public boolean canTriggerHealthChecks() {
        return hasMonitoringAdminAccess();
    }

    /**
     * Check if current user can access Prometheus queries
     */
    public boolean canAccessPrometheusQueries() {
        return hasMonitoringAccess();
    }

    /**
     * Get current username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        
        return authentication.getName();
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Log access attempt
     */
    public void logAccessAttempt(String resource, boolean granted) {
        String username = getCurrentUsername();
        if (granted) {
            log.info("Access granted to {} for resource: {}", username, resource);
        } else {
            log.warn("Access denied to {} for resource: {}", username, resource);
        }
    }
}
