package com.personal.backend.service;

import com.personal.backend.model.ConversationContext;
import com.personal.backend.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationContextManager {
    
    private final Map<String, ConversationContext> activeContexts = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> sessionLocks = new ConcurrentHashMap<>();
    private final AtomicInteger activeRequestCount = new AtomicInteger(0);
    
    @Value("${chat.session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;
    
    @Value("${chat.context.max.messages:50}")
    private int maxContextMessages;
    
    @Value("${chat.context.max.tokens:8000}")
    private int maxContextTokens;
    
    @Value("${chat.max.concurrent.requests:100}")
    private int maxConcurrentRequests;
    
    public ConversationContext createContext(String username, Role userRole) {
        if (activeRequestCount.get() >= maxConcurrentRequests) {
            throw new RuntimeException("Maximum concurrent requests exceeded. Please try again later.");
        }
        
        String sessionId = generateSessionId();
        
        ConversationContext context = ConversationContext.builder()
                .sessionId(sessionId)
                .username(username)
                .userRole(userRole)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(sessionTimeoutMinutes))
                .totalTokenCount(0)
                .messageCount(0)
                .build();
        
        // Create session lock for thread-safe operations on this session
        sessionLocks.put(sessionId, new ReentrantReadWriteLock());
        activeContexts.put(sessionId, context);
        
        log.info("Created new conversation context for user: {} with session: {}", username, sessionId);
        return context;
    }
    
    public Optional<ConversationContext> getContext(String sessionId) {
        return getContext(sessionId, false);
    }
    
    public Optional<ConversationContext> getContext(String sessionId, boolean forUpdate) {
        ConversationContext context = activeContexts.get(sessionId);
        
        if (context == null) {
            log.debug("Context not found for session: {}", sessionId);
            return Optional.empty();
        }
        
        if (context.isExpired()) {
            log.info("Context expired for session: {}, removing", sessionId);
            removeContext(sessionId);
            return Optional.empty();
        }
        
        // Acquire appropriate lock based on operation type
        ReentrantReadWriteLock lock = sessionLocks.get(sessionId);
        if (lock != null) {
            if (forUpdate) {
                lock.writeLock().lock();
            } else {
                lock.readLock().lock();
            }
            
            try {
                // Double-check context still exists after acquiring lock
                context = activeContexts.get(sessionId);
                if (context != null && !context.isExpired()) {
                    // Update last accessed time and extend expiration
                    context.updateExpiration(sessionTimeoutMinutes);
                    return Optional.of(context);
                } else {
                    return Optional.empty();
                }
            } finally {
                if (forUpdate) {
                    lock.writeLock().unlock();
                } else {
                    lock.readLock().unlock();
                }
            }
        }
        
        return Optional.empty();
    }
    
    public ConversationContext getOrCreateContext(String sessionId, String username, Role userRole) {
        if (sessionId != null) {
            Optional<ConversationContext> existingContext = getContext(sessionId);
            if (existingContext.isPresent()) {
                return existingContext.get();
            }
        }
        
        // Create new context if session doesn't exist or is expired
        return createContext(username, userRole);
    }
    
    public void updateContext(ConversationContext context) {
        if (context.getSessionId() != null) {
            ReentrantReadWriteLock lock = sessionLocks.get(context.getSessionId());
            if (lock != null) {
                lock.writeLock().lock();
                try {
                    activeContexts.put(context.getSessionId(), context);
                    log.debug("Updated context for session: {}", context.getSessionId());
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }
    
    public void removeContext(String sessionId) {
        ReentrantReadWriteLock lock = sessionLocks.remove(sessionId);
        ConversationContext removed = activeContexts.remove(sessionId);
        
        if (removed != null) {
            log.info("Removed context for session: {} (user: {})", sessionId, removed.getUsername());
        }
        
        // Clean up the lock
        if (lock != null) {
            // Ensure no threads are waiting on this lock
            lock.writeLock().lock();
            try {
                // Lock acquired, safe to remove
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    public void clearUserContexts(String username) {
        activeContexts.entrySet().removeIf(entry -> {
            boolean shouldRemove = username.equals(entry.getValue().getUsername());
            if (shouldRemove) {
                String sessionId = entry.getKey();
                log.info("Cleared context for user: {} session: {}", username, sessionId);
                
                // Clean up the associated lock
                sessionLocks.remove(sessionId);
            }
            return shouldRemove;
        });
    }
    
    public int getActiveContextCount() {
        return activeContexts.size();
    }
    
    public int getActiveRequestCount() {
        return activeRequestCount.get();
    }
    
    public void incrementActiveRequests() {
        activeRequestCount.incrementAndGet();
    }
    
    public void decrementActiveRequests() {
        activeRequestCount.decrementAndGet();
    }
    
    public int getActiveContextCountForUser(String username) {
        return (int) activeContexts.values().stream()
                .filter(context -> username.equals(context.getUsername()))
                .count();
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupExpiredContexts() {
        int initialSize = activeContexts.size();
        
        activeContexts.entrySet().removeIf(entry -> {
            ConversationContext context = entry.getValue();
            boolean isExpired = context.isExpired();
            
            if (isExpired) {
                String sessionId = entry.getKey();
                log.debug("Cleaning up expired context for session: {} (user: {})", 
                         sessionId, context.getUsername());
                
                // Clean up the associated lock
                sessionLocks.remove(sessionId);
            }
            
            return isExpired;
        });
        
        int removedCount = initialSize - activeContexts.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} expired conversation contexts", removedCount);
        }
    }
    
    @Scheduled(fixedRate = 600000) // Run every 10 minutes
    public void logContextStatistics() {
        int totalContexts = activeContexts.size();
        int activeRequests = activeRequestCount.get();
        
        if (totalContexts > 0 || activeRequests > 0) {
            long totalMessages = activeContexts.values().stream()
                    .mapToLong(ConversationContext::getMessageCount)
                    .sum();
            
            long totalTokens = activeContexts.values().stream()
                    .mapToLong(ConversationContext::getTotalTokenCount)
                    .sum();
            
            log.info("Context statistics: {} active contexts, {} active requests, {} total messages, {} total tokens", 
                     totalContexts, activeRequests, totalMessages, totalTokens);
        }
    }
    
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    public int getMaxContextMessages() {
        return maxContextMessages;
    }
    
    public int getMaxContextTokens() {
        return maxContextTokens;
    }
}