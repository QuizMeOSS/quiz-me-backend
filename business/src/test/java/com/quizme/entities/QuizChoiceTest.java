package com.quizme.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QuizChoiceTest {

    @Test
    void equals_RETURN_false_WHEN_differentQuizId() {
        var choice1 = new QuizChoice(0, 0, (short) 1, "C", true);
        var choice2 = new QuizChoice(1, 0, (short) 1, "C", true);

        assertEquals(choice1.getChoice(), choice2.getChoice());
        assertEquals(choice1.isCorrect(), choice2.isCorrect());
        assertNotEquals(choice1, choice2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentQuestionId() {
        var choice1 = new QuizChoice(1, 2, (short) 1, "C", true);
        var choice2 = new QuizChoice(1, 3, (short) 1, "C", true);

        assertEquals(choice1.getChoice(), choice2.getChoice());
        assertEquals(choice1.isCorrect(), choice2.isCorrect());
        assertNotEquals(choice1, choice2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentChoiceId() {
        var choice1 = new QuizChoice(1, 2, (short) 1, "C", true);
        var choice2 = new QuizChoice(1, 2, (short) 3, "C", true);

        assertEquals(choice1.getChoice(), choice2.getChoice());
        assertEquals(choice1.isCorrect(), choice2.isCorrect());
        assertNotEquals(choice1, choice2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentChoiceString() {
        var choice1 = new QuizChoice(0, 0, (short) 1, "C1", true);
        var choice2 = new QuizChoice(0, 0, (short) 1, "C2", true);

        assertEquals(choice1.getId(), choice2.getId());
        assertEquals(choice1.isCorrect(), choice2.isCorrect());
        assertNotEquals(choice1, choice2);
    }

    @Test
    void equals_RETURN_false_WHEN_differentIsCorrect() {
        var choice1 = new QuizChoice(0, 0, (short) 1, "C", true);
        var choice2 = new QuizChoice(0, 0, (short) 1, "C", false);

        assertEquals(choice1.getId(), choice2.getId());
        assertEquals(choice1.getChoice(), choice2.getChoice());
        assertNotEquals(choice1, choice2);
    }

    @Test
    void equals_RETURN_true_WHEN_equalAttributes() {
        var choice1 = new QuizChoice(0, 0, (short) 1, "C", false);
        var choice2 = new QuizChoice(0, 0, (short) 1, "C", false);

        assertEquals(choice1, choice2);
    }

    @Test
    void equals_RETURN_true_WHEN_sameInstance() {
        var choice1 = new QuizChoice(0, 0, (short) 1, "C", false);
        assertEquals(choice1, choice1);
    }

    @Test
    void equals_RETURN_false_WHEN_oneIsNull() {
        var choice1 = new QuizChoice(0, 0, (short) 1, "C", false);
        QuizChoice choice2 = null;
        assertNotEquals(choice1, choice2);
        assertNotEquals(choice2, choice1);
    }
}