package com.quizme;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.dto.SsoLoginDto;
import com.quizme.entities.ExternalIdentity;
import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.outbox.OutboxService;
import com.quizme.repos.ExternalIdentityRepo;
import com.quizme.repos.UserRepo;
import com.quizme.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    public static final User USER = new User("email", "u");
    @Mock
    private UserRepo userRepo;
    @Mock
    private UserCredentialsService userCredentialsService;
    @Mock
    ExternalIdentityRepo externalIdentityRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_WHEN_uniqueUsernameAndEmail_THEN_registersNewUser() {
        var request = new RegisterCredentialsRequestDto(USER.getUsername(), USER.getEmail(), "pw");
        when(userRepo.findByEmail(USER.getEmail())).thenReturn(Optional.empty());
        when(userRepo.findByUsername(USER.getUsername())).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        when(userRepo.save(any())).thenAnswer(_ -> USER);
        when(userCredentialsService.createCredentialsForUser(any(), any()))
                .thenReturn(new UserCredentials(USER, "encodedPw"));

        var result = authService.register(request);

        assertEquals(USER.getUsername(), result.success().getUsername());
        assertEquals(USER.getEmail(), result.success().getEmail());
    }

    @Test
    void register_returnsError_whenUsernameExists() {
        var username = "x";
        var existingUser = new User("e", username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(existingUser));

        var result = authService.register(new RegisterCredentialsRequestDto(username, "e", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Username already in use")), result);
    }

    @Test
    void GIVEN_emailAndCredentialsExistAndEmailVerified_WHEN_register_RETURN_alreadyExistsError() {
        var email = "e";
        var existingUser = new User(email, "x");
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(existingUser));
        var existingCredentials = new UserCredentials(existingUser, "pw");
        existingCredentials.setEmailVerified();
        when(userCredentialsService.findByUserId(existingUser)).thenReturn(Optional.of(existingCredentials));

        var result = authService.register(new RegisterCredentialsRequestDto("x", email, "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "This email is already registered")), result);
    }

    @Test
    void GIVEN_emailAndCredentialsExistButEmailNotVerified_WHEN_register_RETURN_tooManyRequestsError() {
        var existingUser = USER;
        when(userRepo.findByEmail(USER.getEmail())).thenReturn(Optional.of(existingUser));
        var existingCredentials = new UserCredentials(existingUser, "pw");
        // simulate email being sent a few moments ago
        existingCredentials.updateLastRequestedConfirmationEmailTimestamp();

        when(userCredentialsService.findByUserId(existingUser)).thenReturn(Optional.of(existingCredentials));

        var result = authService.register(new RegisterCredentialsRequestDto("x", USER.getEmail(), "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.TOO_MANY_REQUESTS,
                "Can't resend confirmation email now, please try again in few minutes")), result);
    }

    @Test
    void register_WHEN_lastEmailOldEnough_THEN_schedulesNewEmail() {
        var existingUser = USER;
        when(userRepo.findByEmail(USER.getEmail())).thenReturn(Optional.of(existingUser));
        var existingCredentials = new UserCredentials(existingUser, "pw");
        when(userCredentialsService.findByUserId(existingUser)).thenReturn(Optional.of(existingCredentials));

        authService.register(new RegisterCredentialsRequestDto("u", USER.getEmail(), "pw"));

        verify(userCredentialsService).scheduleConfirmationEmail(existingCredentials);
    }

    @Test
    void GIVEN_emailExistsButNoCredentials_WHEN_register_THEN_linkCredentialsToUser() {
        var existingEmail = "email";
        var existingUsername = "oldUsername";
        var existingUser = new User(existingEmail, existingUsername);
        when(userRepo.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));
        when(userCredentialsService.findByUserId(existingUser)).thenReturn(Optional.empty());

        when(userCredentialsService.createCredentialsForUser(any(), any()))
                .thenReturn(new UserCredentials(USER, "encodedPw"));

        var result = authService.register(new RegisterCredentialsRequestDto("newUsername", existingEmail, "pw"));

        assertEquals(existingEmail, result.success().getEmail());
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
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(USER));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.empty());

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void GIVEN_emailVerified_WHEN_loginWithIncorrectPass_RETURN_http404() {
        var userCredentials = new UserCredentials(USER, "pw");
        // simulate email is verified
        userCredentials.setEmailVerified();

        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(userCredentials));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void GIVEN_emailNotVerified_WHEN_validLogin_RETURN_http404() {
        var userCredentials = new UserCredentials(USER, "pw");

        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(userCredentials));

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data")), result);
    }

    @Test
    void GIVEN_emailVerified_WHEN_validLogin_RETURN_tokens() {
        var userCredentials = new UserCredentials(USER, "pw");
        // simulate email is verified
        userCredentials.setEmailVerified();

        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        when(userCredentialsService.findByUserId(any())).thenReturn(Optional.of(userCredentials));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        var result = authService.login(new CredentialsLoginRequestDto("email", "pw"));

        assertEquals("access", result.success().accessToken());
        assertEquals("refresh", result.success().refreshToken());
    }

    @Test
    void sso_registersUserAnd3rdPartyIdentity_whenUserNotFound() {
        String userEmail = "anEmail";
        String username = "aName";
        String provider = "x";
        String providerUserId = "123";
        User user = new User(userEmail, username);
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        when(userRepo.save(any())).thenReturn(user);

        // act
        authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, username, provider, providerUserId));

        // assert
        // user is created
        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        assertEquals(userEmail, userCaptor.getValue().getEmail());
        assertEquals(username, userCaptor.getValue().getUsername());

        // 3rd party identity is created
        var identityCaptor = ArgumentCaptor.forClass(ExternalIdentity.class);
        verify(externalIdentityRepo).save(identityCaptor.capture());
        assertEquals(userEmail, identityCaptor.getValue().getProviderUserEmail());
        assertEquals(username, identityCaptor.getValue().getProviderUsername());
        assertEquals(provider, identityCaptor.getValue().getProvider());
        assertEquals(providerUserId, identityCaptor.getValue().getProviderUserId());
    }

    @Test
    void sso_tokenGeneratedForNewUser_whenUserNotFound() {
        String userEmail = "anEmail";
        String username = "aName";
        String provider = "x";
        String providerUserId = "123";
        User user = new User(userEmail, username);
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenReturn(user);
        when(jwtUtil.generateAccessToken(userEmail)).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(userEmail)).thenReturn("refreshToken");

        // act
        var tokens = authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, username, provider, providerUserId));

        // assert
        assertNotNull(tokens.accessToken());
        assertNotNull(tokens.refreshToken());

    }

    @Test
    void sso_registersIdentity_whenUserFoundButNo3rdPartyIdentity() {
        String userEmail = "anEmail";
        String username = "aName";
        String provider = "x";
        String providerUserId = "123";
        User user = new User(userEmail, username);
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(externalIdentityRepo.findByUserId(user)).thenReturn(List.of());


        // act
        authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, username, provider, providerUserId));

        // assert
        var captor = ArgumentCaptor.forClass(ExternalIdentity.class);
        verify(externalIdentityRepo).save(captor.capture());
        assertEquals(userEmail, captor.getValue().getProviderUserEmail());
        assertEquals(username, captor.getValue().getProviderUsername());
        assertEquals(provider, captor.getValue().getProvider());
        assertEquals(providerUserId, captor.getValue().getProviderUserId());
    }

    @Test
    void sso_registersIdentity_whenUserFoundButNo3rdPartyIdentityWithSameProvider() {
        String userEmail = "anEmail";
        String username = "aName";
        String provider = "x";
        String providerUserId = "123";
        User user = new User(userEmail, username);
        ExternalIdentity externalIdentity = new ExternalIdentity(user, "otherProvider", providerUserId, username, userEmail);
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(externalIdentityRepo.findByUserId(user)).thenReturn(List.of(externalIdentity));


        // act
        authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, username, provider, providerUserId));

        // assert
        var captor = ArgumentCaptor.forClass(ExternalIdentity.class);
        verify(externalIdentityRepo).save(captor.capture());
        assertEquals(userEmail, captor.getValue().getProviderUserEmail());
        assertEquals(username, captor.getValue().getProviderUsername());
        assertEquals(provider, captor.getValue().getProvider());
        assertEquals(providerUserId, captor.getValue().getProviderUserId());
    }

    @Test
    void sso_tokenGenerated_whenUserFoundButNo3rdPartyIdentity() {
        String userEmail = "anEmail";
        String username = "aName";
        String provider = "x";
        String providerUserId = "123";
        User user = new User(userEmail, username);
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(externalIdentityRepo.findByUserId(user)).thenReturn(List.of());
        when(jwtUtil.generateAccessToken(userEmail)).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(userEmail)).thenReturn("refreshToken");

        // act
        var tokens = authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, username, provider, providerUserId));

        // assert
        assertNotNull(tokens.accessToken());
        assertNotNull(tokens.refreshToken());

    }

    @Test
    void sso_tokenGenerated_whenUserAnd3rdPartyIdentityFound() {
        String userEmail = "anEmail";
        String username = "aName";
        String provider = "x";
        String providerUserId = "123";
        User user = new User(userEmail, username);
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(externalIdentityRepo.findByUserId(user)).thenReturn(List.of(
                new ExternalIdentity(user, provider, providerUserId, username, userEmail)
        ));
        when(jwtUtil.generateAccessToken(userEmail)).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(userEmail)).thenReturn("refreshToken");

        // act
        var tokens = authService.ssoRegisterOrLogin(new SsoLoginDto(userEmail, username, provider, providerUserId));

        // assert
        assertNotNull(tokens.accessToken());
        assertNotNull(tokens.refreshToken());

    }

}
