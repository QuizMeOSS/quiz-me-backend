package com.quizme.entities;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "quizzes_questions")
public class QuizQuestion {
    @EmbeddedId
    QuizQuestionId id;

    @ManyToOne
    @MapsId("quizId")
    @JoinColumn(name = "quiz_id")
    Quiz quiz;

    @ManyToOne
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    Question question;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<QuizChoice> choices;

    protected QuizQuestion() {
    }

    public QuizQuestion(Quiz quiz, Question question) {
        this.id = new QuizQuestionId(quiz.getId(), question.getId());
        this.quiz = quiz;
        this.question = question;
    }

    public Set<QuizChoice> getChoices() {
        return choices;
    }

    public void setChoices(Set<QuizChoice> choices) {
        this.choices = choices;
    }

    public QuizQuestionId getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, quiz, question, choices);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        QuizQuestion that = (QuizQuestion) obj;
        return id.equals(that.id) && question.equals(that.question) && quiz.equals(that.quiz)
                && Objects.equals(
                choices != null ? choices : Collections.emptySet(),
                that.choices != null ? that.choices : Collections.emptySet()
        );
    }
}