package com.quizme.services.idempotency;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record IdempotencyRecord(@NonNull IdempotencyStatus status,
                                @NonNull String payloadHash,
                                @Nullable Object response) {
}
