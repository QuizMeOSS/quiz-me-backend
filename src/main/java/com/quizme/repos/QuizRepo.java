package com.quizme.repos;

import com.quizme.entities.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepo extends JpaRepository<Quiz, Long> {
    // eagerly fetch questions
    @EntityGraph(attributePaths = {
            "questions",
            "questions.choices"
    // with LOAD type, any other attribute set as Eager will be respected
    // This is in contrast to FETCH type which overrides them to LAZY
    }, type = EntityGraph.EntityGraphType.LOAD)
    List<Quiz> findWithQuestionsByUserId(long userId);
}