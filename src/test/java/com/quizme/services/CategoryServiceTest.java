package com.quizme.services;

import com.quizme.dto.NewCategoryDto;
import com.quizme.entities.Category;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.repos.CategoryRepo;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepo categoryRepo;

    @InjectMocks
    private CategoryService categoryService;

    /**
     * User can't have 2 categories with the same name.
     */
    @Test
    void createCategory_returnsFailure_whenCategoryExists() {
        when(categoryRepo.save(any())).thenThrow(
                new DataIntegrityViolationException("",
                        new ConstraintViolationException("", null, ConstraintViolationException.ConstraintKind.UNIQUE, ""))
        );

        var result = categoryService.createCategory(new NewCategoryDto("dup"), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS,
                "Category with same name already exists")), result);
    }

    @Test
    void createCategory_returnsSuccess_whenUniqueCategory() {
        when(categoryRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        var result = categoryService.createCategory(new NewCategoryDto("new"), mock(User.class));

        assertEquals("new", result.success().name());
    }

    @Test
    void createCategory_propagatesException_whenUnexpectedConstraintViolation() {
        when(categoryRepo.save(any())).thenThrow(
                new DataIntegrityViolationException("",
                        // not the expected unique constraint violation
                        new ConstraintViolationException("", null, ConstraintViolationException.ConstraintKind.NOT_NULL, ""))
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryService.createCategory(new NewCategoryDto("dup"), mock(User.class));
        });
    }

    @Test
    void createCategory_propagatesException_whenUnexpectedExceptionWhileSaving() {
        when(categoryRepo.save(any())).thenThrow(
                new DataIntegrityViolationException("",
                        // not the expected unique constraint violation
                        new RuntimeException(""))
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryService.createCategory(new NewCategoryDto("dup"), mock(User.class));
        });
    }

    @Test
    void getAllCategories_FetchesAllUserCategories() {
        User user = new User("", "");
        var categories = List.of(
                new Category(user, "a"),
                new Category(user, "b")
        );
        when(categoryRepo.findAllByUser(user)).thenReturn(
                categories
        );

        var serviceResponse = categoryService.getAllCategories(user);

        assertEquals("a", serviceResponse.toArray(Category[]::new)[0].getName());
        assertEquals("b", serviceResponse.toArray(Category[]::new)[1].getName());
    }

    @Test
    void getCategoriesByIdsForUser_returnsAllFoundCategories() {
        var user = new User("e", "u");
        var expectedCat1 = new Category(user, "Cat1");
        var expectedCat2 = new Category(user, "Cat2");
        when(categoryRepo.findAllByUserAndIdIn(any(), any())).thenReturn(
                List.of(expectedCat1, expectedCat2)
        );

        var categories = categoryService.getCategoriesByIdsForUser(user, Set.of());

        assertEquals(List.of(expectedCat1, expectedCat2), categories);
    }
}