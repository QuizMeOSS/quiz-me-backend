package com.quizme.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "external_identities")
public class ExternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User userId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "provider_username", nullable = false)
    private String providerUsername;

    @Column(name = "provider_user_email", nullable = false)
    private String providerUserEmail;

    protected ExternalIdentity() {
    }

    public ExternalIdentity(User userId, String provider,
                            String providerUserId,
                            String providerUsername,
                            String providerUserEmail) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerUsername = providerUsername;
        this.providerUserEmail = providerUserEmail;
    }

    public User getUser() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getProviderUsername() {
        return providerUsername;
    }

    public String getProviderUserEmail() {
        return providerUserEmail;
    }
}
