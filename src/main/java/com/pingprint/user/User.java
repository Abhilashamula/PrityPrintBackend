package com.pingprint.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Column(nullable = false)
    private int age;
    @Column(nullable = false, length = 24)
    private String gender;
    @Column(nullable = false, length = 32)
    private String phone;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() { }

    public User(String email, String passwordHash, String displayName, int age, String gender, String phone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.emailVerified = false;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isEnabled() { return enabled; }
    public void verifyEmail() { emailVerified = true; }
}
