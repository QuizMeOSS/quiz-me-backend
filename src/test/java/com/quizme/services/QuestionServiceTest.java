package com.quizme.services;

import com.quizme.dto.NewQuestionDto;
import com.quizme.dto.QuestionChoiceDto;
import com.quizme.entities.Category;
import com.quizme.entities.QuestionChoice;
import com.quizme.entities.User;
import com.quizme.repos.QuestionChoiceRepo;
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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {
    @Mock
    private QuestionRepo questionRepo;

    @Mock
    private CategoryService categoryService;

    @Mock
    private QuestionChoiceRepo questionChoiceRepo;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private QuestionService questionService;

    // default choice
    private final QuestionChoiceDto choice = new QuestionChoiceDto(1, "c", true);

    /**
     * User can't have duplicate questions (case-insensitive).
     */
    @Test
    void createQuestion_returnsFailure_whenQuestionExists() {
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(new Category(new User("email", "username"), "C1")));
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException("dup", new ConstraintViolationException(
                        "duplicate",
                        null,
                        ConstraintViolationException.ConstraintKind.UNIQUE,
                        "question_unique_constraint"
                )));
        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(choice), Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.ALREADY_EXISTS,
                "Question already exists")), result);
    }

    @Test
    void createQuestion_returnsSuccess_whenUniqueQuestion() {
        when(questionRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionChoiceRepo.saveAll(any()))
                .thenAnswer(i -> new ArrayList<>((Set) i.getArguments()[0]));
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));

        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(choice), Set.of()), user);

        assertEquals("q", result.success().getQuestion());
        assertEquals(Set.of(category), result.success().getCategories());
        assertNotNull(result.success().getCreatedAt());
        assertEquals("c", result.success().getChoices().iterator().next().getChoice());
        assertTrue(result.success().getChoices().iterator().next().isCorrect());
    }

    @Test
    void createQuestion_returnsSuccess_whenUniqueQuestionWithMultipleChoices() {
        when(questionRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionChoiceRepo.saveAll(any()))
                .thenAnswer(i -> new ArrayList<>((Set) i.getArguments()[0]));
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));

        var choice2 = new QuestionChoiceDto(2, "choice2", false);
        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(choice, choice2), Set.of()), user);

        assertEquals("q", result.success().getQuestion());
        assertEquals(Set.of(category), result.success().getCategories());
        assertNotNull(result.success().getCreatedAt());
        assertThat(result.success().getChoices())
                .extracting(QuestionChoice::getChoice, QuestionChoice::isCorrect)
                .containsExactlyInAnyOrder(
                        tuple("c", true),
                        tuple("choice2", false)
                );
    }

    @Test
    void createQuestion_propagatesException_whenUnexpectedConstraintViolation() {
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException("",
                        // not the expected unique constraint violation
                        new ConstraintViolationException("", null, ConstraintViolationException.ConstraintKind.NOT_NULL, "")));

        assertThrows(DataIntegrityViolationException.class, () ->
                questionService.createQuestion(new NewQuestionDto("q", Set.of(choice), Set.of()), mock(User.class))
        );
    }

    @Test
    void createCategory_propagatesException_whenUnexpectedExceptionWhileSaving() {
        var user = new User("email", "username");
        var category = new Category(user, "C1");
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(List.of(category));
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException(""));

        assertThrows(DataIntegrityViolationException.class, () ->
                questionService.createQuestion(new NewQuestionDto("q", Set.of(choice), Set.of()), mock(User.class))
        );
    }

    @Test
    void createQuestion_returnsFailure_whenEmptyQuestion() {

        var result = questionService.createQuestion(new NewQuestionDto("", Set.of(choice), Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Question can't be empty")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenNoAnswer() {

        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(), Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Please provide an answer to the question")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenNoCategoryProvided() {

        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(choice), Set.of()), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Question must belong to at least one category")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenNonExistentCategoryProvided() {
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenReturn(Collections.emptyList()); // simulate category doesn't exist
        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(choice),
                Set.of(1L)), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Question must belong to at least one category")), result);
    }

    @Test
    void createQuestion_returnsFailure_whenCategoryDoesntBelongToUser() {
        when(categoryService.getCategoriesByIdsForUser(any(), any()))
                .thenThrow(new IllegalArgumentException("EX MESSAGE")); // simulate one or more categories don't belong to user
        var result = questionService.createQuestion(new NewQuestionDto("q", Set.of(choice),
                Set.of(1L, 2L)), mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "EX MESSAGE")), result);
    }
}