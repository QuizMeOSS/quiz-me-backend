package com.quizme.services.questionspicker;

import com.quizme.entities.Question;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public interface QuestionsPicker {

    /**
     * picks unique questions
     * @param numberOfQuestions number of questions to pick
     * @return a set of questions with size = {@code numberOfQuestions}.
     * @throws InsufficientQuestionsException if number of questions available to pick from
     * is less than {@code numberOfQuestions}
     */
    @NonNull Set<Question> pick(int numberOfQuestions);

    enum Strategy{
        RANDOM
    }
}
