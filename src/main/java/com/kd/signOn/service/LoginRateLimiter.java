package com.kd.signOn.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 mins

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("security");

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Attempt attempt = attempts.get(ip);
        if (attempt == null) return false;

        // Expire window
        if (System.currentTimeMillis() - attempt.lastAttempt > WINDOW_MS) {
            SECURITY_LOG.info("Login attempt block for IP " + ip + " has expired");
            attempts.remove(ip);
            return false;
        }
        if (attempt.count == MAX_ATTEMPTS) {
            SECURITY_LOG.warn("Login attempts blocked for IP " + ip);
        }
        return attempt.count >= MAX_ATTEMPTS;
    }

    public void recordIPAttempt(String ip) {
        Attempt attempt = attempts.getOrDefault(ip, new Attempt());
        attempt.count++;
        attempt.lastAttempt = System.currentTimeMillis();
        attempts.put(ip, attempt);
    }

    private static class Attempt {
        int count = 0;
        long lastAttempt = System.currentTimeMillis();
    }
}
