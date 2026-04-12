package com.example.userservice.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterConfig {

    private final Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    public RateLimiter getRateLimiter(String key) {
        return rateLimiterMap.computeIfAbsent(key, k -> RateLimiter.create(100.0));
    }

    public boolean tryAcquire(String key) {
        return getRateLimiter(key).tryAcquire();
    }

    public boolean tryAcquire(String key, int permits) {
        return getRateLimiter(key).tryAcquire(permits);
    }

    public boolean tryAcquire(String key, int permits, long timeout, java.util.concurrent.TimeUnit unit) {
        return getRateLimiter(key).tryAcquire(permits, timeout, unit);
    }
}
