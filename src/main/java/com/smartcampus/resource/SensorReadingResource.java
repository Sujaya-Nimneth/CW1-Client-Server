package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Sub-Resource class for managing SensorReadings.
 *
 * This class is NOT annotated with @Path — it is a sub-resource, instantiated
 * by the SensorResource's sub-resource locator method. The sensorId is passed
 * via the constructor, scoping all operations to a specific sensor's readings.
 *
 * Supports:
 *   GET  /   — Retrieve the historical log of all readings for this sensor
 *   POST /   — Record a new reading (side effect: updates parent Sensor.currentValue)
 */
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore dataStore = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET / — Returns the entire historical log of readings for this sensor.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings() {
        List<SensorReading> readings = dataStore.getReadings(sensorId);
        return Response.ok(readings).build();
    }

    /**
     * POST / — Appends a new reading to this sensor's historical log.
     *
     * State Constraint:
     *   If the sensor's status is "MAINTENANCE", the sensor is physically disconnected
     *   and cannot accept new readings. A SensorUnavailableException is thrown,
     *   which is mapped to HTTP 403 Forbidden.
     *
     * Side Effect:
     *   A successful POST triggers an update to the parent Sensor's currentValue field,
     *   ensuring data consistency across the entire API. The currentValue always reflects
     *   the most recently recorded reading.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = dataStore.getSensor(sensorId);

        // State constraint check: sensor must not be in MAINTENANCE mode
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently in MAINTENANCE mode and cannot accept new readings. " +
                    "Please wait until the sensor is restored to ACTIVE status."
            );
        }

        // Auto-generate ID and timestamp if not provided by the client
        if (reading.getId() == null || reading.getId().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        dataStore.addReading(sensorId, reading);

        // Side effect: update the parent sensor's currentValue to the latest reading
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
