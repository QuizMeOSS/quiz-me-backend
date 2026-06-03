package com.quizme.entities;

import jakarta.persistence.*;

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
}
