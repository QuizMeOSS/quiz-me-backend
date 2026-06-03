package com.quizme.dto;

import com.quizme.entities.Category;
import com.quizme.entities.QuizQuestion;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record QuizQuestionDto(
        long id,
        String question,
        Set<QuestionChoiceDto> choices,
        Set<Long> categories,
        LocalDateTime createdAt
) {
    public static QuizQuestionDto fromEntity(QuizQuestion quizQuestion) {
        return new QuizQuestionDto(quizQuestion.getId().getQuestionId(),
                quizQuestion.getQuestion().getQuestion(),
                QuestionChoiceDto.fromEntities(quizQuestion.getQuestion().getChoices()),
                quizQuestion.getQuestion().getCategories().stream().map(Category::getId).collect(Collectors.toSet()),
                quizQuestion.getQuestion().getCreatedAt());
    }
}
