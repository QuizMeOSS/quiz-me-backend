package com.quizme.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuizQuestion> questions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    protected Quiz() {
    }

    public Quiz(User user) {
        this.user = user;
        createdAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Collection<QuizQuestion> getQuestions() {
        return questions;
    }

    public User getUser() {
        return user;
    }

    public void setQuestions(Set<QuizQuestion> questions) {
        this.questions = questions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, createdAt, submittedAt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Quiz that = (Quiz) obj;
        // skips `questions` because questions refers to quiz so we would have recursion.
        return id == that.id && user.equals(that.user) && createdAt.equals(that.createdAt)
                && Objects.equals(
                        submittedAt, that.submittedAt
        );
    }
}