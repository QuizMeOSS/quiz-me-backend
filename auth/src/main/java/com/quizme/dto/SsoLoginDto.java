package com.quizme.dto;

public record SsoLoginDto(String email, String username,
                          String provider, String providerUserId) {}
