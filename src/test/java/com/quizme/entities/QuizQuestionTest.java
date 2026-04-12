package com.quizme.entities;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuizQuestionTest {
    @Test
    void questionChoicesAreUnique() throws NoSuchFieldException {
        Field field = QuizQuestion.class.getDeclaredField("choices");
        // a set data structure ensures uniqueness
        assertTrue(Set.class.isAssignableFrom(field.getType()));
    }

    @Test
    void equals_RETURN_false_WHEN_differentId() throws Exception {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz1 = new Quiz(user);
        var quiz2 = new Quiz(user);
        // setting different quiz ids leads to different QuizQuestion id
        EntitiesTestUtils.setId(quiz1, 1);
        EntitiesTestUtils.setId(quiz2, 2);
        var quizQuestion1 = new QuizQuestion(quiz1, question);
        var quizQuestion2 = new QuizQuestion(quiz2, question);

        assertEquals(quizQuestion1.getQuestion(), quizQuestion2.getQuestion());
        assertEquals(quizQuestion1.getChoices(), quizQuestion2.getChoices());
        assertNotEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentQuiz() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz1 = new Quiz(new User()); // different users so different quizzes
        var quiz2 = new Quiz(new User());
        var quizQuestion1 = new QuizQuestion(quiz1, question);
        var quizQuestion2 = new QuizQuestion(quiz2, question);

        assertEquals(quizQuestion1.getId(), quizQuestion2.getId());
        assertEquals(quizQuestion1.getQuestion(), quizQuestion2.getQuestion());
        assertEquals(quizQuestion1.getChoices(), quizQuestion2.getChoices());
        assertNotEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentQuestion() {
        var user = new User();
        var question1 = new Question(user, "Q1", Set.of());
        var question2 = new Question(user, "Q2", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question1);
        var quizQuestion2 = new QuizQuestion(quiz, question2);

        assertEquals(quizQuestion1.getChoices(), quizQuestion2.getChoices());
        assertNotEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentChoices() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        var quizQuestion2 = new QuizQuestion(quiz, question);

        var choice1 = new QuizChoice(quiz.getId(), quizQuestion1.getId().getQuestionId(), (short) 1, "C1", true);
        var choice2 = new QuizChoice(quiz.getId(), quizQuestion2.getId().getQuestionId(), (short) 2, "C2", true);

        quizQuestion1.setChoices(Set.of(choice1));
        quizQuestion2.setChoices(Set.of(choice2));

        assertEquals(quizQuestion1.getQuestion(), quizQuestion2.getQuestion());
        assertEquals(quizQuestion1.getId(), quizQuestion2.getId());
        assertNotEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_false_WHEN_oneContainsNullChoices() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        var quizQuestion2 = new QuizQuestion(quiz, question);

        var choice = new QuizChoice(quiz.getId(), quizQuestion1.getId().getQuestionId(), (short) 1, "C1", true);
        quizQuestion1.setChoices(Set.of(choice));

        assertNotEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_true_WHEN_oneContainsNullChoicesAndOtherContainsEmpty() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        var quizQuestion2 = new QuizQuestion(quiz, question);

        quizQuestion1.setChoices(Set.of());

        assertEquals(quizQuestion1, quizQuestion2);
        assertEquals(quizQuestion2, quizQuestion1);
    }

    @Test
    void equals_RETURN_true_WHEN_bothContainNullChoices() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        var quizQuestion2 = new QuizQuestion(quiz, question);

        assertEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_true_WHEN_bothContainEmptyChoices() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        var quizQuestion2 = new QuizQuestion(quiz, question);

        quizQuestion1.setChoices(Set.of());
        quizQuestion2.setChoices(Set.of());

        assertEquals(quizQuestion1, quizQuestion2);
    }


    @Test
    void equals_RETURN_true_WHEN_equalAttributes() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        var quizQuestion2 = new QuizQuestion(quiz, question);

        var choice = new QuizChoice(quiz.getId(), quizQuestion1.getId().getQuestionId(), (short) 1, "C1", true);
        quizQuestion1.setChoices(Set.of(choice));
        quizQuestion2.setChoices(Set.of(choice));

        assertEquals(quizQuestion1, quizQuestion2);
    }

    @Test
    void equals_RETURN_true_WHEN_sameInstance() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion = new QuizQuestion(quiz, question);

        assertEquals(quizQuestion, quizQuestion);
    }

    @Test
    void equals_RETURN_false_WHEN_oneInstanceIsNull() {
        var user = new User();
        var question = new Question(user, "Q1", Set.of());
        var quiz = new Quiz(user);
        var quizQuestion1 = new QuizQuestion(quiz, question);
        QuizQuestion quizQuestion2 = null;

        assertNotEquals(quizQuestion1, quizQuestion2);
        assertNotEquals(quizQuestion2, quizQuestion1);
    }
}