package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS Filter that provides API observability by logging every incoming
 * request and outgoing response.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter as a single
 * class, ensuring consistent logging across the entire request/response lifecycle.
 *
 * Using JAX-RS filters for cross-cutting concerns like logging is advantageous
 * because:
 *   1. It applies uniformly to ALL endpoints without modifying any resource class.
 *   2. It follows the Separation of Concerns principle — resource methods focus
 *      on business logic, not infrastructure concerns.
 *   3. It can be added, removed, or modified independently of the API logic.
 *   4. New endpoints automatically get logging without any additional code.
 *
 * This is far superior to manually inserting Logger.info() statements inside
 * every single resource method, which is error-prone, violates DRY, and makes
 * it easy to forget logging for new endpoints.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Logs the HTTP method and URI for every incoming request.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(">> REQUEST:  " + method + " " + uri);
    }

    /**
     * Logs the final HTTP status code for every outgoing response.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        int status = responseContext.getStatus();
        LOGGER.info("<< RESPONSE: " + method + " " + uri + " — Status: " + status);
    }
}
