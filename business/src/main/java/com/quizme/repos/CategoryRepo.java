package com.quizme.repos;

import com.quizme.entities.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CategoryRepo extends CrudRepository<Category, Long> {
    List<Category> findAllByUserId(long userId);

    List<Category> findAllByUserIdAndIdIn(long userId, Collection<Long> ids);
}
