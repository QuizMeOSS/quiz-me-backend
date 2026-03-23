package com.quizme.services;

import com.quizme.dto.NewQuestionDto;
import com.quizme.entities.Category;
import com.quizme.entities.Question;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class QuestionService {

    private final QuestionRepo questionRepo;
    private final CategoryService categoryService;

    public QuestionService(QuestionRepo questionRepo,
                           CategoryService categoryService) {
        this.questionRepo = questionRepo;
        this.categoryService = categoryService;
    }

    public Result<Question> createQuestion(NewQuestionDto requestDto,
                                                     User user) {
        if(requestDto.question().isEmpty()){
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Question can't be empty"));
        }
        if(requestDto.answer().isEmpty()){
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Please provide an answer to the question"));
        }
        Set<Category> categories;
        try {
            categories = new HashSet<>(categoryService.getCategoriesByIdsForUser(user, requestDto.categories()));
        } catch (IllegalArgumentException e){
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, e.getMessage()));
        }
        if(categories.isEmpty()){
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED, "Question must belong to at least one category"));
        }
        var question = new Question(user, requestDto.question(), requestDto.answer(), categories);
        return saveQuestionOrReturnErrorIfExists(question);

    }

    @NonNull
    private Result<Question> saveQuestionOrReturnErrorIfExists(Question question) {
        try {
            // Q: why not check if question exists first, then store if it doesn't exist?
            // A: to avoid race conditions. Downside: postgres increments the primary key sequence
            // even if transaction is rolled back, so a primary key is wasted every time
            // a user tries inserting duplicate item. But that's probably ok due to
            // the high range of bigint
            question = questionRepo.save(question);
            return Result.success(question);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException constraintViolationException
                    && constraintViolationException.getKind().equals(ConstraintViolationException.ConstraintKind.UNIQUE)) {
                // expected scenario, wrap it in Result
                return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Question already exists"));
            }
            // unexpected exception, propagate it
            throw e;
        }
    }
}
