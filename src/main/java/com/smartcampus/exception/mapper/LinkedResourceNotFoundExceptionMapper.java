package com.smartcampus.exception.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception Mapper for LinkedResourceNotFoundException.
 *
 * Maps the exception to HTTP 422 Unprocessable Entity with a structured JSON error body.
 * This is triggered when a client submits a valid JSON payload that references a
 * non-existent linked resource (e.g., a sensor with an invalid roomId).
 *
 * Why 422 and not 404?
 *   - 404 means the URL itself was not found.
 *   - 422 means the server understood the request and the JSON structure is valid,
 *     but the semantic content cannot be processed because a referenced entity is missing.
 *   - This distinction is important for API consumers debugging integration issues.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "UNPROCESSABLE_ENTITY");
        errorBody.put("message", exception.getMessage());
        errorBody.put("status", 422);

        return Response.status(422)
                .entity(errorBody)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
