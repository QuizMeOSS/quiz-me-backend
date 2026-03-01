package com.quizme.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record CreatedCategoryDto(
        long id,
        String name
) { }
