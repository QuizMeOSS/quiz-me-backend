package com.quizme.controllers;

import com.quizme.dto.*;
import com.quizme.entities.Quiz;
import com.quizme.repos.QuizRepo;
import com.quizme.services.CategoryService;
import com.quizme.services.QuestionService;
import com.quizme.services.QuizService;
import com.quizme.services.questionspicker.QuestionsPicker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuizControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizRepo quizRepo;

    private final QuestionChoiceDto choice = new QuestionChoiceDto("c1", true);

    @AfterEach
    @Override
    void resetDatabase() {
        super.resetDatabase();
        jdbcTemplate.execute("DELETE FROM questions_categories");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM quizzes");
        jdbcTemplate.execute("DELETE FROM quizzes_questions");
        jdbcTemplate.execute("DELETE FROM quizzes_choices");
        // reset id sequence
        jdbcTemplate.execute("ALTER TABLE categories ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE questions ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE quizzes ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    void createQuiz_quizCreated_WHEN_sufficientQuestions() {
        var newQ1Dto = new NewQuestionDto("q1", Set.of(choice), Set.of(1L));
        var newQ2Dto = new NewQuestionDto("q2", Set.of(choice), Set.of(1L));
        var q1Dto = new QuestionDto(1, newQ1Dto.question(), newQ1Dto.choices(), newQ1Dto.categories(), LocalDateTime.now());
        var q2Dto = new QuestionDto(2, newQ2Dto.question(), newQ2Dto.choices(), newQ2Dto.categories(), LocalDateTime.now());
        categoryService.createCategory(new NewCategoryDto("new"), user);
        questionService.createQuestion(newQ1Dto, user);
        questionService.createQuestion(newQ2Dto, user);

        var requestDto = new NewQuizDto(2, QuestionsPicker.Strategy.RANDOM);

        restTestClient.post()
                .uri("/quiz/new")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .exchange()
                .expectBody(QuizDto.class)
                .consumeWith(quiz -> {
                    assertEquals(1, quiz.getResponseBody().id());
                    assertThat(quiz.getResponseBody().questions())
                            .usingRecursiveComparison()
                            .ignoringFieldsOfTypes(LocalDateTime.class) // ignore creation date
                            .isEqualTo(Set.of(q1Dto, q2Dto));
                    assertEquals("c1", quiz.getResponseBody().questions().iterator().next().choices().iterator().next().choice());
                    assertNotNull(quiz.getResponseBody().createdAt());
                });
    }

    @Test
    void createQuiz_SAVES_quizInDatabase_WHEN_sufficientQuestions() {
        var newQ1Dto = new NewQuestionDto("q1", Set.of(choice), Set.of(1L));
        var newQ2Dto = new NewQuestionDto("q2", Set.of(choice), Set.of(1L));
        var newQ3Dto = new NewQuestionDto("q3", Set.of(choice), Set.of(1L));
        categoryService.createCategory(new NewCategoryDto("new"), user);
        questionService.createQuestion(newQ1Dto, user);
        questionService.createQuestion(newQ2Dto, user);
        questionService.createQuestion(newQ3Dto, user);

        var requestDto = new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM);

        restTestClient.post()
                .uri("/quiz/new")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .exchange();

        List<Quiz> savedQuizzes = quizRepo.findWithQuestionsByUserId(user.getId());
        assertEquals(1, savedQuizzes.size());
        assertEquals(1, savedQuizzes.getFirst().getId());
        assertEquals(3, savedQuizzes.getFirst().getQuestions().size());
        assertNotNull(savedQuizzes.getFirst().getCreatedAt());
    }

    @Test
    void createQuestion_RETURNS_Http400_WHEN_insufficientQuestions() {
        var newQ1Dto = new NewQuestionDto("q1", Set.of(choice), Set.of(1L));
        categoryService.createCategory(new NewCategoryDto("new"), user);
        questionService.createQuestion(newQ1Dto, user);

        var requestDto = new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM);

        restTestClient.post()
                .uri("/quiz/new")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(400, error.getResponseBody().status());
                    assertEquals("VALIDATION_FAILED", error.getResponseBody().error());
                    assertEquals("Requested quiz to contain 3 questions, " +
                            "but user has 1 applicable questions only", error.getResponseBody().message());
                    assertEquals("/quiz/new", error.getResponseBody().path());
                });
    }


}