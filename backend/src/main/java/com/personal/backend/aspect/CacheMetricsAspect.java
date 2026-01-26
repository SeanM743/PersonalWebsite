package com.personal.backend.aspect;

import com.personal.backend.service.CacheMetricsService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsAspect {

    private final CacheMetricsService cacheMetricsService;

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        
        if (cacheable == null || cacheable.cacheNames().length == 0) {
            return joinPoint.proceed();
        }
        
        String cacheName = cacheable.cacheNames()[0];
        String operation = "get";
        String key = generateCacheKey(joinPoint, cacheable.key());
        
        Timer.Sample sample = cacheMetricsService.startCacheOperationTimer(cacheName, operation);
        
        try {
            Object result = joinPoint.proceed();
            
            // Check if result was cached (this is a simplification)
            boolean wasHit = result != null;
            
            if (wasHit) {
                cacheMetricsService.recordCacheHit(cacheName, operation, key);
            } else {
                cacheMetricsService.recordCacheMiss(cacheName, operation, key);
            }
            
            cacheMetricsService.stopCacheOperationTimer(sample, cacheName, operation, wasHit);
            
            return result;
        } catch (Exception e) {
            cacheMetricsService.recordCacheMiss(cacheName, operation, key);
            cacheMetricsService.stopCacheOperationTimer(sample, cacheName, operation, false);
            throw e;
        }
    }

    @Around("@annotation(org.springframework.cache.annotation.CachePut)")
    public Object aroundCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        CachePut cachePut = method.getAnnotation(CachePut.class);
        
        if (cachePut == null || cachePut.cacheNames().length == 0) {
            return joinPoint.proceed();
        }
        
        String cacheName = cachePut.cacheNames()[0];
        String operation = "put";
        String key = generateCacheKey(joinPoint, cachePut.key());
        
        Timer.Sample sample = cacheMetricsService.startCacheOperationTimer(cacheName, operation);
        
        try {
            Object result = joinPoint.proceed();
            
            // CachePut always updates the cache
            cacheMetricsService.recordCacheHit(cacheName, operation, key);
            cacheMetricsService.stopCacheOperationTimer(sample, cacheName, operation, true);
            
            return result;
        } catch (Exception e) {
            cacheMetricsService.stopCacheOperationTimer(sample, cacheName, operation, false);
            throw e;
        }
    }

    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object aroundCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        
        if (cacheEvict == null || cacheEvict.cacheNames().length == 0) {
            return joinPoint.proceed();
        }
        
        String cacheName = cacheEvict.cacheNames()[0];
        String operation = "evict";
        String key = generateCacheKey(joinPoint, cacheEvict.key());
        String reason = cacheEvict.allEntries() ? "all_entries" : "single_entry";
        
        Timer.Sample sample = cacheMetricsService.startCacheOperationTimer(cacheName, operation);
        
        try {
            Object result = joinPoint.proceed();
            
            cacheMetricsService.recordCacheEviction(cacheName, reason, key);
            cacheMetricsService.stopCacheOperationTimer(sample, cacheName, operation, true);
            
            return result;
        } catch (Exception e) {
            cacheMetricsService.stopCacheOperationTimer(sample, cacheName, operation, false);
            throw e;
        }
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.getTarget().getClass()
                    .getMethod(joinPoint.getSignature().getName(), 
                              ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                                      .getParameterTypes());
        } catch (NoSuchMethodException e) {
            log.warn("Could not get method for cache metrics", e);
            return null;
        }
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // Simplified key generation - in production, you'd want to use Spring's 
        // KeyGenerator or evaluate the SpEL expression
        if (keyExpression != null && !keyExpression.isEmpty() && !"".equals(keyExpression)) {
            return keyExpression;
        }
        
        // Generate key from method name and parameters
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(joinPoint.getSignature().getName());
        
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            keyBuilder.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) keyBuilder.append(",");
                keyBuilder.append(args[i] != null ? args[i].toString() : "null");
            }
            keyBuilder.append(")");
        }
        
        return keyBuilder.toString();
    }
}