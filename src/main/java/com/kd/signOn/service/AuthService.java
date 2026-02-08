package com.kd.signOn.service;

import com.kd.signOn.model.User;
import com.kd.signOn.repository.UserRepository;

import java.time.LocalDateTime;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;


@Service
public class AuthService {
    private static final Logger SECURITY_LOG =
        LoggerFactory.getLogger("SECURITY_AUDIT");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
    }

    //============================================================
    // AUTHENTICATION LOGIC
    //============================================================

    /**
     * Authenticate a user
     */
    public User authenticate(String email, String password, String clientIp) {

    // validate input
    ValidationService.validateEmail(email);
    ValidationService.validatePassword(password);

    // IP rate limiting
    if (rateLimiter.isIpBlocked(clientIp)) {
        SECURITY_LOG.warn("Login failed for email={} ip={} reason=ip_rate_limiting",
                email, clientIp);
        throw new RuntimeException("Invalid credentials");
    }

    Optional<User> optionalUser = userRepository.findByEmail(email);

    if (optionalUser.isEmpty()) {
        SECURITY_LOG.warn("Login failed for email={} ip={} reason=user_not_found",
                email, clientIp);
        rateLimiter.recordIpAttempt(clientIp);
        throw new RuntimeException("Invalid credentials");
    }

    User user = optionalUser.get();

    // Persistent disable
    if (!user.isEnabled() || user.isInactive()) {
        SECURITY_LOG.warn("Login failed for user={} ip={} reason=user_disabled",
                user.getId(), clientIp);
        rateLimiter.recordIpAttempt(clientIp);
        throw new RuntimeException("Invalid credentials");
    }

    // Admin cooldown
    if (user.isAdmin() && rateLimiter.isAdminCoolingDown(user.getId())) {
        rateLimiter.extendAdminCooldown(user.getId());
        SECURITY_LOG.warn("Login failed for user={} ip={} reason=admin_cooldown",
                user.getId(), clientIp);
        rateLimiter.recordIpAttempt(clientIp);
        throw new RuntimeException("Invalid credentials");
    }

    // Password check
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {

        boolean shouldDisable =
                rateLimiter.recordUserFailure(user.getId(), user.isAdmin());

        if (shouldDisable && !user.isAdmin()) {
            user.setEnabled(false);
            userRepository.save(user);
        }

        SECURITY_LOG.warn("Login failed for user={} ip={} reason=invalid_password",
                user.getId(), clientIp);

        rateLimiter.recordIpAttempt(clientIp);
        throw new RuntimeException("Invalid credentials");
    }

    rateLimiter.recordIpAttempt(clientIp);
    rateLimiter.recordUserSuccess(user.getId());

    return user;
}

    /**
     * Change a user's password
     */
    public void changePassword(String newPassword) {

        ValidationService.validatePassword(newPassword);

        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setPasswordChangedAt(LocalDateTime.now());

        userRepository.save(user);
    }

}
