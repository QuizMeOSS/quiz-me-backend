package com.quizme;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.repos.UserCredentialsRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCredentialsServiceTest {

    @Mock
    private UserCredentialsRepo userCredentialsRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCredentialsService userCredentialsService;

    private final User sampleUser = new User("test@example.com", "tester");

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
}
