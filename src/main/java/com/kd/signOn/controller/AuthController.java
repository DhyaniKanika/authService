package com.kd.signOn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";   // loads login.html
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
    public String logoutPage() {
        return "logout";  // loads logout.html
    }
}
