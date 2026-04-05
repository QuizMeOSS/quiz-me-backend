package com.quizme.dto;

import com.quizme.entities.Category;
import com.quizme.entities.Question;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record QuestionDto(
        long id,
        String question,
        Set<QuestionChoiceDto> choices,
        Set<Long> categories,
        LocalDateTime createdAt
) {
    public static QuestionDto fromEntity(Question question) {
        return new QuestionDto(question.getId(), question.getQuestion(),
                QuestionChoiceDto.fromEntities(question.getChoices()),
                question.getCategories().stream().map(Category::getId).collect(Collectors.toSet()),
                question.getCreatedAt());
    }
}
