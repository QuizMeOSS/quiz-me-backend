package com.quizme.controllers;

import com.quizme.IntegrationTest;
import com.quizme.dto.*;
import com.quizme.entities.Quiz;
import com.quizme.entities.QuizAttempt;
import com.quizme.exceptionhandler.ApiError;
import com.quizme.repos.QuizAttemptRepo;
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
import static org.junit.jupiter.api.Assertions.*;

class QuizControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizRepo quizRepo;
    @Autowired
    private QuizAttemptRepo quizAttemptRepo;
    @Autowired
    private QuizService quizService;

    private final QuestionChoiceDto choice = new QuestionChoiceDto(1, "c1", true);

    @AfterEach
    @Override
    public void clear() {
        jdbcTemplate.execute("DELETE FROM questions_categories");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM quizzes_attempts");
        jdbcTemplate.execute("DELETE FROM quizzes_questions");
        jdbcTemplate.execute("DELETE FROM quizzes_choices");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM quizzes");
        // reset id sequence
        jdbcTemplate.execute("ALTER TABLE categories ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE questions ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE quizzes ALTER COLUMN id RESTART WITH 1");
        super.clear();
    }

    @Test
    void createQuiz_quizCreated_WHEN_sufficientQuestions() {
        var newQ1Dto = new NewQuestionDto("q1", Set.of(choice), Set.of(1L));
        var newQ2Dto = new NewQuestionDto("q2", Set.of(choice), Set.of(1L));
        var q1Dto = new QuestionDto(1, newQ1Dto.question(), newQ1Dto.choices(), newQ1Dto.categories(), LocalDateTime.now());
        var q2Dto = new QuestionDto(2, newQ2Dto.question(), newQ2Dto.choices(), newQ2Dto.categories(), LocalDateTime.now());
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");
        questionService.createQuestion(newQ1Dto, user, "k2");
        questionService.createQuestion(newQ2Dto, user, "k3");

        var requestDto = new NewQuizDto(2, QuestionsPicker.Strategy.RANDOM);

        restTestClient.post()
                .uri("/quiz/new")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
                .exchange()
                .expectBody(QuizDto.class)
                .consumeWith(quiz -> {
                    assertEquals(1, quiz.getResponseBody().id());
                    assertThat(quiz.getResponseBody().questions())
                            .usingRecursiveComparison()
                            .ignoringFieldsOfTypes(LocalDateTime.class) // ignore creation date
                            .isEqualTo(Set.of(q1Dto, q2Dto));
                    // assert choices are returned
                    assertEquals("c1", quiz.getResponseBody().questions().iterator().next().choices().iterator().next().choice());
                    assertNotNull(quiz.getResponseBody().createdAt());
                });
    }

    @Test
    void createQuiz_SAVES_quizInDatabase_WHEN_sufficientQuestions() {
        var newQ1Dto = new NewQuestionDto("q1", Set.of(choice), Set.of(1L));
        var newQ2Dto = new NewQuestionDto("q2", Set.of(choice), Set.of(1L));
        var newQ3Dto = new NewQuestionDto("q3", Set.of(choice), Set.of(1L));
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");
        questionService.createQuestion(newQ1Dto, user, "k2");
        questionService.createQuestion(newQ2Dto, user, "k3");
        questionService.createQuestion(newQ3Dto, user, "k4");

        var requestDto = new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM);

        restTestClient.post()
                .uri("/quiz/new")
                .header("Idempotency-Key", "idk1")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .exchange();

        List<Quiz> savedQuizzes = quizRepo.findWithQuestionsByUserId(user.getId());
        assertEquals(1, savedQuizzes.size());
        assertEquals(1, savedQuizzes.getFirst().getId());
        assertEquals(3, savedQuizzes.getFirst().getQuestions().size());
        assertNotNull(savedQuizzes.getFirst().getCreatedAt());
        assertNull(savedQuizzes.getFirst().getSubmittedAt());
    }

    @Test
    void createQuiz_RETURNS_Http400_WHEN_insufficientQuestions() {
        var newQ1Dto = new NewQuestionDto("q1", Set.of(choice), Set.of(1L));
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");
        questionService.createQuestion(newQ1Dto, user, "k2");

        var requestDto = new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM);

        restTestClient.post()
                .uri("/quiz/new")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
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

    @Test
    void submitQuiz_SAVES_quizAndAttempts_WHEN_validQuiz() {
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");

        // no need to create actual 4 choices per question, just 1 or 2 is ok
        QuestionChoiceDto q1choice1 = new QuestionChoiceDto(1, "c1", true);
        QuestionChoiceDto q1choice2 = new QuestionChoiceDto(2, "c2", false);
        QuestionChoiceDto q2choice = new QuestionChoiceDto(1, "c1", true);
        QuestionChoiceDto q3choice = new QuestionChoiceDto(1, "c1", true);
        var newQ1Dto = new NewQuestionDto("q1", Set.of(q1choice1, q1choice2), Set.of(1L));
        var newQ2Dto = new NewQuestionDto("q2", Set.of(q2choice), Set.of(1L));
        var newQ3Dto = new NewQuestionDto("q3", Set.of(q3choice), Set.of(1L));
        questionService.createQuestion(newQ1Dto, user, "k3");
        questionService.createQuestion(newQ2Dto, user, "k4");
        questionService.createQuestion(newQ3Dto, user, "k5");

        var newQuizDto = new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM);
        var quizDto = quizService.createQuiz(newQuizDto, user, "k2").success();

        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 2);
        var attempt2 = new QuizQuestionAttemptDto(2L, (short) 1);
        var attempt3 = new QuizQuestionAttemptDto(3L, (short) 1);
        var submitQuizDto = new SubmittedQuizDto(quizDto.id(), List.of(attempt1, attempt2, attempt3));

        restTestClient.post()
                .uri("/quiz/submit")
                .body(submitQuizDto)
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Void.class);

        // assert submission is stored in database
        List<Quiz> savedQuizzes = quizRepo.findWithQuestionsByUserId(user.getId());
        assertEquals(3, savedQuizzes.getFirst().getQuestions().size());
        assertNotNull(savedQuizzes.getFirst().getCreatedAt());
        assertNotNull(savedQuizzes.getFirst().getSubmittedAt());
        var quizQuestions = savedQuizzes.getFirst().getQuestions().stream()
                .sorted((a, b) -> Math.toIntExact(a.getId().getQuestionId() - b.getId().getQuestionId()))
                .toList();
        assertEquals(2, quizQuestions.get(0).getChoices().size());
        assertEquals(1, quizQuestions.get(1).getChoices().size());
        assertEquals(1, quizQuestions.get(2).getChoices().size());

        List<QuizAttempt> quizAttempts = quizAttemptRepo.findAllByIdQuizId(savedQuizzes.getFirst().getId())
                .stream()
                .sorted((a, b) -> Math.toIntExact(a.getId().getQuestionId() - b.getId().getQuestionId()))
                .toList();
        var expectedQ1Attempt = new QuizAttempt(1L, 1L, (short) 2);
        var expectedQ2Attempt = new QuizAttempt(1L, 2L, (short) 1);
        var expectedQ3Attempt = new QuizAttempt(1L, 3L, (short) 1);
        assertTrue(quizAttempts.contains(expectedQ1Attempt)
                && quizAttempts.contains(expectedQ2Attempt)
                && quizAttempts.contains(expectedQ3Attempt));
    }

    @Test
    void submitQuiz_RETURNS_404_WHEN_QuizNotFound() {
        restTestClient.post()
                .uri("/quiz/submit")
                .body(new SubmittedQuizDto(1, List.of()))
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(404, error.getResponseBody().status());
                    assertEquals("NOT_FOUND", error.getResponseBody().error());
                    assertEquals("Quiz not found", error.getResponseBody().message());
                    assertEquals("/quiz/submit", error.getResponseBody().path());
                });
    }

    /**
     * Each question must have only 1 answer, multiple answers per question
     * violates PK constraint and should be rejected
     */
    @Test
    void submitQuiz_RETURNS_400_WHEN_multipleAttemptsPerQuestion() {
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");

        // for simplicity, assume quiz has 1 question
        QuestionChoiceDto q1choice1 = new QuestionChoiceDto(1, "c1", true);
        QuestionChoiceDto q1choice2 = new QuestionChoiceDto(2, "c2", false);
        var newQ1Dto = new NewQuestionDto("q1", Set.of(q1choice1, q1choice2), Set.of(1L));
        questionService.createQuestion(newQ1Dto, user, "k3");

        var newQuizDto = new NewQuizDto(1, QuestionsPicker.Strategy.RANDOM);
        var quizDto = quizService.createQuiz(newQuizDto, user, "k2").success();

        // 2 attempts for same question -> invalid
        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 2);
        var attempt2 = new QuizQuestionAttemptDto(1L, (short) 1);
        var submitQuizDto = new SubmittedQuizDto(quizDto.id(), List.of(attempt1, attempt2));

        restTestClient.post()
                .uri("/quiz/submit")
                .body(submitQuizDto)
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(400, error.getResponseBody().status());
                    assertEquals("VALIDATION_FAILED", error.getResponseBody().error());
                    assertEquals("Found question with multiple answers", error.getResponseBody().message());
                    assertEquals("/quiz/submit", error.getResponseBody().path());
                });
    }

}