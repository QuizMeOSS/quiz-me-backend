package com.quizme.services;

import com.quizme.dto.CredentialsLoginRequestDto;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {
    @Mock
    private UserRepo userRepo;
    @Mock
    private UserCredentialsService userCredentialsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private LoginService loginService;

    @Test
    void login_returnsFailure_whenEmailDoesntExist() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());

        var result = loginService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void login_returnsFailure_whenNoCredentials() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.empty());

        var result = loginService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void login_returnsFailure_whenIncorrectPassword() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(mock(UserCredentials.class)));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        var result = loginService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void login_returnsTokens_whenValidLogin() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(mock(UserCredentials.class)));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        var result = loginService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals("access", result.success().accessToken());
        assertEquals("refresh", result.success().refreshToken());
    }
}