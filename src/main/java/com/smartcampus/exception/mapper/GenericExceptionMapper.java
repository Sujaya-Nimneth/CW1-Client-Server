/*
 * Author : P.R.S.N.P. De Silva
 * IIT ID : 20240744
 */
package com.smartcampus.exception.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "catch-all" Exception Mapper for any uncaught Throwable.
 *
 * This acts as a safety net to ensure the API is "leak-proof" — it should
 * NEVER return a raw Java stack trace or a default server error page to the client.
 *
 * All unexpected runtime errors (NullPointerException, IndexOutOfBoundsException,
 * IllegalStateException, etc.) are caught here and mapped to a generic
 * HTTP 500 Internal Server Error with a clean, structured JSON response.
 *
 * Security Note:
 *   Exposing internal stack traces to external API consumers is a significant
 *   cybersecurity risk. An attacker could learn:
 *     - Internal package structure and class names
 *     - Third-party libraries and their versions (exploitable CVEs)
 *     - Database connection strings or file paths
 *     - Business logic flow and potential injection points
 *   By returning a generic error message, we prevent information leakage.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full stack trace on the server side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GenericExceptionMapper", exception);

        // Return a clean, generic error to the client — no internal details leaked
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "INTERNAL_SERVER_ERROR");
        errorBody.put("message", "An unexpected error occurred on the server. Please try again later.");
        errorBody.put("status", 500);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorBody)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
