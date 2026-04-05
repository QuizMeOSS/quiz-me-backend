package com.quizme.dto;

import com.quizme.entities.Quiz;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record QuizDto(
        long id,
        Set<QuestionDto> questions,
        LocalDateTime createdAt
) {
    public static QuizDto fromEntity(Quiz quiz) {
        return new QuizDto(quiz.getId(),
                quiz.getQuestions()
                        .stream().map(QuestionDto::fromEntity)
                        .collect(Collectors.toSet()),
                quiz.getCreatedAt());
    }
}
