package com.quizme.dto;

import com.quizme.entities.QuestionChoice;

import java.util.Set;
import java.util.stream.Collectors;

public record QuestionChoiceDto(String choice, boolean isCorrect) {
    public static Set<QuestionChoiceDto> fromEntities(Set<QuestionChoice> choices) {
        return choices.stream()
                .map(c -> new QuestionChoiceDto(c.getChoice(), c.isCorrect()))
                .collect(Collectors.toSet());
    }
}
