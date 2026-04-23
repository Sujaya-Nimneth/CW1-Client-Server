/*
 * Author : P.R.S.N.P. De Silva
 * IIT ID : 20240744
 */
package com.smartcampus.exception.mapper;

import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception Mapper for RoomNotEmptyException.
 *
 * Maps the exception to HTTP 409 Conflict with a structured JSON error body.
 * This is triggered when a client attempts to delete a Room that still has
 * active sensors assigned to it, preventing data orphans.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "CONFLICT");
        errorBody.put("message", exception.getMessage());
        errorBody.put("status", 409);

        return Response.status(Response.Status.CONFLICT)
                .entity(errorBody)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
