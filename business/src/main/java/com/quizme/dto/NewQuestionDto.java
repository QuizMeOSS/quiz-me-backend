package com.quizme.dto;

import java.util.Set;

public record NewQuestionDto(
        String question,
        Set<QuestionChoiceDto> choices,
        Set<Long> categories
) {
}
