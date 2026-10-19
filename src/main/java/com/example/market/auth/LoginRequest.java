package com.example.market.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for authenticating an existing user.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) { }
