package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuestionChoiceId implements Serializable {
    @Column(name = "question_id")
    private long questionId;

    @Column(name = "choice_id")
    private short choiceId; // if question has 4 choices, choice_id is [1-4]

    protected QuestionChoiceId() {
    }

    public QuestionChoiceId(long questionId, short choiceId) {
        this.questionId = questionId;
        this.choiceId = choiceId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public short getChoiceId() {
        return choiceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionChoiceId that = (QuestionChoiceId) o;
        return questionId == that.questionId && choiceId == that.choiceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, choiceId);
    }
}
