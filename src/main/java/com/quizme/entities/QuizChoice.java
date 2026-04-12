package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "quizzes_choices")
public class QuizChoice implements Choice {
    @EmbeddedId
    private QuizQuestionChoiceId id;

    @Column(name = "choice", nullable = false)
    private String choice;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect; // multiple choices can be correct

    protected QuizChoice() {
    }

    public QuizChoice(long quizId, long questionId, short choiceId, String choice, boolean isCorrect) {
        this.id = new QuizQuestionChoiceId(quizId, questionId, choiceId);
        this.choice = choice;
        this.isCorrect = isCorrect;
    }

    public QuizQuestionChoiceId getId() {
        return id;
    }

    public String getChoice() {
        return choice;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, choice, isCorrect);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        QuizChoice that = (QuizChoice) obj;
        return id.equals(that.id) && choice.equals(that.choice) && isCorrect == that.isCorrect;
    }
}