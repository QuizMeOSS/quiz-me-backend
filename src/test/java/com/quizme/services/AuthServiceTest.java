package com.quizme.services;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.repos.UserRepo;
import com.quizme.security.JwtUtil;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepo userRepo;
    @Mock
    private UserCredentialsService userCredentialsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_registersNewUser_whenUniqueUsernameAndEmail(){
        var request = new RegisterCredentialsRequestDto("u", "e", "pw");
        when(userRepo.findByEmail("e")).thenReturn(Optional.empty());
        when(userRepo.findByUsername("u")).thenReturn(Optional.empty());
        // simulate db transaction successful
        when(transactionTemplate.execute(any())).thenAnswer(_ -> new User("e", "u"));

        var result = authService.register(request);

        assertEquals("u", result.success().getUsername());
        assertEquals("e", result.success().getEmail());
    }

    @Test
    void register_returnsError_whenUsernameExists(){
        var username = "x";
        var existingUser = new User("e", username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(existingUser));

        var result = authService.register(new RegisterCredentialsRequestDto(username, "e", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Username already in use")), result);
    }

    @Test
    void register_returnsError_whenEmailAndCredentialsExist(){
        var email = "e";
        var existingUser = new User(email, "x");
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userCredentialsService.findByUserId(existingUser)).thenReturn(Optional.of(new UserCredentials(existingUser, "pw")));

        var result = authService.register(new RegisterCredentialsRequestDto("x", email, "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "This email is already registered")), result);
    }

    @Test
    void register_linksCredentialsToUser_whenEmailExistsButNoCredentials(){
        var email = "e";
        var existingUsername = "oldUsername";
        var existingUser = new User(email, existingUsername);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userCredentialsService.findByUserId(existingUser)).thenReturn(Optional.empty());

        var result = authService.register(new RegisterCredentialsRequestDto("newUsername", email, "pw"));

        assertEquals(email, result.success().getEmail());
        // existing username should not be overridden by the new username, we should ignore the new username.
        assertEquals(existingUsername, result.success().getUsername());
    }

    @Test
    void login_returnsFailure_whenEmailDoesntExist() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void login_returnsFailure_whenNoCredentials() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.empty());

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void login_returnsFailure_whenIncorrectPassword() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(mock(UserCredentials.class)));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void login_returnsTokens_whenValidLogin() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(mock(UserCredentials.class)));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals("access", result.success().accessToken());
        assertEquals("refresh", result.success().refreshToken());
    }
}
