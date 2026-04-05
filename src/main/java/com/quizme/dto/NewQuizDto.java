package com.quizme.dto;

import com.quizme.services.questionspicker.QuestionsPicker;

public record NewQuizDto(int questionsCount,
                         QuestionsPicker.Strategy questionsPickingStrategy) { }
