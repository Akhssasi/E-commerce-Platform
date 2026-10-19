package com.example.market.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for registering a new customer account.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 160) String fullName,
        @NotBlank @Size(min = 8, max = 72) String password
) { }
