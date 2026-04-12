package com.quizme.dto;

import com.quizme.entities.Quiz;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record QuizDto(
        long id,
        Set<QuizQuestionDto> questions,
        LocalDateTime createdAt
) {
    public static QuizDto fromEntity(Quiz quiz) {
        return new QuizDto(quiz.getId(),
                quiz.getQuestions()
                        .stream().map(QuizQuestionDto::fromEntity)
                        .collect(Collectors.toSet()),
                quiz.getCreatedAt());
    }
}
