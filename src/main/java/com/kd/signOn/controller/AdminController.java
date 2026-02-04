package com.kd.signOn.controller;

import com.kd.signOn.model.Role;
import com.kd.signOn.model.User;
import com.kd.signOn.model.UserStatusHistory;
import com.kd.signOn.repository.RoleRepository;
import com.kd.signOn.repository.UserRepository;
import com.kd.signOn.repository.UserStatusHistoryRepository;
import com.kd.signOn.service.ValidationService;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserStatusHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserStatusHistoryRepository historyRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Admin landing page
    @GetMapping
    public String adminHome() {
        return "admin";
    }

    // Show create user page
    @GetMapping("/create-user")
    public String createUserPage() {
        return "createUser";
    }

    // Handle create user
    @PostMapping("/create-user")
    public String createUser(@RequestParam String name,@RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             Authentication authentication,
                             Model model) {

        try {
            ValidationService.validateEmail(email);
            ValidationService.validatePassword(password);

            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("User already exists");
            }

            Role userRole = roleRepository.findByName(role)
                    .orElseThrow(() -> new RuntimeException("Invalid role"));

            User admin = userRepository.findByEmail(authentication.getName())
                    .orElseThrow();

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

            historyRepository.save(
                new UserStatusHistory(user, admin, "ENABLED")
            );

            model.addAttribute("success", "User created successfully");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "createUser";
    }

    // Manage users page
    @GetMapping("/manage-users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "manageUsers";
    }

    // Disable user
    @PostMapping("/disable-user")
    public String disableUser(@RequestParam Long userId,
                              Authentication authentication) {

        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        User user = userRepository.findById(userId)
                .orElseThrow();

        user.setEnabled(false);
        userRepository.save(user);

        historyRepository.save(
            new UserStatusHistory(user, admin, "DISABLED")
        );

        return "redirect:/admin/manage-users";
    }

    // Enable user (only if not inactive)
    @PostMapping("/enable-user")
    public String enableUser(@RequestParam Long userId,
                             Authentication authentication,
                             Model model) {

        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        User user = userRepository.findById(userId)
                .orElseThrow();

        if (user.isInactive()) {
            model.addAttribute("error", "User is permanently inactive and cannot be re-enabled");
            return "redirect:/admin/manage-users";
        }

        user.setEnabled(true);
        userRepository.save(user);

        historyRepository.save(
            new UserStatusHistory(user, admin, "ENABLED")
        );

        return "redirect:/admin/manage-users";
    }
    @PostMapping("/inactivate-user")
    public String inactivateUser(@RequestParam Long userId,
                                Authentication authentication) {

        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        User user = userRepository.findById(userId)
                .orElseThrow();

        user.setEnabled(false);
        user.setInactive(true);
        userRepository.save(user);

        historyRepository.save(
            new UserStatusHistory(user, admin, "INACTIVATED")
        );

        return "redirect:/admin/manage-users";
    }

}
