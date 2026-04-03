package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuizQuestionId implements Serializable {
    @Column(name = "quiz_id")
    private long quizId;

    @Column(name = "question_id")
    private long questionId;

    protected QuizQuestionId() {}

    public QuizQuestionId(long quizId, long questionId) { this.quizId = quizId; this.questionId = questionId; }

    public long getQuizId() { return quizId; }
    public long getQuestionId() { return questionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizQuestionId that = (QuizQuestionId) o;
        return quizId == that.quizId && questionId == that.questionId;
    }

    @Override
    public int hashCode() { return Objects.hash(quizId, questionId); }
}