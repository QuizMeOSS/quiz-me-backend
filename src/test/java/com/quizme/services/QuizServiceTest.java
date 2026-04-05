package com.quizme.services;

import com.quizme.dto.NewQuizDto;
import com.quizme.dto.QuestionDto;
import com.quizme.dto.QuizDto;
import com.quizme.entities.Question;
import com.quizme.entities.Quiz;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import com.quizme.repos.QuizRepo;
import com.quizme.services.questionspicker.QuestionsPicker;
import com.quizme.services.questionspicker.QuestionsPickerFactory;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock
    private QuestionRepo questionRepo;
    @Mock
    private QuizRepo quizRepo;

    private QuizService quizService;

    @BeforeEach
    void setup() {
        quizService = new QuizService(questionRepo, quizRepo, new QuestionsPickerFactory());
    }

    /**
     * If the available questions are less than
     * the questions count requested for the quiz, an error is returned.
     */
    @Test
    void createQuiz_returnsFailure_whenInsufficientQuestions() {
        when(questionRepo.findAllByUser(any())).thenReturn(
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
        var user = mock(User.class);
        var q1 = new Question(user, "question1", Collections.emptySet());
        var q2 = new Question(user, "question2", Collections.emptySet());
        var q3 = new Question(user, "question3", Collections.emptySet());
        var q4 = new Question(user, "question4", Collections.emptySet());
        when(quizRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionRepo.findAllByUser(any())).thenReturn(
                List.of(q1, q2, q3, q4)
        );

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
        for (QuestionDto q : result.success().questions()) {
            assertTrue(q.equals(QuestionDto.fromEntity(q1))
                    || q.equals(QuestionDto.fromEntity(q2))
                    || q.equals(QuestionDto.fromEntity(q3))
                    || q.equals(QuestionDto.fromEntity(q4)));
        }
    }

    @Test
    void createQuiz_storesQuizInDatabase_WHEN_successful() {
        var user = mock(User.class);
        var q1 = new Question(user, "question1", Collections.emptySet());
        var q2 = new Question(user, "question2", Collections.emptySet());
        when(quizRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(questionRepo.findAllByUser(any())).thenReturn(
                List.of(q1, q2)
        );

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
            expectedQuiz.setQuestions(Set.of(q1, q2));
        }

        ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepo).save(captor.capture());
        Quiz actualQuiz = captor.getValue();

        assertEquals(expectedQuiz.getQuestions(), actualQuiz.getQuestions());
        assertEquals(expectedQuiz.getCreatedAt(), actualQuiz.getCreatedAt());
        assertEquals(expectedQuiz.getSubmittedAt(), actualQuiz.getSubmittedAt());
    }

    @Test
    void createQuiz_doesNotStoreQuizInDatabase_WHEN_insufficientQuestions() {
        var user = mock(User.class);
        var q1 = new Question(user, "question1", Collections.emptySet());
        when(questionRepo.findAllByUser(any())).thenReturn(
                List.of(q1)
        );

        // insufficient questions
        quizService.createQuiz(
                new NewQuizDto(2, QuestionsPicker.Strategy.RANDOM),
                user
        );

        verify(quizRepo, never()).save(any());
    }
}