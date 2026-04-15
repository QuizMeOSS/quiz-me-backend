package com.quizme.dto;

import com.quizme.services.questionspicker.QuestionsPicker;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NewQuizDtoTest {
    private Validator validator;

    @BeforeEach
    public void setup() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void questionsCountMustBeGreaterThanZero() {
        var result = validator.validate(new NewQuizDto(0, QuestionsPicker.Strategy.RANDOM));
        assertEquals(1, result.size());
        assertEquals("Quiz must have at least one question", result.iterator().next().getMessage());
    }

    @Test
    void questionsCountMustBePositive() {
        var result = validator.validate(new NewQuizDto(-1, QuestionsPicker.Strategy.RANDOM));
        assertEquals(1, result.size());
        assertEquals("Quiz must have at least one question", result.iterator().next().getMessage());
    }

    @Test
    void questionPickingStrategyDefaultsToRandom() {
        // no strategy specified, should default to Strategy.RANDOM
        var dto = new NewQuizDto(2, null);
        assertEquals(QuestionsPicker.Strategy.RANDOM, dto.questionsPickingStrategy());
    }
}