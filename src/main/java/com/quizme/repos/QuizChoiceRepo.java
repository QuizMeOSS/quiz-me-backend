package com.quizme.repos;

import com.quizme.entities.QuizChoice;
import com.quizme.entities.QuizChoiceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizChoiceRepo extends JpaRepository<QuizChoice, QuizChoiceId> {
}