package com.quizme.services;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.dto.SsoLoginDto;
import com.quizme.dto.TokensDto;
import com.quizme.entities.ExternalIdentity;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.repos.ExternalIdentityRepo;
import com.quizme.repos.UserRepo;
import com.quizme.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final UserCredentialsService userCredentialsService;
    private final ExternalIdentityRepo externalIdentityRepo;
    private final TransactionTemplate transactionTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepo userRepo,
                       UserCredentialsService userCredentialsService,
                       ExternalIdentityRepo externalIdentityRepo,
                       TransactionTemplate transactionTemplate,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.userCredentialsService = userCredentialsService;
        this.externalIdentityRepo = externalIdentityRepo;
        this.transactionTemplate = transactionTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a user with given credentials.<br>
     * The user may already exist as they may have signed up via OAuth.
     * In that case, we link the credentials to the existing user.
     * Otherwise, we create a new user and link the credentials to them.
     *
     * @param request RegisterCredentialsRequestDto containing user registration info
     * @return Result<User> containing the registered user or error information
     */
    public Result<User> register(RegisterCredentialsRequestDto request) {
        var userWithSameEmail = userRepo.findByEmail(request.email());
        if (userWithSameEmail.isPresent()) {
            return handleExistingEmail(request, userWithSameEmail.get());
        }

        var usernameExists = userRepo.findByUsername(request.username()).isPresent();
        if (usernameExists) {
            return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Username already in use"));
        }

        return Result.success(registerNewUserAndCredentials(request));
    }

    /**
     * Handle the case where a user with the same email already exists.
     * Either link credentials to existing user or return an error if credentials already exist.
     *
     * @param request           RegisterCredentialsRequestDto containing user registration info
     * @param userWithSameEmail the existing user with the same email
     * @return Result<User> containing the user or error information
     */
    private Result<User> handleExistingEmail(RegisterCredentialsRequestDto request, User userWithSameEmail) {
        var credentials = userCredentialsService.findByUserId(userWithSameEmail);
        if (credentials.isPresent()) {
            return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "This email is already registered"));
        }
        // Link new credentials to the existing user
        userCredentialsService.createCredentialsForUser(userWithSameEmail, request.password());
        return Result.success(userWithSameEmail);
    }

    private User registerNewUserAndCredentials(RegisterCredentialsRequestDto request) {
        // We use a transaction to ensure both user and credentials are created atomically
        // @Transactional annotation cannot be used because the method is called from within the same class
        // and thus would not be proxied by Spring for transaction management
        // so we use TransactionTemplate instead
        var transactionResult = transactionTemplate.execute((TransactionCallback<Object>) _ -> {
            User user = userRepo.save(new User(request.email(), request.username()));
            userCredentialsService.createCredentialsForUser(user, request.password());
            return user;
        });

        return (User) transactionResult;
    }

    /**
     * Logs user in using email and password
     *
     * @param body {@link CredentialsLoginRequestDto} object containing email and password
     * @return {@link Result} containing tokens or error information.
     */
    public Result<TokensDto> login(CredentialsLoginRequestDto body) {
        var userOptional = userRepo.findByEmail(body.email());
        if (userOptional.isEmpty()) {
            return Result.failure(new Failure(FailureReason.NOT_FOUND, "Incorrect login data"));
        }
        var user = userOptional.get();
        var userCredentialsOptional = userCredentialsService.findByUserId(user);
        if (userCredentialsOptional.isEmpty()) {
            return Result.failure(new Failure(FailureReason.NOT_FOUND,
                    "Incorrect login data"));
        }

        if (!passwordEncoder.matches(body.password(), userCredentialsOptional.get().getPassword())) {
            return Result.failure(new Failure(FailureReason.NOT_FOUND,
                    "Incorrect login data"));
        }

        return Result.success(generateTokensForUser(user));
    }

    public TokensDto ssoRegisterOrLogin(SsoLoginDto loginDto) {
        var existingUserOpt = userRepo.findByEmail(loginDto.email());
        if (existingUserOpt.isEmpty()) {
            existingUserOpt = Optional.of(registerUserAndExternalIdentity(loginDto));
        } else {
            var externalIdentityOpt = externalIdentityRepo.findByUserId(existingUserOpt.get());
            if (externalIdentityOpt.isEmpty()
                    // we need to create a different record for GitHub, Google, etc...
                    || externalIdentityOpt.stream().noneMatch(i -> i.getProvider().equals(loginDto.provider()))) {
                linkExternalIdentityToUser(existingUserOpt.get(), loginDto);
            }
        }
        return generateTokensForUser(existingUserOpt.get());
    }

    private User registerUserAndExternalIdentity(SsoLoginDto dto) {
        // We use a transaction to ensure both user and identity are created atomically
        // @Transactional annotation cannot be used because the method is called from within the same class
        // and thus would not be proxied by Spring for transaction management
        // so we use TransactionTemplate instead
        var transactionResult = transactionTemplate.execute((TransactionCallback<Object>) _ -> {
            User user = userRepo.save(new User(dto.email(), dto.username()));
            externalIdentityRepo.save(new ExternalIdentity(user, dto.provider(),
                    dto.providerUserId(), dto.username(), dto.email()));
            return user;
        });

        return (User) transactionResult;
    }

    private void linkExternalIdentityToUser(User user, SsoLoginDto dto) {
        externalIdentityRepo.save(new ExternalIdentity(user, dto.provider(),
                dto.providerUserId(), dto.username(), dto.email()));
    }

    private TokensDto generateTokensForUser(User user) {
        var accessToken = jwtUtil.generateAccessToken(user.getEmail());
        var refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new TokensDto(accessToken, refreshToken);
    }
}
