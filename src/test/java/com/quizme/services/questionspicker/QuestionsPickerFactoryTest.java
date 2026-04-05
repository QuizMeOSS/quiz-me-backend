package com.quizme.services.questionspicker;

import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class QuestionsPickerFactoryTest {

    QuestionRepo repo = mock(QuestionRepo.class);
    User user = mock(User.class);

    @Test
    void createPicker_returnsRandomPicker_whenStrategyRandom() {
        var factory = new QuestionsPickerFactory();
        var context = new QuestionsPickerContext(user, repo);

        QuestionsPicker picker = factory.createPicker(QuestionsPicker.Strategy.RANDOM, context);

        assertThat(picker).isInstanceOf(RandomPicker.class);
    }

    @Test
    void createPicker_throwsIllegalArgument_whenStrategyIsNull() {
        var factory = new QuestionsPickerFactory();
        var context = new QuestionsPickerContext(user, repo);

        assertThrows(IllegalArgumentException.class, () -> factory.createPicker(null, context));
    }
}
