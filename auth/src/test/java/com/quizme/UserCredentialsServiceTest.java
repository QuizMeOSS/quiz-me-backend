package com.quizme;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.outbox.OutboxEventTypes;
import com.quizme.outbox.OutboxService;
import com.quizme.repos.UserCredentialsRepo;
import com.quizme.utils.JwtUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCredentialsServiceTest {

    @Mock
    private UserCredentialsRepo userCredentialsRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static AppProperties appProperties;
    @Mock
    private OutboxService outboxService;
    private JwtUtil jwtUtil;

    private UserCredentialsService userCredentialsService;

    private final User sampleUser = new User("test@example.com", "tester");

    @BeforeAll
    static void setupClass() throws IOException {
        appProperties = UnitTestUtils.initAppProperties();
    }

    @BeforeEach
    void setup() {
        jwtUtil = new JwtUtil(appProperties);

        userCredentialsService = new UserCredentialsService(userCredentialsRepo, passwordEncoder,
                outboxService, jwtUtil);
    }

    @Test
    void findByUserId_returnsPresentOptional_whenRepoReturnsValue() {
        var credentials = new UserCredentials(sampleUser, "hashed");
        when(userCredentialsRepo.findByUserId(sampleUser)).thenReturn(Optional.of(credentials));

        Optional<UserCredentials> result = userCredentialsService.findByUserId(sampleUser);

        // verify service layer didn't alter the returned value
        assertSame(credentials, result.get());
        // verify interaction with the repo
        verify(userCredentialsRepo, times(1)).findByUserId(sampleUser);
    }

    @Test
    void findByUserId_returnsEmptyOptional_whenRepoReturnsEmpty() {
        when(userCredentialsRepo.findByUserId(sampleUser)).thenReturn(Optional.empty());

        Optional<UserCredentials> result = userCredentialsService.findByUserId(sampleUser);

        // verify service layer didn't alter the returned value
        assertTrue(result.isEmpty());
        // verify interaction with the repo
        verify(userCredentialsRepo, times(1)).findByUserId(sampleUser);
    }

    @Test
    void createCredentialsForUser_hashesPassword() {
        String plain = "mySecret";
        String hashed = "hashedSecret";
        when(passwordEncoder.encode(plain)).thenReturn(hashed);

        var createdCredentials = userCredentialsService.createCredentialsForUser(sampleUser, plain);

        var credentialsPassword = createdCredentials.getPassword();
        assertEquals(hashed, credentialsPassword);
    }

    @Test
    void createCredentialsForUser_linksCredentialsToUser() {
        var createdCredentials = userCredentialsService.createCredentialsForUser(sampleUser, "pw");

        var linkedUser = createdCredentials.getUser();

        assertSame(sampleUser, linkedUser);
    }

    @Test
    void WHEN_scheduleConfirmationEmail_THEN_payloadContainsTokenAndEmail() {
        // arrange
        var credentials = new UserCredentials(sampleUser, "hashed");
        when(userCredentialsRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var captor = ArgumentCaptor.forClass(Map.class);

        // act
        userCredentialsService.scheduleConfirmationEmail(credentials);

        // assert
        verify(outboxService).saveEvent(eq(OutboxEventTypes.SIGN_UP), captor.capture());
        var payload = captor.getValue();
        assertEquals(sampleUser.getEmail(), payload.get("email"));
        assertNotNull(payload.get("confirmationToken"));
    }

    @Test
    void WHEN_scheduleConfirmationEmail_THEN_tokenSubjectIsUserCredentialsId() {
        var credentials = new UserCredentials(sampleUser, "hashed");
        ReflectionTestUtils.setField(credentials, "id", 5L);
        when(userCredentialsRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var captor = ArgumentCaptor.forClass(Map.class);

        userCredentialsService.scheduleConfirmationEmail(credentials);

        verify(outboxService).saveEvent(eq(OutboxEventTypes.SIGN_UP), captor.capture());
        var tokenSubject = jwtUtil.getTokenSubject((String) captor.getValue().get("confirmationToken"));
        assertEquals(String.valueOf(5), tokenSubject);
    }

    @Test
    void WHEN_scheduleConfirmationEmail_THEN_updateLastRequestedEmailTimestamp() {
        // arrange
        var credentials = new UserCredentials(sampleUser, "hashed");
        AtomicReference<LocalDateTime> lastRequestedEmailTime = new AtomicReference<>();
        assertNull(credentials.getLastRequestedConfirmationEmailTimestamp());
        when(userCredentialsRepo.save(any())).thenAnswer(inv -> {
            UserCredentials savedCredentials = inv.getArgument(0);
            lastRequestedEmailTime.set(savedCredentials.getLastRequestedConfirmationEmailTimestamp());
            return savedCredentials;
        });

        // act
        userCredentialsService.scheduleConfirmationEmail(credentials);

        // assert
        assertNotNull(lastRequestedEmailTime.get());

    }
}
