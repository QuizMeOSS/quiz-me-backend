package com.quizme.controllers;

import com.quizme.config.AppProperties;
import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.entities.User;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.security.JwtUtil;
import com.quizme.services.LoginService;
import com.quizme.services.RegistrationService;
import com.quizme.services.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class AuthControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private RegistrationService registrationService;
    @MockitoBean
    private LoginService loginService;
    @MockitoBean
    private AppProperties appProperties;
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void register() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");
        var createdUser = new User("e", "u");
        var result = Result.success(createdUser);
        when(registrationService.register(requestDto)).thenReturn(result);
        when(mapper.map(result, "/register"))
                .thenAnswer(_ ->
                        ResponseEntity.ok(createdUser)
                );

        restTestClient.post()
                .uri("/register")
                .body(requestDto)
                .exchange()
                .expectBody(User.class)
                .consumeWith(user -> {
                    assertEquals("e", user.getResponseBody().getEmail());
                    assertEquals("u", user.getResponseBody().getUsername());
                });
    }
}