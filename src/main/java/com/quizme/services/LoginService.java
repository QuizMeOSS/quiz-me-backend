package com.quizme.services;

import com.quizme.dto.CredentialsLoginRequestDto;
import com.quizme.dto.TokensDto;
import com.quizme.entities.User;
import com.quizme.repos.UserRepo;
import com.quizme.security.JwtUtil;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UserRepo userRepo;
    private final UserCredentialsService userCredentialsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginService(UserRepo userRepo,
                        UserCredentialsService userCredentialsService,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.userCredentialsService = userCredentialsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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

    private TokensDto generateTokensForUser(User user) {
        var accessToken = jwtUtil.generateAccessToken(user.getEmail());
        var refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new TokensDto(accessToken, refreshToken);
    }
}
