package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions_choices")
public class QuestionChoice {
    @EmbeddedId
    private QuestionChoiceId id;

    @Column(name = "choice", nullable = false)
    private String choice;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect; // multiple choices can be correct

    protected QuestionChoice() {
    }

    public QuestionChoice(long questionId, short choiceId, String choice, boolean isCorrect) {
        this.id = new QuestionChoiceId(questionId, choiceId);
        this.choice = choice;
        this.isCorrect = isCorrect;
    }

    public QuestionChoiceId getId() {
        return id;
    }

    public String getChoice() {
        return choice;
    }

    public boolean isCorrect() {
        return isCorrect;
    }
}