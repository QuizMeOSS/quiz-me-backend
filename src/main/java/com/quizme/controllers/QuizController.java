package com.quizme.controllers;

import com.quizme.dto.NewQuizDto;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.services.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final UserRepo userRepo;
    private final QuizService quizService;
    private final ResultToResponseEntityMapper responseMapper;

    public QuizController(
            UserRepo userRepo,
            QuizService quizService,
            ResultToResponseEntityMapper responseMapper
    ) {
        this.userRepo = userRepo;
        this.quizService = quizService;
        this.responseMapper = responseMapper;
    }

    @PostMapping("/new")
    public ResponseEntity<?> createQuiz(
            @RequestBody NewQuizDto requestDto,
            @AuthenticationPrincipal UserDetails authUser,
            HttpServletRequest request) {
        var user = userRepo.findByEmail(authUser.getUsername())
                .get(); // since we were able to authenticate user, then they exist

        var result = quizService.createQuiz(requestDto, user);
        if (result.failure() != null) {
            return responseMapper.map(result, request.getRequestURI());
        }
        return ResponseEntity.ok(result.success());
    }
}
