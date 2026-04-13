package com.quizme.controllers;

import com.quizme.dto.NewQuestionDto;
import com.quizme.dto.QuestionDto;
import com.quizme.entities.Category;
import com.quizme.entities.Question;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.security.TokenFilter;
import com.quizme.services.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = QuestionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TokenFilter.class))
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class QuestionControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private QuestionService questionService;

    @Test
    @WithMockUser(username = "user@email.com")
    void createQuestion_happyScenario() {
        var user = new User("e", "u");
        Result<Question> result = Result.success(new Question(user,
                "q",
                Set.of(new Category(user, "c1"))));
        when(questionService.createQuestion(any(), any())).thenReturn(result);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        restTestClient.post()
                .uri("/questions")
                .body(new NewQuestionDto("", Set.of(), Set.of()))
                .exchange()
                .expectBody(QuestionDto.class)
                .consumeWith(question -> {
                    assertEquals(0, question.getResponseBody().id()); // id is set by database, so here we get 0
                    assertEquals("q", question.getResponseBody().question());
                    assertEquals(Set.of(0L), question.getResponseBody().categories()); // id is set by database, so here we get 0
                    assertNotNull(question.getResponseBody().createdAt());
                });
    }

    @Test
    @WithMockUser(username = "user@email.com")
    void createQuestion_failureIsMappedToApiError() {
        Result<Question> result = Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "exists"));
        when(questionService.createQuestion(any(), any())).thenReturn(result);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        restTestClient.post()
                .uri("/questions")
                .body(new NewQuestionDto("", Set.of(), Set.of()))
                .exchange();

        // verify the mapper was invoked to map the response to ApiError
        verify(mapper).map(result, "/questions");
    }

}