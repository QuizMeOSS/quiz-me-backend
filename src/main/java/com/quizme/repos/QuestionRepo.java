package com.quizme.repos;

import com.quizme.entities.Question;
import com.quizme.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepo extends CrudRepository<Question, Long> {
    @EntityGraph(attributePaths = {
            "choices"
            // with LOAD type, any other attribute set as Eager will be respected
            // This is in contrast to FETCH type which overrides them to LAZY
    }, type = EntityGraph.EntityGraphType.LOAD)
    List<Question> findAllWithChoicesByUser(User user);
}
