package com.quizme.repos;

import com.quizme.entities.Category;
import com.quizme.entities.Question;
import com.quizme.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepo extends CrudRepository<Question, Long> {
}
