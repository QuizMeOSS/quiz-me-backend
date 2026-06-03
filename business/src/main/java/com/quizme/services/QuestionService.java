package com.quizme.services;

import com.quizme.aspects.Idempotent;
import com.quizme.dto.NewQuestionDto;
import com.quizme.dto.QuestionChoiceDto;
import com.quizme.entities.Category;
import com.quizme.entities.Question;
import com.quizme.entities.QuestionChoice;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.repos.QuestionChoiceRepo;
import com.quizme.repos.QuestionRepo;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class QuestionService {

    private final QuestionRepo questionRepo;
    private final CategoryService categoryService;
    private final QuestionChoiceRepo questionChoiceRepo;
    private final TransactionTemplate transactionTemplate;

    public QuestionService(QuestionRepo questionRepo,
                           CategoryService categoryService,
                           QuestionChoiceRepo questionChoiceRepo,
                           TransactionTemplate transactionTemplate) {
        this.questionRepo = questionRepo;
        this.categoryService = categoryService;
        this.questionChoiceRepo = questionChoiceRepo;
        this.transactionTemplate = transactionTemplate;
    }

    @Idempotent(payload = "requestDto")
    public Result<Question> createQuestion(NewQuestionDto requestDto,
                                           User user, String idempotencyKey) {
        if (requestDto.question().isEmpty()) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Question can't be empty"));
        }
        if (requestDto.choices().stream().noneMatch(QuestionChoiceDto::isCorrect)) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Please provide an answer to the question"));
        }
        Set<Category> categories;
        try {
            categories = new HashSet<>(categoryService.getCategoriesByIdsForUser(user, requestDto.categories()));
        } catch (IllegalArgumentException e) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, e.getMessage()));
        }
        if (categories.isEmpty()) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Question must belong to at least one category"));
        }

        var question = new Question(user, requestDto.question(), categories);
        return saveQuestionOrReturnErrorIfExists(question, requestDto.choices());

    }

    @NonNull
    private Result<Question> saveQuestionOrReturnErrorIfExists(Question question, Set<QuestionChoiceDto> choicesDto) {
        try {
            return transactionTemplate.execute(_ -> {
                // Q: why not check if question exists first, then store if it doesn't exist?
                // A: Because this a more generic way to catch any constraint violation.
                // Otherwise, we would need to validate -by code- every constraint before storing.
                // Downside: postgres increments the primary key sequence
                // even if transaction is rolled back, so a primary key is wasted every time
                // a user tries inserting duplicate item. But that's probably ok due to
                // the high range of bigint
                var savedQuestion = questionRepo.save(question);

                var savedChoices = saveQuestionChoices(savedQuestion, choicesDto);
                savedQuestion.setChoices(new HashSet<>(savedChoices));

                return Result.success(savedQuestion);
            });
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException constraintViolationException
                    && constraintViolationException.getKind().equals(ConstraintViolationException.ConstraintKind.UNIQUE)) {
                // expected scenario, wrap it in Result
                return Result.failure(
                        new Failure(FailureReason.ALREADY_EXISTS, "Question already exists")
                );
            }
            throw e;
        }
    }

    @NonNull
    private List<QuestionChoice> saveQuestionChoices(Question question, Set<QuestionChoiceDto> choicesDto) {
        Set<QuestionChoice> choices = new HashSet<>();
        short choiceId = 1;
        for (QuestionChoiceDto choiceDto : choicesDto) {
            choices.add(new QuestionChoice(question.getId(), choiceId++, choiceDto.choice(), choiceDto.isCorrect()));
        }
        return questionChoiceRepo.saveAll(choices);
    }
}
