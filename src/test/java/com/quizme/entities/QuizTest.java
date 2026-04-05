package com.quizme.entities;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuizTest {
    @Test
    void quizQuestionsAreUnique() throws NoSuchFieldException {
        Field field = Quiz.class.getDeclaredField("questions");
        // a set data structure ensures uniqueness
        assertTrue(Set.class.isAssignableFrom(field.getType()));
    }
}