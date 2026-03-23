package com.quizme.dto;

import com.quizme.entities.Category;

import java.time.LocalDateTime;
import java.util.Set;

public record CreatedCategoryDto(
        long id,
        String name
) {
    public static CreatedCategoryDto fromEntity(Category category){
        return new CreatedCategoryDto(category.getId(), category.getName());
    }
}
