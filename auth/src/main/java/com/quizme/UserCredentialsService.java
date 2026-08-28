package com.quizme;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.outbox.OutboxEventTypes;
import com.quizme.outbox.OutboxService;
import com.quizme.repos.UserCredentialsRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class UserCredentialsService {
    private final UserCredentialsRepo userCredentialsRepo;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;

    public UserCredentialsService(UserCredentialsRepo userCredentialsRepo,
                                  PasswordEncoder passwordEncoder,
                                  OutboxService outboxService) {
        this.userCredentialsRepo = userCredentialsRepo;
        this.passwordEncoder = passwordEncoder;
        this.outboxService = outboxService;
    }

    public Optional<UserCredentials> findByUserId(User user) {
        return userCredentialsRepo.findByUserId(user);
    }

    /**
     * Create and UserCredentials for the given user with the provided password.
     * The password is hashed.
     *
     * @param user     the User entity to associate the credentials with
     * @param password the plaintext password to be hashed.
     */
    public UserCredentials createCredentialsForUser(User user, String password) {
        return new UserCredentials(user, passwordEncoder.encode(password));
    }

    @Transactional
    public void scheduleConfirmationEmail(UserCredentials userCredentials) {
        userCredentials.updateLastRequestedConfirmationEmailTimestamp();
        userCredentialsRepo.save(userCredentials);
        var payload = Map.of(
                "email", userCredentials.getUser().getEmail()
        );
        outboxService.saveEvent(OutboxEventTypes.SIGN_UP, payload);
    }
}
