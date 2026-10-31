
package com.ai.SpringAiDemo.auth;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email
) {}
