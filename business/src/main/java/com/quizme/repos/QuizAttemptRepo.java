package com.quizme.repos;

import com.quizme.entities.QuizAttempt;
import com.quizme.entities.QuizQuestionChoiceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, QuizQuestionChoiceId> {
    List<QuizAttempt>  findAllByIdQuizId(long quizId);
}