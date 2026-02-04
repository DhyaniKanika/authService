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

@Configuration
public class DataLoader {

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

                System.out.println("=== Initial Admin Setup ===");

                System.out.print("Enter admin name: ");
                String name = scanner.nextLine();

                System.out.print("Enter admin email: ");
                String email = scanner.nextLine();

                System.out.print("Enter admin password: ");
                String password = scanner.nextLine();

                ValidationService.validateName(name);
                ValidationService.validateEmail(email);
                ValidationService.validatePassword(password);

                User admin = new User();
                admin.setName(name);
                admin.setEmail(email);
                admin.setPasswordHash(passwordEncoder.encode(password));
                admin.setRole(adminRole);
                admin.setEnabled(true);
                admin.setInactive(false);
                admin.setCreatedAt(LocalDateTime.now());

                // save first (no createdBy yet)
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

                System.out.println("Admin user created successfully.");
            }
        };
    }
}
