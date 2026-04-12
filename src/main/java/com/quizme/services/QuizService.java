package com.quizme.services;

import com.quizme.dto.NewQuizDto;
import com.quizme.dto.QuizDto;
import com.quizme.dto.SubmittedQuizDto;
import com.quizme.entities.*;
import com.quizme.repos.QuestionRepo;
import com.quizme.repos.QuizAttemptRepo;
import com.quizme.repos.QuizRepo;
import com.quizme.services.questionspicker.InsufficientQuestionsException;
import com.quizme.services.questionspicker.QuestionsPickerContext;
import com.quizme.services.questionspicker.QuestionsPickerFactory;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuestionRepo questionRepo;
    private final QuizRepo quizRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final QuestionsPickerFactory questionsPickerFactory;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public QuizService(
            QuestionRepo questionRepo,
            QuizAttemptRepo quizAttemptRepo,
            TransactionTemplate transactionTemplate,
            QuizRepo quizRepo
    ) {
        this(questionRepo, quizRepo, quizAttemptRepo, transactionTemplate, new QuestionsPickerFactory());
    }

    QuizService(
            QuestionRepo questionRepo,
            QuizRepo quizRepo,
            QuizAttemptRepo quizAttemptRepo,
            TransactionTemplate transactionTemplate,
            QuestionsPickerFactory questionsPickerFactory
    ) {
        this.questionRepo = questionRepo;
        this.quizRepo = quizRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.transactionTemplate = transactionTemplate;
        this.questionsPickerFactory = questionsPickerFactory;
    }

    public Result<QuizDto> createQuiz(NewQuizDto requestDto, User user) {
        var questionsPicker = questionsPickerFactory.createPicker(requestDto.questionsPickingStrategy(),
                new QuestionsPickerContext(user, questionRepo));
        Set<Question> pickedQuestions;
        try {
            pickedQuestions = questionsPicker.pick(requestDto.questionsCount());
        } catch (InsufficientQuestionsException e) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                    String.format("Requested quiz to contain %d questions, " +
                                    "but user has %d applicable questions only",
                            e.getRequestedCount(),
                            e.getAvailableCount())));
        }

        var quiz = new Quiz(user);
        quiz.setQuestions(pickedQuestions.stream().map(q -> new QuizQuestion(quiz, q))
                .collect(Collectors.toSet()));
        var savedQuiz = saveQuizAndChoices(quiz);

        return Result.success(QuizDto.fromEntity(savedQuiz));
    }

    private Quiz saveQuizAndChoices(Quiz quiz) {
        // We use a transaction to ensure both user and identity are created atomically
        // @Transactional annotation cannot be used because the method is called from within the same class
        // and thus would not be proxied by Spring for transaction management
        // so we use TransactionTemplate instead
        return transactionTemplate.execute(_ -> {
            var savedQuiz = quizRepo.saveAndFlush(quiz);
            for (var quizQuestion : savedQuiz.getQuestions()) {
                Set<QuizChoice> choices = quizQuestion.getQuestion().getChoices().stream()
                        .map(c -> new QuizChoice(
                                savedQuiz.getId(),
                                quizQuestion.getId().getQuestionId(),
                                c.getId().getChoiceId(),
                                c.getChoice(),
                                c.isCorrect()
                        ))
                        .collect(Collectors.toSet());
                quizQuestion.setChoices(choices);
            }
            return savedQuiz;
        });
    }

    public Result<Void> submitQuiz(SubmittedQuizDto requestDto) {
        var quizOptional = quizRepo.findById(requestDto.quizId());
        if (quizOptional.isEmpty()) {
            return Result.failure(new Failure(FailureReason.NOT_FOUND, "Quiz not found"));
        }
        var quiz = quizOptional.get();
        try {
            updateQuizAndAttempts(requestDto, quiz);
            return Result.success(null);
        } catch (DataIntegrityViolationException e) {
            if (violatedQuestionChoiceUniqueness(e)) {
                return Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                        "Found question with multiple answers"));
            }
            throw e;
        }
    }

    private boolean violatedQuestionChoiceUniqueness(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException constraintViolationException
                && constraintViolationException.getKind().equals(ConstraintViolationException.ConstraintKind.UNIQUE)
                && "quizzes_attempts_pkey".equals(constraintViolationException.getConstraintName());
    }

    private void updateQuizAndAttempts(SubmittedQuizDto requestDto, Quiz quiz) {
        transactionTemplate.execute(_ -> {
            quiz.setSubmittedAt(LocalDateTime.now());
            saveQuizAttempts(requestDto);
            // update existing quiz
            quizRepo.save(quiz);
            return null;
        });

    }

    private void saveQuizAttempts(SubmittedQuizDto requestDto) {
        var quizAttempts = requestDto.attempts()
                .stream()
                .map(a -> new QuizAttempt(requestDto.quizId(), a.questionId(), a.choiceId()))
                .toList();
        quizAttemptRepo.saveAll(quizAttempts);
    }
}
