package com.kd.signOn.controller;

import com.kd.signOn.model.User;
import com.kd.signOn.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import org.slf4j.Logger;


@Controller
public class AuthController {

    private final AuthService authService;

    // Security audit logger
    private static final Logger SECURITY_LOG =
        LoggerFactory.getLogger("SECURITY_AUDIT");

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Login page
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Login action
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {

        try {
            String ip = request.getRemoteAddr();
            User user = authService.authenticate(email, password, ip);

            // Create authentication token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user.getId(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
                    );

            // Store in security context
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authToken);

            // Store in session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            SECURITY_LOG.info("User {} logged in from IP {}", user.getId(), ip);

            // Check if password change is required
            if (user.isPasswordChangeRequired()) {
                return "redirect:/change-password";
            }

            // Redirect based on role
            if (user.getRole().getName().equals("ADMIN")) {
                return "redirect:/admin";
            } else {
                return "redirect:/landing";
            }

        } catch (RuntimeException ex) {
            SECURITY_LOG.warn("Failed login attempt for user {}: {}", email, ex.getMessage());
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
    }


    @GetMapping("/landing")
    public String landingPage() {
        return "landing"; // loads landing.html
    }

    @GetMapping("/account")
    public String accountPage( Model model) {
        User user = authService.getCurrentUser();

        model.addAttribute("name", user.getName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole().getName());

        return "account"; // loads account.html
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {

        SECURITY_LOG.info("User with id {} logged out from IP {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal(), request.getRemoteAddr());
        // Invalidate HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        return "redirect:/logout-success";
    }

    // Access Denied page
    @GetMapping("/access-denied")
        public String accessDenied() {
            return "accessDenied";
    }

    // Change Password page
    @GetMapping("/change-password")
        public String changePasswordPage() {
            return "changePassword";
    }

    //Logout success page
    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "logoutSuccess";
    }

    // Change Password action
   @PostMapping("/change-password")
        public String changePassword(@RequestParam String password,
                                    HttpServletRequest request,
                                    Model model) {

            Long userId = Long.valueOf(
                    SecurityContextHolder.getContext()
                            .getAuthentication()
                            .getName()
            );

            try {
                authService.changePassword(password);

                SECURITY_LOG.warn("Password successfully changed for userId={}", userId);

                // force re-login
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();

                return "redirect:/login?passwordChanged";

            } catch (IllegalArgumentException ex) {
                // business rule violation (like reuse)
                SECURITY_LOG.warn("Password change rejected for userId={} reason={}", userId, ex.getMessage());

                model.addAttribute("error", ex.getMessage());
                return "changePassword";

            } catch (Exception ex) {
                SECURITY_LOG.error("Password change failed unexpectedly for userId={}", userId);

                model.addAttribute("error", "Failed to change password");
                return "changePassword";
            }
        }

}

