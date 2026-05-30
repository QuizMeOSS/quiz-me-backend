package com.quizme.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "quizzes_attempts")
public class QuizAttempt {
    @EmbeddedId
    private QuizQuestionChoiceId id;

    protected QuizAttempt() {}

    public QuizAttempt(long quizId, long questionId, short answerId) { this.id = new QuizQuestionChoiceId(quizId, questionId, answerId); }

    public QuizQuestionChoiceId getId() { return id; }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof QuizAttempt)) return false;
        return id.equals(((QuizAttempt) obj).getId());
    }
}