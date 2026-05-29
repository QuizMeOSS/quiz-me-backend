package com.quizme.utils;

import com.quizme.entities.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ControllerUtils {
    @Nullable
    public static String getUserIdempotencyKey(@NonNull User user, @Nullable String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return user.getUsername() + "_" + key;
    }
}
