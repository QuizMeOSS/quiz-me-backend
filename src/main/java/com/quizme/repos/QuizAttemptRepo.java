package com.quizme.repos;

import com.quizme.entities.QuizAttempt;
import com.quizme.entities.QuizAttemptId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, QuizAttemptId> {
}