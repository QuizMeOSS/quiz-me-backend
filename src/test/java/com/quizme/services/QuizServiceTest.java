package com.quizme.services;

import com.quizme.dto.*;
import com.quizme.entities.*;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.repos.QuestionRepo;
import com.quizme.repos.QuizAttemptRepo;
import com.quizme.repos.QuizRepo;
import com.quizme.services.questionspicker.QuestionsPicker;
import com.quizme.services.questionspicker.QuestionsPickerFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    private static final User user = mock(User.class);

    @Mock
    private QuestionRepo questionRepo;
    @Mock
    private QuizRepo quizRepo;
    @Mock
    private QuizAttemptRepo quizAttemptRepo;
    @Mock
    private TransactionTemplate transactionTemplate;

    private QuizService quizService;

    @BeforeEach
    void setup() {
        quizService = new QuizService(questionRepo, quizRepo, quizAttemptRepo, transactionTemplate, new QuestionsPickerFactory());
    }

    /**
     * If the available questions are less than
     * the questions count requested for the quiz, an error is returned.
     */
    @Test
    void createQuiz_returnsFailure_whenInsufficientQuestions() {
        when(questionRepo.findAllWithChoicesByUser(any())).thenReturn(
                List.of(mock(Question.class), mock(Question.class))
        );
        var result = quizService.createQuiz(
                new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM),
                mock(User.class));

        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                "Requested quiz to contain 3 questions, but user " +
                        "has 2 applicable questions only")), result);
    }

    /**
     * If user has 4 questions, and quiz needs 3 questions,
     * only 3 questions are used in the quiz.
     */
    @Test
    void createQuiz_returnsQuiz_whenSufficientQuestions() {
        var q1 = new Question(user, "question1", Collections.emptySet());
        var q2 = new Question(user, "question2", Collections.emptySet());
        var q3 = new Question(user, "question3", Collections.emptySet());
        var q4 = new Question(user, "question4", Collections.emptySet());

        var expectedQuestionsPool = Set.of(
                new QuizQuestionDto(q1.getId(), q1.getQuestion(),
                        QuestionChoiceDto.fromEntities(q1.getChoices()),
                        q1.getCategories().stream().map(Category::getId).collect(Collectors.toSet()), q1.getCreatedAt()),
                new QuizQuestionDto(q2.getId(), q2.getQuestion(),
                        QuestionChoiceDto.fromEntities(q2.getChoices()),
                        q2.getCategories().stream().map(Category::getId).collect(Collectors.toSet()), q2.getCreatedAt()),
                new QuizQuestionDto(q3.getId(), q3.getQuestion(),
                        QuestionChoiceDto.fromEntities(q3.getChoices()),
                        q3.getCategories().stream().map(Category::getId).collect(Collectors.toSet()), q3.getCreatedAt()),
                new QuizQuestionDto(q4.getId(), q4.getQuestion(),
                        QuestionChoiceDto.fromEntities(q4.getChoices()),
                        q4.getCategories().stream().map(Category::getId).collect(Collectors.toSet()), q4.getCreatedAt())
        );

        when(quizRepo.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionRepo.findAllWithChoicesByUser(any())).thenReturn(
                List.of(q1, q2, q3, q4)
        );
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        Result<QuizDto> result;
        // mock LocalDateTime
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            result = quizService.createQuiz(
                    new NewQuizDto(3, QuestionsPicker.Strategy.RANDOM),
                    user
            );
        }

        assertEquals(fixedDate, result.success().createdAt());
        assertEquals(3, result.success().questions().size());
        // the 3 picked questions should be originated from the 4 source questions
        assertTrue(expectedQuestionsPool.containsAll(result.success().questions()));
    }

    @Test
    void createQuiz_storesQuizInDatabase_WHEN_successful() {
        var q1 = new Question(user, "question1", Collections.emptySet());
        var q2 = new Question(user, "question2", Collections.emptySet());
        when(quizRepo.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionRepo.findAllWithChoicesByUser(any())).thenReturn(
                List.of(q1, q2)
        );
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        Quiz expectedQuiz;

        // mock LocalDateTime
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            quizService.createQuiz(
                    new NewQuizDto(2, QuestionsPicker.Strategy.RANDOM),
                    user
            );
            expectedQuiz = new Quiz(user);
            expectedQuiz.setQuestions(Set.of(new QuizQuestion(expectedQuiz, q1), new QuizQuestion(expectedQuiz, q2)));
        }

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepo).saveAndFlush(captor.capture());
        Quiz actualQuiz = captor.getValue();

        assertEquals(expectedQuiz.getQuestions(), actualQuiz.getQuestions());
        assertEquals(expectedQuiz.getCreatedAt(), actualQuiz.getCreatedAt());
        assertEquals(expectedQuiz.getSubmittedAt(), actualQuiz.getSubmittedAt());
    }

    @Test
    void createQuiz_SETS_quizChoices_WHEN_successful() {
        var q1 = new Question(user, "question1", Collections.emptySet());
        q1.setChoices(Set.of(new QuestionChoice(0, (short) 1, "C1", true)));
        var q2 = new Question(user, "question2", Collections.emptySet());
        q2.setChoices(Set.of(new QuestionChoice(0, (short) 1, "C2", true),
                new QuestionChoice(0, (short) 2, "C3", false)));
        when(quizRepo.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionRepo.findAllWithChoicesByUser(any())).thenReturn(
                List.of(q1, q2)
        );
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        Quiz expectedQuiz;
        // mock LocalDateTime
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            quizService.createQuiz(
                    new NewQuizDto(2, QuestionsPicker.Strategy.RANDOM),
                    user
            );
            expectedQuiz = new Quiz(user);
            var expectedQuizQuestion1 = new QuizQuestion(expectedQuiz, q1);
            var expectedQuizQuestion2 = new QuizQuestion(expectedQuiz, q2);
            expectedQuizQuestion1.setChoices(Set.of(new QuizChoice(0, expectedQuizQuestion1.getId().getQuestionId(), (short) 1, "C1", true)));
            expectedQuizQuestion2.setChoices(Set.of(new QuizChoice(0, expectedQuizQuestion2.getId().getQuestionId(), (short) 1, "C2", true),
                    new QuizChoice(0, expectedQuizQuestion2.getId().getQuestionId(), (short) 2, "C3", false)));
            expectedQuiz.setQuestions(Set.of(expectedQuizQuestion1, expectedQuizQuestion2));
        }

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepo).saveAndFlush(captor.capture());
        Quiz actualQuiz = captor.getValue();

        actualQuiz.getQuestions().forEach(actualQQ -> {
            QuizQuestion expectedQQ = expectedQuiz.getQuestions().stream()
                    .filter(q -> q.equals(actualQQ))
                    .findFirst().orElseThrow();
            assertEquals(
                    new HashSet<>(expectedQQ.getChoices()),
                    new HashSet<>(actualQQ.getChoices())
            );
        });
    }

    @Test
    void createQuiz_doesNotStoreQuizInDatabase_WHEN_insufficientQuestions() {
        var q1 = new Question(user, "question1", Collections.emptySet());
        when(questionRepo.findAllWithChoicesByUser(any())).thenReturn(
                List.of(q1)
        );

        // insufficient questions
        quizService.createQuiz(
                new NewQuizDto(2, QuestionsPicker.Strategy.RANDOM),
                user
        );

        verify(quizRepo, never()).save(any());
    }

    @Test
    void submitQuiz_setsQuizSubmittedAt_WHEN_validRequest() {
        var submittedQuizDto = new SubmittedQuizDto(1, List.of());
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            quizService.submitQuiz(submittedQuizDto);
        }

        assertEquals(fixedDate, quiz.getSubmittedAt());
    }

    @Test
    void submitQuiz_STORES_quiz_WHEN_validRequest() {
        var submittedQuizDto = new SubmittedQuizDto(0, List.of());
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        quizService.submitQuiz(submittedQuizDto);
        verify(quizRepo).save(quiz);
    }

    @Test
    void submitQuiz_STORES_quizAttempts_WHEN_validRequest() {
        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 1);
        var attempt2 = new QuizQuestionAttemptDto(2L, (short) 3);
        var submittedQuizDto = new SubmittedQuizDto(0, List.of(attempt1, attempt2));
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        quizService.submitQuiz(submittedQuizDto);
        verify(quizAttemptRepo).saveAll(
                List.of(
                        new QuizAttempt(quiz.getId(), attempt1.questionId(), attempt1.choiceId()),
                        new QuizAttempt(quiz.getId(), attempt2.questionId(), attempt2.choiceId())
                )
        );
    }

    @Test
    void submitQuiz_RETURNS_notFound_WHEN_quizNotFound() {
        var submittedQuizDto = new SubmittedQuizDto(0, List.of());
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.empty());

        var result = quizService.submitQuiz(submittedQuizDto);
        assertEquals(Result.failure(new Failure(FailureReason.NOT_FOUND, "Quiz not found")),
                result);

    }

    @Test
    void submitQuiz_RETURNS_validationFailed_WHEN_multipleAnswersPerQuestion() {
        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 1);
        var attempt2 = new QuizQuestionAttemptDto(2L, (short) 3);
        var submittedQuizDto = new SubmittedQuizDto(0, List.of(attempt1, attempt2));
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException("",
                        new ConstraintViolationException("", null,
                                ConstraintViolationException.ConstraintKind.UNIQUE, "quizzes_attempts_pkey")));

        var res = quizService.submitQuiz(submittedQuizDto);
        assertEquals(Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                        "Found question with multiple answers")),
                res);
    }

    /**
     * Make sure we don't mask unexpected exceptions
     */
    @Test
    void submitQuiz_THROWS_exception_WHEN_unexpectedConstraintViolation() {
        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 1);
        var submittedQuizDto = new SubmittedQuizDto(0, List.of(attempt1));
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                // unexpected constraint violation exception
                .thenThrow(new DataIntegrityViolationException("",
                        new ConstraintViolationException("", null,
                                ConstraintViolationException.ConstraintKind.NOT_NULL, "")));

        assertThrows(DataIntegrityViolationException.class, () ->
                quizService.submitQuiz(submittedQuizDto));
    }

    @Test
    void submitQuiz_THROWS_exception_WHEN_unexpectedDataIntegrityException() {
        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 1);
        var submittedQuizDto = new SubmittedQuizDto(0, List.of(attempt1));
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                // unexpected data integrity exception (no constraint violation)
                .thenThrow(new DataIntegrityViolationException("", null));

        assertThrows(DataIntegrityViolationException.class, () ->
                quizService.submitQuiz(submittedQuizDto));
    }

    @Test
    void submitQuiz_RETURNS_validationFailed_WHEN_unexpectedUniqueConstraintViolated() {
        var attempt1 = new QuizQuestionAttemptDto(1L, (short) 1);
        var submittedQuizDto = new SubmittedQuizDto(0, List.of(attempt1));
        var quiz = new Quiz(user);
        when(quizRepo.findById(submittedQuizDto.quizId())).thenReturn(Optional.of(quiz));
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException("",
                        new ConstraintViolationException("", null,
                                ConstraintViolationException.ConstraintKind.UNIQUE, "some_other_key")));

        assertThrows(DataIntegrityViolationException.class, () ->
                quizService.submitQuiz(submittedQuizDto));
    }
}