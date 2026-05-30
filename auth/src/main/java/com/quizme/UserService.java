package com.quizme;

import com.quizme.dto.TokensDto;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.repos.UserRepo;
import com.quizme.utils.JwtUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepo userRepo;
    private final JwtUtil jwtUtil;

    public UserService(UserRepo userRepo,
                       JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        var userOpt = userRepo.findByEmail(username);
        if (userOpt.isEmpty()) {
            throw UsernameNotFoundException.fromUsername(username);
        }

        var user = userOpt.get();
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .build();
    }

    public Result<TokensDto> refreshToken(String refreshToken) {
        var isExpired = jwtUtil.isExpired(refreshToken);
        if (isExpired) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Refresh token has expired"));
        }
        var userOpt = userRepo.findByEmail(jwtUtil.getUsername(refreshToken));
        return userOpt.map(user -> Result.success(generateTokensForUser(user)))
                // This shouldn't happen, because it means we issued a refresh token for non-existent user
                .orElseGet(() -> Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Couldn't refresh token")));
    }

    private TokensDto generateTokensForUser(User user) {
        var accessToken = jwtUtil.generateAccessToken(user.getEmail());
        var refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new TokensDto(accessToken, refreshToken);
    }
}
