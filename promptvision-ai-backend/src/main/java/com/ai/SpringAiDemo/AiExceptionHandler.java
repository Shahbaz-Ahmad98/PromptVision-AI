
package com.ai.SpringAiDemo;

import com.ai.SpringAiDemo.chat.ChatHistoryController;
import com.openai.errors.InternalServerException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(assignableTypes = {
        GenAIController.class,
        ChatHistoryController.class
})
public class AiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(AiExceptionHandler.class);

    public record ApiError(
            int status,
            String message,
            LocalDateTime timestamp
    ) {}

    // ==========================================
    // GEMINI RATE LIMIT / QUOTA
    // ==========================================

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimit(
            RateLimitException ex
    ) {
        log.warn("AI provider rate limit: {}", ex.getMessage());

        return error(
                HttpStatus.TOO_MANY_REQUESTS,
                "Gemini request limit reached. Please try again later."
        );
    }

    // ==========================================
    // GEMINI SERVER ERROR / OVERLOAD
    // ==========================================

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ApiError> handleProviderError(
            InternalServerException ex
    ) {
        log.warn("AI provider unavailable: {}", ex.getMessage());

        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini is temporarily unavailable. Please try again shortly."
        );
    }

    // ==========================================
    // INVALID GEMINI API KEY
    // ==========================================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleProviderAuth(
            UnauthorizedException ex
    ) {
        log.error("AI provider authentication failed");

        return error(
                HttpStatus.BAD_GATEWAY,
                "AI provider authentication failed. Please contact the administrator."
        );
    }

    // ==========================================
    // BUILD ERROR RESPONSE
    // ==========================================

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(
                        status.value(),
                        message,
                        LocalDateTime.now()
                ));
    }
}
