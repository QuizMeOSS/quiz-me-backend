package com.quizme.services.questionspicker;

import com.quizme.entities.Question;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomPickerTest {

    QuestionRepo repo = mock(QuestionRepo.class);
    static User user = mock(User.class);
    RandomPicker picker = new RandomPicker(new QuestionsPickerContext(user, repo));

    // some dummy questions to be used in tests
    static Question q1 = new Question(user, "q1", Set.of());
    static Question q2 = new Question(user, "q2", Set.of());
    static Question q3 = new Question(user, "q3", Set.of());
    static Question q4 = new Question(user, "q4", Set.of());
    static Question q5 = new Question(user, "q5", Set.of());

    @Test
    void constructor_THROWS_IllegalArgumentEx_WHEN_contextMissing() {
        assertThrows(IllegalArgumentException.class, () -> new RandomPicker(new QuestionsPickerContext(null, repo)));
        assertThrows(IllegalArgumentException.class, () -> new RandomPicker(new QuestionsPickerContext(user, null)));
    }

    @Test
    void pick_THROWS_InsufficientQuestionsEx_WHEN_insufficientQuestions() {
        when(repo.findAllWithChoicesByUser(user)).thenReturn(List.of(mock(Question.class)));

        // only 1 question available but 2 requested -> insufficient
        assertThrows(InsufficientQuestionsException.class, () -> picker.pick(2));
    }

    @Test
    void pick_RETURNS_requestedNumberOfUniqueQuestions_WHEN_sufficientQuestions() {
        when(repo.findAllWithChoicesByUser(user)).thenReturn(List.of(q1, q2, q3));

        Set<Question> picked = picker.pick(2);

        assertEquals(2, picked.size());
    }

    @Test
    void pick_RETURNS_all_WHEN_requestedCountEqualsAvailableQuestionsCount() {
        when(repo.findAllWithChoicesByUser(user)).thenReturn(List.of(q1, q2, q3));

        Set<Question> picked = picker.pick(3);

        assertEquals(3, picked.size());
        assertTrue(picked.containsAll(Set.of(q1, q2, q3)));
    }

    @ParameterizedTest
    @MethodSource("seedCases")
    void pick_RETURNS_randomQuestions_WHEN_requestedCountLessThanAvailableQuestionsCount(
            int seed, Set<Question> expected
    ) {
        when(repo.findAllWithChoicesByUser(user)).thenReturn(List.of(q1, q2, q3, q4, q5));
        picker = new RandomPicker(new QuestionsPickerContext(user, repo), new Random(seed));

        Set<Question> picked = picker.pick(3);

        assertEquals(3, picked.size());
        assertTrue(picked.containsAll(expected));
    }

    /**
     * just a couple of cases to ensure output is indeed random
     * @return seed and expected picked questions
     */
    static Stream<Arguments> seedCases() {
        return Stream.of(
                Arguments.of(7, Set.of(q1, q4, q5)),
                Arguments.of(123, Set.of(q2, q4, q5))
        );
    }
}