package com.quizme.controllers;

import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.entities.User;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.security.TokenFilter;
import com.quizme.services.CategoryService;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = CategoryController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TokenFilter.class))
@AutoConfigureRestTestClient
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters, this is just unit testing
class CategoryControllerTest {
    @Autowired
    private RestTestClient restTestClient;
    @MockitoBean
    private ResultToResponseEntityMapper mapper;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(username = "user@email.com")
    void createCategory_happyScenario() {
        Result<CreatedCategoryDto> result = Result.success(new CreatedCategoryDto(1, "a"));
        when(categoryService.createCategory(any(), any())).thenReturn(result);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        restTestClient.post()
                .uri("/categories")
                .body(new NewCategoryDto("exists"))
                .exchange()
                .expectBody(CreatedCategoryDto.class)
                .consumeWith(category -> {
                    assertEquals(1, category.getResponseBody().id());
                    assertEquals("a", category.getResponseBody().name());
                });
    }

    @Test
    @WithMockUser(username = "user@email.com")
    void createCategory_failureIsMappedToApiError() {
        Result<CreatedCategoryDto> result = Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "exists"));
        when(categoryService.createCategory(any(), any())).thenReturn(result);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        restTestClient.post()
                .uri("/categories")
                .body(new NewCategoryDto("exists"))
                .exchange();

        // verify the mapper was invoked to map the response to ApiError
        verify(mapper).map(result, "/categories");
    }

    @Test
    @WithMockUser(username = "user@email.com")
    void getCategories() {
        List<CreatedCategoryDto> categories = List.of(new CreatedCategoryDto(1, "x"),
                new CreatedCategoryDto(3, "y"));
        when(categoryService.getCategories(any())).thenReturn(categories);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));

        restTestClient.get()
                .uri("/categories")
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<CreatedCategoryDto>>() {
                })
                .consumeWith(result -> {
                    assertTrue(result.getStatus().is2xxSuccessful());
                    // no ORDER BY clause, so order isn't guarantee
                    assertTrue(result.getResponseBody().contains(new CreatedCategoryDto(1, "x")));
                    assertTrue(result.getResponseBody().contains(new CreatedCategoryDto(3, "y")));
                });
    }

}