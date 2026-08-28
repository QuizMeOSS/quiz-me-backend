package com.quizme.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_credentials")
public class UserCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User userId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email_verified", nullable = false)
    private boolean isEmailVerified;

    @Column(name = "last_requested_confirmation_email")
    private LocalDateTime lastRequestedConfirmationEmailTimestamp;

    protected UserCredentials() {}

    public UserCredentials(User userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public User getUser() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified() {
        isEmailVerified = true;
    }

    public LocalDateTime getLastRequestedConfirmationEmailTimestamp() {
        return lastRequestedConfirmationEmailTimestamp;
    }

    public void updateLastRequestedConfirmationEmailTimestamp() {
        this.lastRequestedConfirmationEmailTimestamp = LocalDateTime.now();
    }
}
