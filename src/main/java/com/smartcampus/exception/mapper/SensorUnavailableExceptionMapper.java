package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception Mapper for SensorUnavailableException.
 *
 * Maps the exception to HTTP 403 Forbidden with a structured JSON error body.
 * This is triggered when a client attempts to POST a new reading to a sensor
 * that is currently in "MAINTENANCE" mode — the sensor is physically disconnected
 * and cannot accept new data.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "FORBIDDEN");
        errorBody.put("message", exception.getMessage());
        errorBody.put("status", 403);

        return Response.status(Response.Status.FORBIDDEN)
                .entity(errorBody)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
