package com.quizme.controllers;

import com.quizme.dto.ApiError;
import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.services.CategoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private CategoryService categoryService;

    @AfterEach
    @Override
    void resetDatabase() {
        super.resetDatabase();
        jdbcTemplate.execute("DELETE FROM categories");
    }

    @Test
    void createCategory_categoryReturned_whenUniqueName() {
        var requestDto = new NewCategoryDto("new");

        restTestClient.post()
                .uri("/categories")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .exchange()
                .expectBody(CreatedCategoryDto.class)
                .consumeWith(category -> {
                    assertEquals(1, category.getResponseBody().id());
                    assertEquals("new", category.getResponseBody().name());
                });
    }

    @Test
    void register_returnsHttp409_whenCategoryWithSameNameExists() {
        // simulate existing category
        var requestDto = new NewCategoryDto("new");
        categoryService.createCategory(requestDto, user);

        restTestClient.post()
                .uri("/categories")
                .body(requestDto)
                .cookie("access_token", accessToken)
                .exchange()
                .expectBody(ApiError.class)
                .consumeWith(error -> {
                    assertEquals(409, error.getResponseBody().status());
                    assertEquals("ALREADY_EXISTS", error.getResponseBody().error());
                    assertEquals("Category with same name already exists", error.getResponseBody().message());
                    assertEquals("/categories", error.getResponseBody().path());
                });
    }

}