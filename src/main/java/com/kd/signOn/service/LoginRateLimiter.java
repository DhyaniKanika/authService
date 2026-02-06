package com.kd.signOn.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 mins

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Attempt attempt = attempts.get(ip);
        if (attempt == null) return false;

        // Expire window
        if (System.currentTimeMillis() - attempt.lastAttempt > WINDOW_MS) {
            attempts.remove(ip);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        Attempt attempt = attempts.getOrDefault(ip, new Attempt());
        attempt.count++;
        attempt.lastAttempt = System.currentTimeMillis();
        attempts.put(ip, attempt);
    }

    public void recordSuccess(String ip) {
        attempts.remove(ip);
    }

    private static class Attempt {
        int count = 0;
        long lastAttempt = System.currentTimeMillis();
    }
}
