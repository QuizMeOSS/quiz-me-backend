package com.quizme.controllers;

import com.quizme.entities.User;
import com.quizme.repos.UserRepo;
import com.quizme.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
public class IntegrationTest {
    @Autowired
    protected RestTestClient restTestClient;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    protected JwtUtil jwtUtil;
    // Clean up database after each test
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected User user;
    protected String accessToken;

    @BeforeEach
    void setup(){
        user = userRepo.save(new User("user@email.com", "username"));
        accessToken = jwtUtil.generateAccessToken(user.getEmail());
    }

    @AfterEach
    void resetDatabase() {
        jdbcTemplate.execute("DELETE FROM user_credentials");
        jdbcTemplate.execute("DELETE FROM users");
    }
}
