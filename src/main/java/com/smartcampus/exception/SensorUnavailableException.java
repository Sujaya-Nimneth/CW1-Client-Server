/*
 * Author : P.R.S.N.P. De Silva
 * IIT ID : 20240744
 */
package com.smartcampus.exception;

/**
 * Thrown when an operation is attempted on a sensor that is currently
 * in a state that prevents it from accepting the operation.
 *
 * Example: Attempting to POST a new reading to a sensor that has
 * a status of "MAINTENANCE" — the sensor is physically disconnected.
 *
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}
