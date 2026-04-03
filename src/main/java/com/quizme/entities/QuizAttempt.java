package com.quizme.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "quizzes_attempts")
public class QuizAttempt {
    @EmbeddedId
    private QuizAttemptId id;

    protected QuizAttempt() {}

    public QuizAttempt(long quizId, long questionId, short answerId) { this.id = new QuizAttemptId(quizId, questionId, answerId); }

    public QuizAttemptId getId() { return id; }
}