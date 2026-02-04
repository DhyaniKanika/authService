package com.kd.signOn.controller;

import com.kd.signOn.model.User;
import com.kd.signOn.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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


@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletRequest request,
                        Model model) {

        try {
            User user = authService.authenticate(email, password);

            // Create authentication token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()))
                    );

            // Store in security context
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authToken);

            // Store in session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

            if (user.getRole().getName().equals("ADMIN")) {
                return "redirect:/admin";
            } else {
                return "redirect:/landing";
            }

        } catch (RuntimeException ex) {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
    }


    @GetMapping("/landing")
    public String landingPage() {
        return "landing"; // loads landing.html
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";   // loads admin.html
    }

    @GetMapping("/mfa")
    public String mfaPage() {
        return "mfa";     // loads mfa.html
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {

        // Invalidate HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        return "redirect:/login";
    }  // loads logout.html
    
}
