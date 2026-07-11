package com.quizme.services;

import com.quizme.aspects.Idempotent;
import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.entities.Category;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.repos.CategoryRepo;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

@Service
public class CategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepo categoryRepo;

    public CategoryService(CategoryRepo categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @Idempotent(payload = "requestDto")
    public Result<CreatedCategoryDto> createCategory(@NonNull NewCategoryDto requestDto,
                                                     @NonNull User user,
                                                     @Nullable String idempotencyKey) {
        var category = new Category(user, requestDto.name());
        return saveCategoryOrReturnErrorIfExists(category);

    }

    @NonNull
    private Result<CreatedCategoryDto> saveCategoryOrReturnErrorIfExists(Category category) {
        try {
            // Q: why not check if name exists first, then store if it doesn't exist?
            // A: to avoid race conditions. Downside: postgres increments the primary key sequence
            // even if transaction is rolled back, so a primary key is wasted every time
            // a user tries inserting duplicate item. But that's probably ok due to
            // the high range of bigint
            categoryRepo.save(category);
            return Result.success(CreatedCategoryDto.fromEntity(category));
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException constraintViolationException
                    && constraintViolationException.getKind().equals(ConstraintViolationException.ConstraintKind.UNIQUE)) {
                // expected scenario, wrap it in Result
                return Result.failure(new Failure(FailureReason.ALREADY_EXISTS, "Category with same name already exists"));
            }
            // unexpected exception, propagate it
            throw e;
        }
    }

    @NonNull
    public Collection<Category> getAllCategories(User user) {
        LOGGER.info("Getting all categories for user {}", user.getId());
        return categoryRepo.findAllByUser(user)
                .stream()
                .toList();
    }

    /**
     * Get specific categories of a user by id.
     *
     * @param user user to get their categories
     * @param ids  ids of the categories to look for
     * @return categories of a user, selected by id.
     */
    @NonNull
    public Collection<Category> getCategoriesByIdsForUser(User user, Set<Long> ids) {
        return categoryRepo.findAllByUserAndIdIn(user, ids);
    }
}
