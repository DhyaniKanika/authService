package com.kd.signOn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import com.kd.signOn.service.PasswordChangeRequiredFilter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.kd.signOn.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

        @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Public endpoints (intranet)
            .requestMatchers("/login", "/logout", "/css/**", "/styles.css").permitAll()

            // Role-based access
            .requestMatchers("/admin/**", "/h2-console/**").hasRole("ADMIN")
            .requestMatchers("/landing/**").hasAnyRole("ADMIN", "USER")

            // Password change
            .requestMatchers("/change-password").authenticated()

            // Account management
            .requestMatchers("/account/**").authenticated()

            // Everything requires authentication
            .anyRequest().authenticated()
        )
        // Custom filters to force changing password securely
        .addFilterBefore(
            new PasswordChangeRequiredFilter(userRepository),
            AuthorizationFilter.class
        )
        // exception handling
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
            response.sendRedirect("/login");
        })
        // gracefully handle access denied
        .accessDeniedPage("/access-denied")
        )
        // session cleanup is a must at logout
        .logout(logout -> logout
            .logoutUrl("/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessUrl("/login?logout")
        )
        // CSRF protection
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/h2-console/**")
        )
        //
        .headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin() // for H2 console (dev only)
            .httpStrictTransportSecurity(hsts -> hsts
            .includeSubDomains(true)
            .maxAgeInSeconds(31536000)
        ))); 

    return http.build();
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
