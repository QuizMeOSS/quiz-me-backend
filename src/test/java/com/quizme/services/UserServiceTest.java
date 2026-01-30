package com.quizme.services;

import com.quizme.entities.User;
import com.quizme.repos.UserRepo;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    @Test
    void testIsUserDetailsService() {
        assertInstanceOf(UserDetailsService.class, userService);
    }

    @Test
    void testExceptionThrownIfUserNotFound() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("name");
        });
    }

    @Test
    void testUserObjectIsMappedToUserDetails() {
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(
                new User("email", "pw")
        ));

        var userDetails = userService.loadUserByUsername("email");

        var expectedUserDetails = new org.springframework.security.core.userdetails.User(
                "email", "pw", true, true, true, true, Collections.emptyList()
        );

        assertEquals(expectedUserDetails, userDetails);
    }
}