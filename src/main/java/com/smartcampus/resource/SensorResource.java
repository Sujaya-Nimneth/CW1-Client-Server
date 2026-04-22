package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * JAX-RS Resource class for managing Sensors at /api/v1/sensors.
 *
 * Supports:
 *   GET  /                       — List all sensors (with optional ?type= filter)
 *   POST /                       — Register a new sensor (validates roomId exists)
 *   GET  /{sensorId}             — Fetch a specific sensor
 *   {sensorId}/readings          — Sub-resource locator → SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * GET / — Lists all sensors.
     * Supports an optional query parameter 'type' for filtering (e.g., ?type=CO2).
     * If type is not provided, returns the full collection.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors;

        if (type != null && !type.trim().isEmpty()) {
            sensors = dataStore.getSensorsByType(type);
        } else {
            sensors = dataStore.getAllSensors();
        }

        return Response.ok(sensors).build();
    }

    /**
     * POST / — Registers a new sensor.
     *
     * Integrity Check:
     *   The roomId specified in the request body MUST reference an existing Room.
     *   If the Room does not exist, a LinkedResourceNotFoundException is thrown,
     *   which is mapped to HTTP 422 Unprocessable Entity.
     *
     * Side Effect:
     *   Upon successful creation, the sensor's ID is added to the parent Room's sensorIds list,
     *   maintaining referential integrity between sensors and rooms.
     *
     * We explicitly use @Consumes(MediaType.APPLICATION_JSON).
     * If a client sends a different media type (e.g., text/plain or application/xml),
     * JAX-RS will automatically return HTTP 415 Unsupported Media Type.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        // Validate that the referenced Room exists
        Room room = dataStore.getRoom(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: the referenced Room '" + sensor.getRoomId() +
                    "' does not exist. Please create the room first or use a valid roomId."
            );
        }

        // Check if a sensor with the same ID already exists
        if (dataStore.getSensor(sensor.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"CONFLICT\", \"message\": \"A sensor with ID '" + sensor.getId() + "' already exists.\", \"status\": 409}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Persist the sensor
        dataStore.addSensor(sensor);

        // Link the sensor to the room (maintain referential integrity)
        room.getSensorIds().add(sensor.getId());

        // Build Location URI: /api/v1/sensors/{sensorId}
        URI locationUri = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();

        return Response.created(locationUri).entity(sensor).build();
    }

    /**
     * GET /{sensorId} — Fetches a specific sensor by ID.
     * Returns 404 if the sensor does not exist.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"NOT_FOUND\", \"message\": \"Sensor '" + sensorId + "' does not exist.\", \"status\": 404}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * Sub-Resource Locator for sensor readings.
     *
     * This method delegates all requests matching /sensors/{sensorId}/readings
     * to a dedicated SensorReadingResource class. This is the Sub-Resource Locator pattern:
     *   - The method has @Path but NO HTTP method annotation (@GET, @POST, etc.)
     *   - It returns an instance of another resource class
     *   - JAX-RS delegates the remaining path resolution to that class
     *
     * This pattern keeps code modular and avoids a single monolithic controller.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadings(@PathParam("sensorId") String sensorId) {
        // Verify the sensor exists before delegating
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' does not exist.");
        }
        return new SensorReadingResource(sensorId);
    }
}
