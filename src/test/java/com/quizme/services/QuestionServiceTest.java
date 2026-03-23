package com.quizme.services;

import com.quizme.dto.NewQuestionDto;
import com.quizme.entities.Category;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {
    @Mock
    private QuestionRepo questionRepo;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private QuestionService questionService;

    /**
     * User can't have duplicate questions (case-insensitive).
     */
    @Test
    void createQuestion_returnsFailure_whenQuestionExists() {
        when(questionRepo.save(any())).thenThrow(
                new DataIntegrityViolationException("",
                        new ConstraintViolationException("", null, ConstraintViolationException.ConstraintKind.UNIQUE, ""))
        );
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(new Category(new User("email", "username"), "C1")));

        var result = questionService.createQuestion(new NewQuestionDto("q", "a", Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS,
                "Question already exists")), result);
    }

    @Test
    void createQuestion_returnsSuccess_whenUniqueQuestion() {
        when(questionRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));

        var result = questionService.createQuestion(new NewQuestionDto("q", "a", Set.of()), user);

        assertEquals("q", result.success().getQuestion());
        assertEquals("a", result.success().getAnswer());
        assertEquals(Set.of(category), result.success().getCategories());
        assertNotNull(result.success().getCreatedAt());
    }

    @Test
    void createQuestion_propagatesException_whenUnexpectedConstraintViolation() {
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));
        when(questionRepo.save(any())).thenThrow(
                new DataIntegrityViolationException("",
                        // not the expected unique constraint violation
                        new ConstraintViolationException("", null, ConstraintViolationException.ConstraintKind.NOT_NULL, ""))
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            questionService.createQuestion(new NewQuestionDto("q","a", Set.of()), mock(User.class));
        });
    }

    @Test
    void createCategory_propagatesException_whenUnexpectedExceptionWhileSaving() {
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));
        when(questionRepo.save(any())).thenThrow(
                new DataIntegrityViolationException("",
                        // not the expected unique constraint violation
                        new RuntimeException(""))
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            questionService.createQuestion(new NewQuestionDto("q","a", Set.of()), mock(User.class));
        });
    }

    @Test
    void createQuestion_returnsFailure_whenEmptyQuestion() {

        var result = questionService.createQuestion(new NewQuestionDto("", "a", Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Question can't be empty")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenEmptyAnswer() {

        var result = questionService.createQuestion(new NewQuestionDto("q", "", Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Please provide an answer to the question")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenNoCategoryProvided() {

        var result = questionService.createQuestion(new NewQuestionDto("q", "a", Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Question must belong to at least one category")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenNonExistentCategoryProvided() {
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(Collections.emptyList()); // simulate category doesn't exist
        var result = questionService.createQuestion(new NewQuestionDto("q", "a", Set.of(1L)), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Question must belong to at least one category")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenCategoryDoesntBelongToUser() {
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenThrow(new IllegalArgumentException("EX MESSAGE")); // simulate one or more categories don't belong to user
        var result = questionService.createQuestion(new NewQuestionDto("q", "a", Set.of(1L, 2L)), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "EX MESSAGE")), result);
    }
}