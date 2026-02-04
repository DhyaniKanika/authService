package com.kd.signOn.config;

import com.kd.signOn.model.Role;
import com.kd.signOn.model.User;
import com.kd.signOn.repository.RoleRepository;
import com.kd.signOn.repository.UserRepository;
import com.kd.signOn.service.ValidationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Scanner;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {

        return args -> {

            // Create roles if not exist
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

            roleRepository.findByName("USER")
                    .orElseGet(() -> roleRepository.save(new Role("USER")));

            // Only bootstrap if no users exist
            if (userRepository.count() == 0) {

                Scanner scanner = new Scanner(System.in);

                System.out.println("=== Initial Admin Setup ===");

                System.out.print("Enter admin email: ");
                String email = scanner.nextLine();

                System.out.print("Enter admin password: ");
                String password = scanner.nextLine();

                // Validate input (whitelist rules)
                ValidationService.validateEmail(email);
                ValidationService.validatePassword(password);

                User admin = new User();
                admin.setEmail(email);
                admin.setPasswordHash(passwordEncoder.encode(password));
                admin.setRole(adminRole);
                admin.setEnabled(true);
                admin.setCreatedAt(LocalDateTime.now());

                // save without createdBy
                admin = userRepository.save(admin);

                // self-reference
                admin.setCreatedBy(admin);
                userRepository.save(admin);

                System.out.println("Admin user created successfully.");
            }
        };
    }
}
