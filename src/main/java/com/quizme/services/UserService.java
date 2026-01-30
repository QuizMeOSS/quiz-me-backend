package com.quizme.services;

import com.quizme.repos.UserRepo;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
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
}
