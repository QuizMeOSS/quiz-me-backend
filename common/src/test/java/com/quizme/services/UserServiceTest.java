package com.quizme.services;

import com.quizme.dto.TokensDto;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.repos.UserRepo;
import com.quizme.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepo userRepo;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private com.quizme.services.UserService userService;

    @Test
    void implementsUserDetailsService() {
        assertInstanceOf(UserDetailsService.class, userService);
    }

    @Test
    void loadUser_throwsException_whenUserNotFound() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("name");
        });
    }

    @Test
    void loadUser_mapsUserObjectToUserDetailsObject() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(
                new User("email", "pw")
        ));

        var userDetails = userService.loadUserByUsername("email");

        var expectedUserDetails = new org.springframework.security.core.userdetails.User(
                "email", "pw", true, true, true, true, Collections.emptyList()
        );

        assertEquals(expectedUserDetails, userDetails);
    }

    @Test
    void refreshToken_returnsFailure_whenRefreshTokenExpired() {
        when(jwtUtil.isExpired(any())).thenReturn(true);

        var result = userService.refreshToken(anyString());

        assertEquals(new Failure(FailureReason.VALIDATION_FAILED, "Refresh token has expired"), result.failure());
    }

    @Test
    void refreshToken_returnsTokens_whenRefreshTokenIsValid() {
        when(jwtUtil.isExpired(any())).thenReturn(false);
        when(jwtUtil.generateAccessToken(any())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refreshToken");
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        var result = userService.refreshToken(anyString());

        assertEquals(new TokensDto("accessToken", "refreshToken"), result.success());
    }
}