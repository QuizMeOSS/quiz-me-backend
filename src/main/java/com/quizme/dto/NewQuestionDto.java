package com.quizme.dto;

import com.quizme.entities.Category;

import java.time.LocalDateTime;
import java.util.Set;

public record NewQuestionDto(
        String question,
        String answer,
        Set<Long> categories
) { }
