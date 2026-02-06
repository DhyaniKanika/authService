package com.kd.signOn.service;

import com.kd.signOn.model.User;
import com.kd.signOn.repository.UserRepository;

import java.time.LocalDateTime;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
    }

    public User authenticate(String email, String password, String clientIp) {

        // validate input
        ValidationService.validateEmail(email);
        ValidationService.validatePassword(password);

        //rate limiting
        if (rateLimiter.isBlocked(clientIp)) {
            throw new RuntimeException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isEnabled() || user.isInactive()) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            rateLimiter.recordFailure(clientIp);
            throw new RuntimeException("Invalid credentials");
        }

        rateLimiter.recordSuccess(clientIp);
        return user;
    }
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
