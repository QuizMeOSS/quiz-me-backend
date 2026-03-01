package com.quizme.controllers;

import com.quizme.dto.CreatedCategoryDto;
import com.quizme.dto.NewCategoryDto;
import com.quizme.mappers.ResultToResponseEntityMapper;
import com.quizme.repos.UserRepo;
import com.quizme.services.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/categories")
public class CategoryController {


    private final ResultToResponseEntityMapper responseMapper;
    private final CategoryService categoryService;
    private final UserRepo userRepo;

    public CategoryController(
            ResultToResponseEntityMapper responseMapper,
            CategoryService categoryService,
            UserRepo userRepo
    ) {
        this.responseMapper = responseMapper;
        this.categoryService = categoryService;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> newCategory(
            @RequestBody NewCategoryDto requestDto,
            @AuthenticationPrincipal UserDetails authUser,
            HttpServletRequest request
    ) {
        var user = userRepo.findByEmail(authUser.getUsername())
                .get(); // since we were able to authenticate user, then they exist
        var result = categoryService.createCategory(requestDto, user);

        if (result.failure() != null) {
            return responseMapper.map(result, request.getRequestURI());
        }

        return ResponseEntity.ok(result.success());
    }

    @GetMapping
    public ResponseEntity<Collection<CreatedCategoryDto>> getCategories(
            @AuthenticationPrincipal UserDetails authUser
    ) {
        var user = userRepo.findByEmail(authUser.getUsername())
                .get(); // since we were able to authenticate user, then they exist

        var categories = categoryService.getCategories(user);

        return ResponseEntity.ok(categories);
    }
}
