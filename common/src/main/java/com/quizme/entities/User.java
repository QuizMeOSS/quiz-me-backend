package com.quizme.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String email;
    private String username;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected User(){}

    public User(String email, String username) {
        this.email = email;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
    public String getUsername() {
        return username;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
