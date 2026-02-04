package com.kd.signOn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    // FK → roles.id
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // FK → users.id (who created this user)
    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // FK → users.id (who disabled this user)
    @ManyToOne
    @JoinColumn(name = "disabled_by_user_id")
    private User disabledBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime disabledAt;

    public User() {}

    public User(String email, String passwordHash, Role role, User createdBy) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.enabled = true;
    }

    // Getters & setters

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getDisabledBy() {
        return disabledBy;
    }

    public void setDisabledBy(User disabledBy) {
        this.disabledBy = disabledBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(LocalDateTime disabledAt) {
        this.disabledAt = disabledAt;
    }
}
