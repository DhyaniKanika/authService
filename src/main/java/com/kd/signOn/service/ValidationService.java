package com.kd.signOn.service;

public class ValidationService {

    // allow only safe characters for email
    private static final String EMAIL_REGEX = "^[A-Za-z0-9@._+-]+$";

    // allow strong but safe passwords
    private static final String PASSWORD_REGEX = "^[A-Za-z0-9!@#$%^&*()_+=-]{8,64}$";

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (!password.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and use only letters, numbers, and these symbols: ! @ # $ % ^ & * ( ) _ + = -");
        }
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (!name.matches("^[a-zA-Z '-]{2,50}$")) {
            throw new RuntimeException("Invalid name");
        }
    }
}
