package com.quizme.dto;

import com.quizme.entities.Category;
import com.quizme.entities.Question;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record CreatedQuestionDto(
        long id,
        String question,
        String answer,
        Set<Long> categories,
        LocalDateTime createdAt
) {
    public static CreatedQuestionDto fromEntity(Question question){
        return new CreatedQuestionDto(question.getId(), question.getQuestion(), question.getAnswer(),
                question.getCategories().stream().map(Category::getId).collect(Collectors.toSet()),
                question.getCreatedAt());
    }
}
