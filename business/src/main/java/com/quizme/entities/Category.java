package com.quizme.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "user_category", columnNames = {"userId", "name"})
        })
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    // no need to fetch the complete user when fetching the category
    // because the clients would know the user anyway so no need to return the user object to them
    // benefit: cached redis values are shorter
    // note: db fk constraints now responsible for integrity and cascade deletion
    @Column(name = "user_id", nullable = false)
    private long userId;
    @Column(nullable = false)
    private String name;


    protected Category() {
    }

    public Category(long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category that = (Category) obj;

        return id == that.id && userId == that.userId && name.equals(that.name);
    }
}
