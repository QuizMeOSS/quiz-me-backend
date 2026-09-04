package com.quizme;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.outbox.OutboxEventTypes;
import com.quizme.outbox.OutboxService;
import com.quizme.repos.UserCredentialsRepo;
import com.quizme.utils.JwtUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class UserCredentialsService {
    private static final long EMAIL_CONFIRMATION_LINK_DURATION = 86_400_000; // 24 hours
    private final UserCredentialsRepo userCredentialsRepo;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;
    private final JwtUtil jwtUtil;

    public UserCredentialsService(UserCredentialsRepo userCredentialsRepo,
                                  PasswordEncoder passwordEncoder,
                                  OutboxService outboxService,
                                  JwtUtil jwtUtil) {
        this.userCredentialsRepo = userCredentialsRepo;
        this.passwordEncoder = passwordEncoder;
        this.outboxService = outboxService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void verifyEmail(long credentialsId) {
        userCredentialsRepo.verifyEmail(credentialsId);
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
        userCredentials = userCredentialsRepo.save(userCredentials); // get row id
        var confirmationToken = generateEmailConfirmationToken(
                userCredentials.getId()
        );
        userCredentials.updateLastRequestedConfirmationEmailTimestamp();
        var payload = Map.of(
                "email", userCredentials.getUser().getEmail(),
                "confirmationToken", confirmationToken
        );
        outboxService.saveEvent(OutboxEventTypes.SIGN_UP, payload);
    }

    @NonNull
    private String generateEmailConfirmationToken(long credentialsId) {
        return jwtUtil.generateToken(String.valueOf(credentialsId),
                EMAIL_CONFIRMATION_LINK_DURATION);
    }
}
