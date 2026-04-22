package com.smartcampus.exception;

/**
 * Thrown when a resource references another resource (via a foreign key)
 * that does not exist in the system.
 *
 * Example: A sensor is being created with a roomId that does not map to any existing Room.
 *
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 *
 * Why 422 instead of 404?
 *   A 404 indicates the requested URL itself was not found. In this case, the URL
 *   (e.g., POST /api/v1/sensors) is perfectly valid — the issue is that a referenced
 *   resource embedded inside a valid JSON payload doesn't exist. HTTP 422 more accurately
 *   conveys that the server understood the request structure but cannot process the
 *   semantic content.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
