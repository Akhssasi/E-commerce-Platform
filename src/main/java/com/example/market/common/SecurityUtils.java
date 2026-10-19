package com.example.market.common;

import com.example.market.auth.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility for accessing the currently authenticated user from the security context.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public final class SecurityUtils {

    private SecurityUtils() { }

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw ApiException.forbidden("Authentication is required");
        }
        return (User) auth.getPrincipal();
    }

    public static String currentEmail() {
        return currentUser().getEmail();
    }
}
