package com.quizme.entities;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuizTest {
    @Test
    void quizQuestionsAreUnique() throws NoSuchFieldException {
        Field field = Quiz.class.getDeclaredField("questions");
        // a set data structure ensures uniqueness
        assertTrue(Set.class.isAssignableFrom(field.getType()));
    }

    @Test
    void equals_RETURN_false_WHEN_differentCreationDate() throws InterruptedException {
        var user = new User();
        var q1 = new Quiz(user);
        // sometimes creation is quick so both quizzes get same time
        // so we introduce a delay
        Thread.sleep(1);
        var q2 = new Quiz(user);

        assertEquals(q1.getId(), q2.getId());
        assertEquals(q1.getSubmittedAt(), q2.getSubmittedAt());
        assertNotEquals(q1, q2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentId() throws Exception {
        var user = new User();
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            var q1 = new Quiz(user);
            var q2 = new Quiz(user);
            // both IDs are initially 0, need to change them
            EntitiesTestUtils.setId(q1, 1);
            EntitiesTestUtils.setId(q2, 2);

            assertEquals(q1.getCreatedAt(), q2.getCreatedAt());
            assertEquals(q1.getSubmittedAt(), q2.getSubmittedAt());
            assertNotEquals(q1, q2);
        }
    }

    @Test
    void equals_RETURN_false_WHEN_differentUser() {
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            var q1 = new Quiz(new User("email", "username"));
            var q2 = new Quiz(new User("email2", "username"));

            assertEquals(q1.getId(), q2.getId());
            assertEquals(q1.getCreatedAt(), q2.getCreatedAt());
            assertEquals(q1.getSubmittedAt(), q2.getSubmittedAt());
            assertNotEquals(q1, q2);
        }
    }

    @Test
    void equals_RETURN_false_WHEN_differentSubmissionDate() {
        var user = new User();
        var q1SubmissionDate = LocalDateTime.of(2026, 2, 2, 2, 2);
        var q2SubmissionDate = LocalDateTime.of(2026, 3, 3, 3, 3);
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            var q1 = new Quiz(user);
            var q2 = new Quiz(user);

            q1.setSubmittedAt(q1SubmissionDate);
            q2.setSubmittedAt(q2SubmissionDate);

            assertEquals(q1.getId(), q2.getId());
            assertEquals(q1.getCreatedAt(), q2.getCreatedAt());
            assertNotEquals(q1, q2);
        }
    }

    @Test
    void equals_RETURN_false_WHEN_oneSubmittedAndOneNot() {
        var user = new User();
        var q1SubmissionDate = LocalDateTime.of(2026, 2, 2, 2, 2);
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            var q1 = new Quiz(user);
            var q2 = new Quiz(user);

            q1.setSubmittedAt(q1SubmissionDate);

            assertEquals(q1.getId(), q2.getId());
            assertEquals(q1.getCreatedAt(), q2.getCreatedAt());
            assertNotEquals(q1, q2);
        }
    }


    /**
     * Since there is a 2-way reference between Quiz <-> QuizQuestion,
     * we don't check questions equality inside quiz to avoid infinite recursion.
     */
    @Test
    void equals_RETURN_true_WHEN_differentQuestions() {
        var user = new User();
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            var q1 = new Quiz(user);
            q1.setQuestions(Set.of(new QuizQuestion(q1, new Question(user, "q", Set.of()))));
            var q2 = new Quiz(user);

            assertEquals(q1, q2);
        }
    }

    @Test
    void equals_RETURN_true_WHEN_equalAttributes() {
        var user = new User();
        LocalDateTime fixedDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        try (MockedStatic<LocalDateTime> mockedLocalDateTime = Mockito.mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedDate);
            var q1 = new Quiz(user);
            var q2 = new Quiz(user);

            assertEquals(q1, q2);
        }
    }

    @Test
    void equals_RETURN_true_WHEN_sameInstance() {
        var q1 = new Quiz(new User());
        assertEquals(q1, q1);
    }

    @Test
    void equals_RETURN_false_WHEN_oneInstanceIsNull() {
        var user = new User();
        var q1 = new Quiz(user);
        Quiz q2 = null;
        assertNotEquals(q1, q2);
        assertNotEquals(q2, q1);
    }
}