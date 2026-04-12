package com.quizme.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "quizzes_choices")
public class QuizChoice implements Choice {
    @EmbeddedId
    private QuizQuestionChoiceId id;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id", insertable = false, updatable = false),
            @JoinColumn(name = "question_id", referencedColumnName = "question_id", insertable = false, updatable = false)
    })
    QuizQuestion quizQuestion;

    @Column(name = "choice", nullable = false)
    private String choice;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect; // multiple choices can be correct

    protected QuizChoice() {
    }

    public QuizChoice(long quizId, QuizQuestion question, short choiceId, String choice, boolean isCorrect) {
        this.id = new QuizQuestionChoiceId(quizId, question.getId().getQuestionId(), choiceId);
        this.quizQuestion = question;
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
    public int getChoiceId() {
        return id.getChoiceId();
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