package com.quizme.services;

import com.quizme.dto.NewQuizDto;
import com.quizme.dto.QuizDto;
import com.quizme.entities.Question;
import com.quizme.entities.Quiz;
import com.quizme.entities.User;
import com.quizme.repos.QuestionRepo;
import com.quizme.repos.QuizRepo;
import com.quizme.services.questionspicker.InsufficientQuestionsException;
import com.quizme.services.questionspicker.QuestionsPickerContext;
import com.quizme.services.questionspicker.QuestionsPickerFactory;
import com.quizme.services.result.Failure;
import com.quizme.services.result.FailureReason;
import com.quizme.services.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class QuizService {

    private final QuestionRepo questionRepo;
    private final QuizRepo quizRepo;
    private final QuestionsPickerFactory questionsPickerFactory;

    @Autowired
    public QuizService(
            QuestionRepo questionRepo,
            QuizRepo quizRepo
    ) {
        this(questionRepo, quizRepo, new QuestionsPickerFactory());
    }

    QuizService(
            QuestionRepo questionRepo,
            QuizRepo quizRepo,
            QuestionsPickerFactory questionsPickerFactory
    ) {
        this.questionRepo = questionRepo;
        this.quizRepo = quizRepo;
        this.questionsPickerFactory = questionsPickerFactory;
    }

    public Result<QuizDto> createQuiz(NewQuizDto requestDto, User user) {
        var questionsPicker = questionsPickerFactory.createPicker(requestDto.questionsPickingStrategy(),
                new QuestionsPickerContext(user, questionRepo));
        Set<Question> pickedQuestions;
        try {
            pickedQuestions = questionsPicker.pick(requestDto.questionsCount());
        } catch (InsufficientQuestionsException e) {
            return Result.failure(new Failure(FailureReason.VALIDATION_FAILED,
                    String.format("Requested quiz to contain %d questions, " +
                                    "but user has %d applicable questions only",
                            e.getRequestedCount(),
                            e.getAvailableCount())));
        }

        var quiz = new Quiz(user);
        quiz.setQuestions(pickedQuestions);
        quiz = quizRepo.save(quiz);

        return Result.success(QuizDto.fromEntity(quiz));
    }
}
