package com.quizme.services.questionspicker;

import com.quizme.entities.Question;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomPickerTest {

    QuestionRepo repo = mock(QuestionRepo.class);
    User user = mock(User.class);

    @Test
    void constructor_THROWS_IllegalArgumentEx_WHEN_contextMissing() {
        assertThrows(IllegalArgumentException.class, () -> new RandomPicker(new QuestionsPickerContext(null, repo)));
        assertThrows(IllegalArgumentException.class, () -> new RandomPicker(new QuestionsPickerContext(user, null)));
    }

    @Test
    void pick_THROWS_InsufficientQuestionsEx_WHEN_insufficientQuestions() {
        when(repo.findAllByUser(user)).thenReturn(List.of(mock(Question.class)));

        var picker = new RandomPicker(new QuestionsPickerContext(user, repo));

        // only 1 question available but 2 requested -> insufficient
        assertThrows(InsufficientQuestionsException.class, () -> picker.pick(2));
    }

    @Test
    void pick_RETURNS_requestedNumberOfUniqueQuestions_WHEN_sufficientQuestions() {
        var q1 = new Question(user, "q1", Set.of());
        var q2 = new Question(user, "q2", Set.of());
        var q3 = new Question(user, "q3", Set.of());

        when(repo.findAllByUser(user)).thenReturn(List.of(q1, q2, q3));

        var picker = new RandomPicker(new QuestionsPickerContext(user, repo));
        Set<Question> picked = picker.pick(2);

        assertEquals(2, picked.size());
        for (Question q : picked) {
            assertTrue(q == q1 || q == q2 || q == q3);
        }
    }

    @Test
    void pick_RETURNS_all_WHEN_requestedCountEqualsAvailableQuestionsCount() {
        var q1 = new Question(user, "q1", Set.of());
        var q2 = new Question(user, "q2", Set.of());
        var q3 = new Question(user, "q3", Set.of());

        when(repo.findAllByUser(user)).thenReturn(List.of(q1, q2, q3));

        var picker = new RandomPicker(new QuestionsPickerContext(user, repo));
        Set<Question> picked = picker.pick(3);

        assertEquals(3, picked.size());
        assertTrue(picked.containsAll(Set.of(q1, q2, q3)));
    }
}