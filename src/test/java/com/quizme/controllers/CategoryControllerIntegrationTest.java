package com.quizme.controllers;

import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.exceptionhandler.ApiError;
import com.quizme.services.CategoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryControllerIntegrationTest extends IntegrationTest {
    @Autowired
    private CategoryService categoryService;

    @AfterEach
    @Override
    void resetDatabase() {
        super.resetDatabase();
        jdbcTemplate.execute("DELETE FROM categories");
        // reset id sequence
        jdbcTemplate.execute("ALTER TABLE categories ALTER COLUMN id RESTART WITH 1");
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
    void createCategory_returnsHttp409_whenCategoryWithSameNameExists() {
        // simulate existing category
        var requestDto = new NewCategoryDto("new");
        categoryService.createCategory(requestDto, user, "");

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

    @Test
    void getCategories() {
        var requestDto = new NewCategoryDto("Algorithms");
        var requestDto2 = new NewCategoryDto("OS");
        var requestDto3 = new NewCategoryDto("Databases");
        categoryService.createCategory(requestDto, user, "");
        categoryService.createCategory(requestDto2, user, "");
        categoryService.createCategory(requestDto3, user, "");

        restTestClient.get()
                .uri("/categories")
                .cookie("access_token", accessToken)
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<CreatedCategoryDto>>() {
                })
                .consumeWith(result -> {
                    assertTrue(result.getStatus().is2xxSuccessful());
                    // no ORDER BY clause, so order isn't guarantee
                    assertTrue(result.getResponseBody().contains(new CreatedCategoryDto(1, requestDto.name())));
                    assertTrue(result.getResponseBody().contains(new CreatedCategoryDto(2, requestDto2.name())));
                    assertTrue(result.getResponseBody().contains(new CreatedCategoryDto(3, requestDto3.name())));
                });
    }

}