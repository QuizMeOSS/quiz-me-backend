package com.quizme.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "user_question", columnNames = {"user_id", "question"})
        })
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String question;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
    @ManyToMany
    @JoinTable(
            name = "questions_categories",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @OneToMany
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private Set<QuestionChoice> choices = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Question() {
    }

    public Question(User user, String question, Set<Category> categories) {
        this.user = user;
        this.question = question;
        this.categories = categories;
    }

    public long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public Set<QuestionChoice> getChoices() {
        return choices;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setChoices(Set<QuestionChoice> choices) {
        this.choices = choices;
    }
}
