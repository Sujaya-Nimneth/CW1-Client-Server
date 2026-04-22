package com.smartcampus.exception;

/**
 * Thrown when an attempt is made to delete a Room that still has
 * active sensors assigned to it.
 *
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
