package com.kd.signOn.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 mins

    private static final int USER_MAX_FAILURES = 3;
    private static final long USER_WINDOW_MS = 5 * 60 * 1000;

    // Admin cooldown instead of disable
    private static final long ADMIN_COOLDOWN_MS = 5 * 60 * 1000;

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger("SECURITY_AUDIT");

 
    private final Map<String, Attempt> ipAttempts = new ConcurrentHashMap<>();
    private final Map<Long, Attempt> userFailures = new ConcurrentHashMap<>();
    private final Map<Long, Long> adminCooldownUntil = new ConcurrentHashMap<>();


    //============================================================
    // IP RATE LIMITING
    //============================================================

    /**
     * Check if an IP address is blocked
     */
    public boolean isIpBlocked(String ip) {
        Attempt attempt = ipAttempts.get(ip);
        if (attempt == null) return false;

        if (System.currentTimeMillis() - attempt.lastAttempt > WINDOW_MS) {
            SECURITY_LOG.info("IP block expired for ip={}", ip);
            ipAttempts.remove(ip);
            return false;
        }

        if (attempt.count == MAX_ATTEMPTS) {
            SECURITY_LOG.warn("IP blocked ip={} attempts={}", ip, attempt.count);
        }

        return attempt.count >= MAX_ATTEMPTS;
    }

    /**
     * Record a login attempt from an IP address
     */
    public void recordIpAttempt(String ip) {
        Attempt attempt = ipAttempts.getOrDefault(ip, new Attempt());
        attempt.count++;
        attempt.lastAttempt = System.currentTimeMillis();
        ipAttempts.put(ip, attempt);
    }

    // ============================================================
    // USER FAILURE RATE LIMITING
    // ============================================================

    /**
     * @return true if account should be persistently disabled (regular user) or put on cooldown (admin) after crossing thresholds of login failures
     */
    public boolean recordUserFailure(Long userId, boolean isAdmin) {

        long now = System.currentTimeMillis();

        Attempt attempt = userFailures.getOrDefault(userId, new Attempt());

        if (now - attempt.lastAttempt > USER_WINDOW_MS) {
            attempt.count = 0;
        }

        attempt.count++;
        attempt.lastAttempt = now;

        userFailures.put(userId, attempt);

        // Log once when threshold is hit
        if (attempt.count == USER_MAX_FAILURES) {

            if (isAdmin) {
                long until = now + ADMIN_COOLDOWN_MS;
                adminCooldownUntil.put(userId, until);

                SECURITY_LOG.error(
                        "Admin cooldown started userId={} until={}",
                        userId, Instant.ofEpochMilli(until)
                );
                return false;
            }

            SECURITY_LOG.error("User lock threshold reached userId={}", userId);
            return true;
        }
        return false;
    }

    /**
     * Called when a user login is successful if threshold is not reached before successful login
     */
    public void recordUserSuccess(Long userId) {
        userFailures.remove(userId);
        adminCooldownUntil.remove(userId);
    }

    /**
     * Reset all tracking for a user when admin reenables account or for an admin account when cooldown expires.
     */
    public void resetUser(Long userId) {
        SECURITY_LOG.info("user {} enabled", userId);
        userFailures.remove(userId);
        adminCooldownUntil.remove(userId);
    }

    /**
     * Check if an admin user is on cooldown
     */
    public boolean isAdminCoolingDown(Long userId) {
        Long until = adminCooldownUntil.get(userId);
        if (until == null) return false;

        if (System.currentTimeMillis() > until) {
            SECURITY_LOG.info("Admin cooldown expired userId={}", userId);
            adminCooldownUntil.remove(userId);
            return false;
        }

        return true;
    }

    /**
     * Extend the cooldown period for an admin user if the user logs in while
     * currently on cooldown and the maximum cooldown period has not been reached.
     */
    public void extendAdminCooldown(Long userId) {
        long now = System.currentTimeMillis();
        long newUntil = now + ADMIN_COOLDOWN_MS;

        adminCooldownUntil.put(userId, newUntil);

        SECURITY_LOG.error(
                "Admin cooldown extended userId={} until={}",
                userId, Instant.ofEpochMilli(newUntil)
        );
    }

    //============================================================
    // Attempt Class
    //============================================================
    private static class Attempt {
        int count = 0;
        long lastAttempt = System.currentTimeMillis();
    }
}
