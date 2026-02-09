package com.kd.signOn.config;

import com.kd.signOn.model.Role;
import com.kd.signOn.model.User;
import com.kd.signOn.model.UserStatusHistory;
import com.kd.signOn.repository.RoleRepository;
import com.kd.signOn.repository.UserRepository;
import com.kd.signOn.repository.UserStatusHistoryRepository;
import com.kd.signOn.service.ValidationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.io.Console;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataLoader {
    Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   UserStatusHistoryRepository historyRepository,
                                   PasswordEncoder passwordEncoder) {

        return args -> {

            // Create roles
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

            roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(new Role("USER")));

            // Only bootstrap if no users exist
            if (userRepository.count() == 0) {

                Scanner scanner = new Scanner(System.in);
                Console console = System.console();

                try {
                    System.out.println("=== Initial Admin Setup ===");

                    String name = "admin";
                    String email = "admin@kd.com";

                    String password;

                    while (true) {

                        char[] passwordChars;
                        char[] confirmChars;

                        if (console != null) {
                            passwordChars = console.readPassword("Enter admin password: ");
                            confirmChars = console.readPassword("Confirm admin password: ");
                        } else {
                            System.out.println("WARNING: Console unavailable, password will be visible.");
                            System.out.print("Enter admin password: ");
                            passwordChars = scanner.nextLine().toCharArray();
                            System.out.print("Confirm admin password: ");
                            confirmChars = scanner.nextLine().toCharArray();
                        }

                        String pwd = new String(passwordChars);
                        String confirm = new String(confirmChars);

                        // wipe arrays
                        Arrays.fill(passwordChars, '\0');
                        Arrays.fill(confirmChars, '\0');

                        if (pwd.equals(confirm)) {
                            password = pwd;
                            break;
                        } else {
                            System.out.println("Passwords do not match. Try again.");
                        }
                    }

                try {
                    ValidationService.validatePassword(password);
                } catch (IllegalArgumentException e) {
                    System.out.println("Password validation failed: " + e.getMessage());
                    return;
                }

                User admin = new User();
                admin.setName(name);
                admin.setEmail(email);
                admin.setPasswordHash(passwordEncoder.encode(password));
                admin.setRole(adminRole);
                admin.setEnabled(true);
                admin.setInactive(false);
                admin.setPasswordChangeRequired(false);
                admin.setCreatedAt(LocalDateTime.now());

                // Save first to get ID
                admin = userRepository.save(admin);

                // self-reference createdBy
                admin.setCreatedBy(admin);
                admin = userRepository.save(admin);

                // write audit history
                UserStatusHistory history = new UserStatusHistory(
                        admin,
                        admin,
                        "ENABLED"
                );
                historyRepository.save(history);

                SECURITY_LOG.info("Initial admin user {} created with email {}", admin.getId(), admin.getEmail());
                System.out.println();
                System.out.println("========================================");
                System.out.println(" SignOn installation completed");
                System.out.println("========================================");
                System.out.println();
                System.out.println("Admin account created.");
                System.out.println();
                System.out.println("Login URL:");
                System.out.println("https://localhost:8443/login");
                System.out.println();
                System.out.println(" Admin Username:");
                System.out.println(email);
                System.out.println();
                System.out.println("Use the password you entered during setup.");
                System.out.println();
                System.out.println("========================================");
                System.out.println();


            } finally {
                scanner.close();
            }
            } else {
                return;
            }
               

            
        };
    }
}
