package com.quizme.controllers;

import com.quizme.dto.NewQuizDto;
import com.quizme.dto.QuizDto;
import com.quizme.dto.QuizQuestionDto;
import com.quizme.dto.SubmittedQuizDto;
import com.quizme.entities.User;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.security.TokenFilter;
import com.quizme.services.QuizService;
import com.quizme.services.questionspicker.QuestionsPicker;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = QuizController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TokenFilter.class))
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class QuizControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private QuizService quizService;

    @Test
    @WithMockUser(username = "user@email.com")
    void createQuiz_happyScenario() {
        var user = new User("e", "u");
        var expectedResponse = new QuizDto(1L,
                Set.of(new QuizQuestionDto(1, "q1", Set.of(), Set.of(), LocalDateTime.now()),
                        new QuizQuestionDto(2, "q2", Set.of(), Set.of(), LocalDateTime.now())
                ),
                LocalDateTime.now());
        Result<QuizDto> result = Result.success(expectedResponse);
        when(quizService.createQuiz(any(), any())).thenReturn(result);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(user));

        restTestClient.post()
                .uri("/quiz/new")
                .body(new NewQuizDto(4, QuestionsPicker.Strategy.RANDOM))
                .exchange()
                .expectBody(QuizDto.class)
                .consumeWith(quiz ->
                        assertEquals(expectedResponse, quiz.getResponseBody())
                );
    }

    @Test
    @WithMockUser(username = "user@email.com")
    void createQuiz_failureIsMappedToApiError() {
        Result<QuizDto> result = Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "error"));
        when(quizService.createQuiz(any(), any())).thenReturn(result);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        restTestClient.post()
                .uri("/quiz/new")
                .body(new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM))
                .exchange();

        // verify the mapper was invoked to map the response to ApiError
        verify(mapper).map(result, "/quiz/new");
    }

    @Test
    void submitQuiz_happyScenario() {
        Result<Void> result = Result.success(null);
        when(quizService.submitQuiz(any())).thenReturn(result);

        restTestClient.post()
                .uri("/quiz/submit")
                .body(new SubmittedQuizDto(1, List.of()))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Void.class);
    }

    @Test
    void submitQuiz_failureIsMappedToApiError() {
        Result<Void> result = Result.failure(new Failure(FailureReason.NOT_FOUND, "error"));
        when(quizService.submitQuiz(any())).thenReturn(result);

        restTestClient.post()
                .uri("/quiz/submit")
                .body(new SubmittedQuizDto(1, List.of()))
                .exchange();

        // verify the mapper was invoked to map the response to ApiError
        verify(mapper).map(result, "/quiz/submit");
    }

}