package com.quizme.services.questionspicker;

/**
 * Creates a {@link QuestionsPicker} based on the requested {@link com.quizme.services.questionspicker.QuestionsPicker.Strategy}.
 */
public class QuestionsPickerFactory {

    public QuestionsPicker createPicker(QuestionsPicker.Strategy strategy,
                                       QuestionsPickerContext context){
        if(strategy == QuestionsPicker.Strategy.RANDOM){
            return new RandomPicker(context);
        }
        throw new IllegalArgumentException("Unknown questions picking strategy");
    }
}
