package com.quizme.controllers;

import com.quizme.IntegrationTest;
import com.quizme.dto.NewCategoryDto;
import com.quizme.dto.NewQuestionDto;
import com.quizme.dto.QuestionChoiceDto;
import com.quizme.dto.QuestionDto;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.ApiError;
import com.quizme.services.CategoryService;
import com.quizme.services.QuestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuestionControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private QuestionService questionService;
    @Autowired
    private CategoryService categoryService;
    private final QuestionChoiceDto choice = new QuestionChoiceDto(1, "c1", true);

    @AfterEach
    @Override
    public void clear() {
        super.clear();
        jdbcTemplate.execute("DELETE FROM questions_categories");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM questions");
        // reset id sequence
        jdbcTemplate.execute("ALTER TABLE categories ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE questions ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    void createQuestion_questionReturned_whenUniqueQuestion() {
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");
        var requestDto = new NewQuestionDto("newQ", Set.of(choice), Set.of(1L));

        restTestClient.post()
                .uri("/questions")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
                .exchange()
                .expectBody(QuestionDto.class)
                .consumeWith(question -> {
                    assertEquals(1, question.getResponseBody().id());
                    assertEquals("newQ", question.getResponseBody().question());
                    assertEquals("c1", question.getResponseBody().choices().iterator().next().choice());
                    assertTrue(question.getResponseBody().choices().iterator().next().isCorrect());
                    assertEquals(Set.of(1L), question.getResponseBody().categories());
                    assertNotNull(question.getResponseBody().createdAt());
                });
    }

    @Test
    void createQuestion_returnsHttp409_whenQuestionExists_caseInsensitive() {
        // simulate existing category and question
        categoryService.createCategory(new NewCategoryDto("new"), user, "k");
        questionService.createQuestion(new NewQuestionDto("duplicate question", Set.of(choice), Set.of(1L)), user, "k2");

        restTestClient.post()
                .uri("/questions")
                .body(new NewQuestionDto("Duplicate Question", Set.of(choice), Set.of(1L)))
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(409, error.getResponseBody().status());
                    assertEquals("ALREADY_EXISTS", error.getResponseBody().error());
                    assertEquals("Question already exists", error.getResponseBody().message());
                    assertEquals("/questions", error.getResponseBody().path());
                });
    }

    @Test
    void createQuestion_returnsHttp409_whenNoCategoryProvided() {
        restTestClient.post()
                .uri("/questions")
                .body(new NewQuestionDto("Question", Set.of(choice), Set.of()))
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(400, error.getResponseBody().status());
                    assertEquals("VALIDATION_FAILED", error.getResponseBody().error());
                    assertEquals("Question must belong to at least one category", error.getResponseBody().message());
                    assertEquals("/questions", error.getResponseBody().path());
                });
    }

    @Test
    void createQuestion_skipsNonExistentCategories() {
        categoryService.createCategory(new NewCategoryDto("Cat1"), user, "k1");
        categoryService.createCategory(new NewCategoryDto("Cat2"), user, "k2");
        var requestDto = new NewQuestionDto("newQ", Set.of(choice), Set.of(1L, 2L, 3L)); // 3 doesn't exist

        restTestClient.post()
                .uri("/questions")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
                .exchange()
                .expectBody(QuestionDto.class)
                .consumeWith(question -> {
                    assertEquals(1, question.getResponseBody().id());
                    assertEquals("newQ", question.getResponseBody().question());
                    assertEquals("c1", question.getResponseBody().choices().iterator().next().choice());
                    assertTrue(question.getResponseBody().choices().iterator().next().isCorrect());
                    assertEquals(Set.of(1L, 2L), question.getResponseBody().categories()); // only categories 1,2 linked
                    assertNotNull(question.getResponseBody().createdAt());
                });
    }

    @Test
    void createQuestion_skipsCategoriesBelongingToOtherUser() {
        var otherUser = userRepo.save(new User("otherEmail", "otherName"));
        categoryService.createCategory(new NewCategoryDto("Cat1"), user, "k1");
        // should be skipped
        categoryService.createCategory(new NewCategoryDto("Cat2"), otherUser, "k2");
        var requestDto = new NewQuestionDto("newQ", Set.of(choice), Set.of(1L, 2L));

        restTestClient.post()
                .uri("/questions")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .header("Idempotency-Key", "idk1")
                .exchange()
                .expectBody(QuestionDto.class)
                .consumeWith(question -> {
                    assertEquals(1, question.getResponseBody().id());
                    assertEquals("newQ", question.getResponseBody().question());
                    assertEquals("c1", question.getResponseBody().choices().iterator().next().choice());
                    assertTrue(question.getResponseBody().choices().iterator().next().isCorrect());
                    assertEquals(Set.of(1L), question.getResponseBody().categories()); // category 2 skipped
                    assertNotNull(question.getResponseBody().createdAt());
                });
    }
}