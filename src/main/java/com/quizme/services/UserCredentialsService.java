package com.quizme.services;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.repos.UserCredentialsRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCredentialsService {
    private final UserCredentialsRepo userCredentialsRepo;
    private final PasswordEncoder passwordEncoder;

    public UserCredentialsService(UserCredentialsRepo userCredentialsRepo,
                                  PasswordEncoder passwordEncoder) {
        this.userCredentialsRepo = userCredentialsRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<UserCredentials> findByUserId(User user) {
        return userCredentialsRepo.findByUserId(user);
    }

    /**
     * Create and save UserCredentials for the given user with the provided password.
     * The password is hashed before being stored.
     *
     * @param user     the User entity to associate the credentials with
     * @param password the plaintext password to be hashed and stored
     */
    public void createCredentialsForUser(User user, String password) {
        var userCredentials = new UserCredentials(user, passwordEncoder.encode(password));
        userCredentialsRepo.save(userCredentials);
    }
}
