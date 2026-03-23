package com.quizme.services;

import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.entities.Category;
import com.quizme.entities.User;
import com.quizme.repos.CategoryRepo;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.StreamSupport;

@Service
public class CategoryService {

    private final CategoryRepo categoryRepo;

    public CategoryService(CategoryRepo categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    public Result<CreatedCategoryDto> createCategory(NewCategoryDto requestDto,
                                                     User user) {
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
        return categoryRepo.findAllByUser(user)
                .stream()
                .toList();
    }

    /**
     * Get specific categories of a user by id.
     * @param user user to get their categories
     * @param ids ids of the categories to look for
     * @return categories of a user, selected by id.
     */
    @NonNull
    public Collection<Category> getCategoriesByIds(User user, Set<Long> ids){
        return StreamSupport.stream(categoryRepo.findAllById(ids).spliterator(), false)
                .peek(category -> {
                    if (!category.getUser().getId().equals(user.getId())) {
                        throw new IllegalArgumentException(String.format("Category %s doesn't belong to user %s",
                                category.getId(), user.getId()));
                    }
                })
                .toList();
    }
}
