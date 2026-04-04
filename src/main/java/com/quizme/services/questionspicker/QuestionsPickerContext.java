package com.quizme.services.questionspicker;

import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import org.jspecify.annotations.Nullable;

public record QuestionsPickerContext(
        @Nullable User user,
        @Nullable QuestionRepo questionRepo
) {
}
