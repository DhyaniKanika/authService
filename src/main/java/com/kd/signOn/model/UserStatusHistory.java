package com.kd.signOn.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_status_history")
public class UserStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user whose status changed
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // admin who performed the action
    @ManyToOne(optional = false)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(nullable = false)
    private String action; // ENABLED or DISABLED

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column
    private String reason; // optional

    public UserStatusHistory() {}

    public UserStatusHistory(User user, User performedBy, String action) {
        this.user = user;
        this.performedBy = performedBy;
        this.action = action;
        this.performedAt = LocalDateTime.now();
    }

    // getters and setters
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User performedBy) { this.performedBy = performedBy; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
