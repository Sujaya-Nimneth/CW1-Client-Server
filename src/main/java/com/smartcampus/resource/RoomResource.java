package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * JAX-RS Resource class for managing Rooms at /api/v1/rooms.
 *
 * Supports:
 *   GET  /              — List all rooms
 *   POST /              — Create a new room (returns 201 Created with Location header)
 *   GET  /{roomId}      — Fetch a specific room by ID
 *   DELETE /{roomId}    — Delete a room (blocked if sensors are still assigned → 409 Conflict)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * GET / — Returns a comprehensive list of all rooms.
     * Returns full room objects (not just IDs) to reduce client-side round trips.
     */
    @GET
    public Response getAllRooms() {
        List<Room> rooms = dataStore.getAllRooms();
        return Response.ok(rooms).build();
    }

    /**
     * POST / — Creates a new room.
     * Returns HTTP 201 Created with a Location header pointing to the new resource.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        // Check if a room with the same ID already exists
        if (dataStore.getRoom(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"CONFLICT\", \"message\": \"A room with ID '" + room.getId() + "' already exists.\", \"status\": 409}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        dataStore.addRoom(room);

        // Build the Location URI: /api/v1/rooms/{roomId}
        URI locationUri = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();

        return Response.created(locationUri).entity(room).build();
    }

    /**
     * GET /{roomId} — Fetches detailed metadata for a specific room.
     * Returns 404 if the room does not exist.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"NOT_FOUND\", \"message\": \"Room '" + roomId + "' does not exist.\", \"status\": 404}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /{roomId} — Deletes a room by ID.
     *
     * Business Logic Constraint:
     *   A room cannot be deleted if it still has active sensors assigned to it.
     *   This prevents data orphans. If sensors are present, a RoomNotEmptyException
     *   is thrown, which is mapped to HTTP 409 Conflict.
     *
     * Idempotency:
     *   The first DELETE removes the room and returns 204 No Content.
     *   Subsequent DELETEs for the same ID return 404 Not Found — the server state
     *   is unchanged (the room is already gone), so the operation is still idempotent.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoom(roomId);

        // Room doesn't exist — return 404 (idempotent: server state unchanged)
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"NOT_FOUND\", \"message\": \"Room '" + roomId + "' does not exist.\", \"status\": 404}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Business rule: cannot delete a room that still has sensors assigned
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted because it still has " +
                    room.getSensorIds().size() + " active sensor(s) assigned: " + room.getSensorIds()
            );
        }

        dataStore.removeRoom(roomId);
        return Response.noContent().build();    // 204 No Content
    }
}
