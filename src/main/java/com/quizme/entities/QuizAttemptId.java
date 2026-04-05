package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuizAttemptId implements Serializable {
    @Column(name = "quiz_id")
    private long quizId;

    @Column(name = "question_id")
    private long questionId;

    @Column(name = "answer_id")
    private short answerId;

    protected QuizAttemptId() {}

    public QuizAttemptId(long quizId, long questionId, short answerId) { this.quizId = quizId; this.questionId = questionId; this.answerId = answerId; }

    public long getQuizId() { return quizId; }
    public long getQuestionId() { return questionId; }
    public short getAnswerId() { return answerId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizAttemptId that = (QuizAttemptId) o;
        return quizId == that.quizId && questionId == that.questionId && answerId == that.answerId;
    }

    @Override
    public int hashCode() { return Objects.hash(quizId, questionId, answerId); }
}