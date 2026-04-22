package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root "Discovery" endpoint for the Smart Campus API.
 *
 * Provides essential API metadata including versioning info, administrative contact details,
 * and a map of primary resource collections (HATEOAS-style navigation).
 *
 * This allows client developers to programmatically discover available resources
 * without relying solely on static documentation.
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiDiscovery() {
        Map<String, Object> discovery = new LinkedHashMap<>();

        // API metadata
        discovery.put("name", "Smart Campus Sensor & Room Management API");
        discovery.put("version", "1.0.0");
        discovery.put("description", "A RESTful API for managing campus rooms, sensors, and their historical readings.");

        // Administrative contact
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("department", "Campus Facilities Management");
        contact.put("email", "smartcampus@university.edu");
        contact.put("support", "https://university.edu/smartcampus/support");
        discovery.put("contact", contact);

        // Resource map — HATEOAS-style links to primary resource collections
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        discovery.put("resources", resources);

        return Response.ok(discovery).build();
    }
}
