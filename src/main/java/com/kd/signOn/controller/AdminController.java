package com.kd.signOn.controller;

import com.kd.signOn.model.Role;
import com.kd.signOn.model.User;
import com.kd.signOn.model.UserStatusHistory;
import com.kd.signOn.repository.RoleRepository;
import com.kd.signOn.repository.UserRepository;
import com.kd.signOn.repository.UserStatusHistoryRepository;
import com.kd.signOn.service.ValidationService;
import com.kd.signOn.service.LoginRateLimiter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger SECURITY_LOG =
        LoggerFactory.getLogger("SECURITY_AUDIT");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserStatusHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserStatusHistoryRepository historyRepository,
                           PasswordEncoder passwordEncoder,
                           LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
    }

    // ============================================================
    // Admin landing page
    // ============================================================

    @GetMapping
    public String adminHome() {
        return "admin";
    }

    // ============================================================
    // Create User
    // ============================================================

    @GetMapping("/create-user")
    public String createUserPage() {
        return "createUser";
    }

    @PostMapping("/create-user")
    public String createUser(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             Authentication authentication,
                             Model model) {
        // Check if user has admin role
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            SECURITY_LOG.warn("Unauthorized create-user attempt by {}", authentication.getName());
            return "redirect:/access-denied";
        }

        try {
            ValidationService.validateEmail(email);
            ValidationService.validatePassword(password);

            if ("ADMIN".equalsIgnoreCase(role)) {
                throw new RuntimeException("Invalid role");
            }

            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("User already exists");
            }

            Role userRole = roleRepository.findByName(role)
                    .orElseThrow(() -> new RuntimeException("Invalid role"));

            Long adminId = Long.valueOf(authentication.getName());
            User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(userRole);
            user.setEnabled(true);
            user.setInactive(false);
            user.setCreatedAt(LocalDateTime.now());
            user.setCreatedBy(admin);

            userRepository.save(user);

            SECURITY_LOG.info("Admin {} created user {}", admin.getId(), user.getId());

            historyRepository.save(
                new UserStatusHistory(user, admin, "ENABLED")
            );

            model.addAttribute("success", "User created successfully");

        } catch (Exception e) {
            SECURITY_LOG.error("Create user failed by {} reason={}",
                    authentication.getName(), e.getMessage());

            model.addAttribute("error", "Unable to create user");
        }

        return "createUser";
    }

    // ============================================================
    // Manage Users
    // ============================================================

    @GetMapping("/manage-users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userRepository.findByRole_NameNot("ADMIN"));
        return "manageUsers";
    }

    // ============================================================
    // Disable User
    // ============================================================

    @PostMapping("/disable-user")
    public String disableUser(@RequestParam Long userId,
                              Authentication authentication) {

        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            SECURITY_LOG.warn("Unauthorized disable attempt by {}", authentication.getName());
            return "redirect:/access-denied";
        }

        Long adminId = Long.valueOf(authentication.getName());
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);

        SECURITY_LOG.warn("Admin {} disabled user {}", admin.getId(), user.getId());

        historyRepository.save(
            new UserStatusHistory(user, admin, "DISABLED")
        );

        return "redirect:/admin/manage-users";
    }

    // ============================================================
    // Enable User
    // ============================================================

    @PostMapping("/enable-user")
    public String enableUser(@RequestParam Long userId,
                             Authentication authentication,
                             Model model) {

        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            SECURITY_LOG.warn("Unauthorized enable attempt by {}", authentication.getName());
            return "redirect:/access-denied";
        }

        Long adminId = Long.valueOf(authentication.getName());
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (user.isInactive()) {
            model.addAttribute("error", "User is permanently inactive and cannot be re-enabled");
            return "redirect:/admin/manage-users";
        }

        user.setEnabled(true);
        userRepository.save(user);

        // Reset rate limiting state after recovery
        rateLimiter.resetUser(user.getId());

        SECURITY_LOG.warn("Admin {} enabled user {}", admin.getId(), user.getId());

        historyRepository.save(
            new UserStatusHistory(user, admin, "ENABLED")
        );

        return "redirect:/admin/manage-users";
    }

    // ============================================================
    // Inactivate User (permanent)
    // ============================================================

    @PostMapping("/inactivate-user")
    public String inactivateUser(@RequestParam Long userId,
                                 Authentication authentication) {

        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            SECURITY_LOG.warn("Unauthorized inactivate attempt by {}", authentication.getName());
            return "redirect:/access-denied";
        }

        Long adminId = Long.valueOf(authentication.getName());
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        user.setEnabled(false);
        user.setInactive(true);
        userRepository.save(user);

        SECURITY_LOG.warn("Admin {} inactivated user {}", admin.getId(), user.getId());

        historyRepository.save(
            new UserStatusHistory(user, admin, "INACTIVATED")
        );

        return "redirect:/admin/manage-users";
    }
}
