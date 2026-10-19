package com.example.market.common;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested domain resource cannot be found.
 *
 * @author Akhssassi Mohamed (Akhssassi18@gmail.com)
 * @since 2026-10-20
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object key) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", resource + " not found: " + key);
    }
}
