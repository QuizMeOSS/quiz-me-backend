package com.quizme.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "quizzes_questions")
public class QuizQuestion {
    @EmbeddedId
    private QuizQuestionId id;

    protected QuizQuestion() {}

    public QuizQuestion(long quizId, long questionId) { this.id = new QuizQuestionId(quizId, questionId); }

    public QuizQuestionId getId() { return id; }
}