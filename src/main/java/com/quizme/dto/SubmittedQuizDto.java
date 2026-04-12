package com.quizme.dto;

import java.util.List;

public record SubmittedQuizDto(long quizId,
                               List<QuizQuestionAttemptDto> attempts) {
}
