package com.quizme.dto;

import com.quizme.services.questionspicker.QuestionsPicker;
import jakarta.validation.constraints.Min;

public record NewQuizDto(
        @Min(value = 1, message = "Quiz must have at least one question")
        int questionsCount,
        QuestionsPicker.Strategy questionsPickingStrategy) {

    public NewQuizDto {
        if (questionsPickingStrategy == null) {
            questionsPickingStrategy = QuestionsPicker.Strategy.RANDOM;
        }
    }
}
