package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuizChoiceId implements Serializable {
    @Column(name = "quiz_id")
    private long quizId;

    @Column(name = "question_id")
    private long questionId;

    @Column(name = "choice_id")
    private short choiceId;

    protected QuizChoiceId() {}

    public QuizChoiceId(long quizId, long questionId, short choiceId) { this.quizId = quizId; this.questionId = questionId; this.choiceId = choiceId; }

    public long getQuizId() { return quizId; }
    public long getQuestionId() { return questionId; }
    public short getChoiceId() { return choiceId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizChoiceId that = (QuizChoiceId) o;
        return quizId == that.quizId && questionId == that.questionId && choiceId == that.choiceId;
    }

    @Override
    public int hashCode() { return Objects.hash(quizId, questionId, choiceId); }
}