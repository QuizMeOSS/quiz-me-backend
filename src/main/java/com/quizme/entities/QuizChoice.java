package com.quizme.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "quizzes_choices")
public class QuizChoice {
    @EmbeddedId
    private QuizChoiceId id;

    @Column(name = "choice", nullable = false)
    private String choice;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    protected QuizChoice() {}

    public QuizChoice(long quizId, long questionId, short choiceId, String choice, boolean isCorrect) {
        this.id = new QuizChoiceId(quizId, questionId, choiceId);
        this.choice = choice;
        this.isCorrect = isCorrect;
    }

    public QuizChoiceId getId() { return id; }
    public String getChoice() { return choice; }
    public boolean isCorrect() { return isCorrect; }
}