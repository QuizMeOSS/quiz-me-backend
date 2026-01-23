package com.quizme.controllers;

import com.quizme.dto.RegisterCredentialsRequestDto;
import com.quizme.entities.User;
import com.quizme.mappers.ResultToResponseEntityMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@WebMvcTest(RegistrationController.class)
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class RegistrationControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private RegistrationService service;

    @Test
    void register() {
        var requestDto = new RegisterCredentialsRequestDto("u", "e", "pw");
        var createdUser = new User("e", "u");
        var result = Result.success(createdUser);
        when(service.register(requestDto)).thenReturn(result);
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