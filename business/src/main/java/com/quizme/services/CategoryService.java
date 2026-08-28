package com.quizme.services;

import com.quizme.aspects.Idempotent;
import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.entities.Category;
import com.quizme.entities.User;
import com.quizme.exceptionhandler.result.Failure;
import com.quizme.exceptionhandler.result.FailureReason;
import com.quizme.exceptionhandler.result.Result;
import com.quizme.idempotency.CacheableResults;
import com.quizme.repos.CategoryRepo;
import com.quizme.services.cache.ApiCacheService;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepo categoryRepo;
    private final ApiCacheService apiCacheService;

    public CategoryService(CategoryRepo categoryRepo,
                           ApiCacheService apiCacheService) {
        this.categoryRepo = categoryRepo;
        this.apiCacheService = apiCacheService;
    }

    @Idempotent(payload = "requestDto")
    public Result<CreatedCategoryDto> createCategory(@NonNull NewCategoryDto requestDto,
                                                     @NonNull User user,
                                                     @Nullable String idempotencyKey) {
        var category = new Category(user.getId(), requestDto.name());
        var saveCategoryResult = saveCategoryOrReturnErrorIfExists(category);
        if (saveCategoryResult.success() != null) {
            apiCacheService.invalidate(CacheableResults.CATEGORY, user.getId().toString());
        }

        return saveCategoryResult;

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
    public Collection<CreatedCategoryDto> getAllCategories(User user) {

        LOGGER.info("Getting all categories for user {}", user.getId());
        Optional<Collection<Category>> cachedCategories = apiCacheService.get(CacheableResults.CATEGORY, user.getId().toString(),
                apiCacheService.createListType(Category.class));
        if (cachedCategories.isPresent()) {
            return cachedCategories.get().stream()
                    .map(CreatedCategoryDto::fromEntity)
                    .collect(Collectors.toSet());
        }

        var categoriesFromDb = getCategoriesFromDb(user);
        apiCacheService.storeResult(CacheableResults.CATEGORY,
                user.getId().toString(),
                categoriesFromDb,
                Duration.ofDays(3));
        return categoriesFromDb
                .stream()
                .map(CreatedCategoryDto::fromEntity)
                .collect(Collectors.toSet());
    }

    private Collection<Category> getCategoriesFromDb(User user) {
        LOGGER.info("Getting categories from DB...");
        return categoryRepo.findAllByUserId(user.getId())
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
        Optional<Collection<Category>> cachedCategories = apiCacheService.get(CacheableResults.CATEGORY, user.getId().toString(),
                apiCacheService.createListType(Category.class));
        if (cachedCategories.isPresent()) {
            return cachedCategories.get().stream()
                    .filter(c -> ids.contains(c.getId()))
                    .collect(Collectors.toSet());

        }
        return categoryRepo.findAllByUserIdAndIdIn(user.getId(), ids);
    }
}
