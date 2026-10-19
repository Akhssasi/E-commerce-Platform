package com.example.market.auth;

/**
 * Response payload returned after a successful login.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record AuthResponse(String accessToken, String tokenType, String fullName, String role) {

    public static AuthResponse bearer(String token, User user) {
        return new AuthResponse(token, "Bearer", user.getFullName(), user.getRole().name());
    }
}
