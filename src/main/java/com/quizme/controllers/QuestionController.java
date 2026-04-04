package com.quizme.controllers;

import com.quizme.dto.QuestionDto;
import com.quizme.dto.NewQuestionDto;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.services.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final ResultToResponseEntityMapper responseMapper;
    private final QuestionService questionService;
    private final UserRepo userRepo;

    public QuestionController(
            ResultToResponseEntityMapper responseMapper,
            QuestionService questionService,
            UserRepo userRepo
    ) {
        this.responseMapper = responseMapper;
        this.questionService = questionService;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> newQuestion(
            @RequestBody NewQuestionDto requestDto,
            @AuthenticationPrincipal UserDetails authUser,
            HttpServletRequest request
    ) {
        var user = userRepo.findByEmail(authUser.getUsername())
                .get(); // since we were able to authenticate user, then they exist

        var result = questionService.createQuestion(requestDto, user);

        if (result.failure() != null) {
            return responseMapper.map(result, request.getRequestURI());
        }

        return ResponseEntity.ok(QuestionDto.fromEntity(result.success()));
    }
}
