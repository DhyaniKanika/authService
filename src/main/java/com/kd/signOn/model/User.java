package com.kd.signOn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user's name
    @Column(nullable = false)
    private String name;

    // user's email - unique
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    // does the user need to reset their password?
    @Column(nullable = false)
    private boolean passwordChangeRequired = true;

    // timestamp of last password change
    private LocalDateTime passwordChangedAt;

    // user's role
    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    // current login state
    @Column(nullable = false)
    private boolean enabled = true;

    // permanent deactivation (left company)
    @Column(nullable = false)
    private boolean inactive = false;

    // timestamp of user creation
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // who created this user
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy; 

    public User() {}

    // getters & setters

    public Long getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isInactive() { return inactive; }
    public void setInactive(boolean inactive) { this.inactive = inactive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }
    
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

}
