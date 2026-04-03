package com.quizme.repos;

import com.quizme.entities.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepo extends JpaRepository<Quiz, Long> {
}