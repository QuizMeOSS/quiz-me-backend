package com.quizme.services.questionspicker;

import com.quizme.entities.Question;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class RandomPicker implements QuestionsPicker {

    private final QuestionRepo questionRepo;
    private final User user;

    public RandomPicker(
            @NonNull QuestionsPickerContext context
    ) {
        this.user = context.user();
        this.questionRepo = context.questionRepo();
        if(user == null || questionRepo == null){
            throw new IllegalArgumentException("question picker missing context");
        }
    }

    @Override
    public @NonNull Set<Question> pick(int numberOfQuestions) {
        var userQuestions = questionRepo.findAllByUser(user);
        if (insufficientQuestions(numberOfQuestions, userQuestions.size())) {
            throw new InsufficientQuestionsException(numberOfQuestions, userQuestions.size());
        }
        return pickNRandomQuestions(userQuestions, numberOfQuestions);
    }

    /**
     * Returns n unique random items from the supplied list.
     * This method assumes the supplied list contains unique items.
     */
    private <E> Set<E> pickNRandomQuestions(List<E> list, int n) {
        // make a new mutable list to:
        // 1. preserve original list
        // 2. avoid error in case original list is immutable
        list = new ArrayList<>(list);
        Collections.shuffle(list);
        Set<E> result = new HashSet<>();
        for (int i = 0; i < n; i++) {
            result.add(list.get(i));
        }
        return result;
    }

    private boolean insufficientQuestions(int questionsToPick, int availableQuestions) {
        return availableQuestions < questionsToPick;
    }
}
