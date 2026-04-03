package com.quizme.repos;

import com.quizme.entities.QuizQuestion;
import com.quizme.entities.QuizQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepo extends JpaRepository<QuizQuestion, QuizQuestionId> {
}