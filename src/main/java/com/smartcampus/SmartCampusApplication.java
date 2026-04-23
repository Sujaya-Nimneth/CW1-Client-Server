/*
 * Author : P.R.S.N.P. De Silva
 * IIT ID : 20240744
 */
package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the Smart Campus REST API.
 *
 * Extends ResourceConfig (which is a subclass of javax.ws.rs.core.Application)
 * and uses the @ApplicationPath annotation to set the versioned API base path.
 *
 * The embedded Grizzly HTTP server is started in the main() method,
 * making the application self-contained — no external servlet container required.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    private static final Logger LOGGER = Logger.getLogger(SmartCampusApplication.class.getName());

    public SmartCampusApplication() {
        // Scan these packages for JAX-RS resources, exception mappers, and filters
        packages(
            "com.smartcampus.resource",
            "com.smartcampus.exception.mapper",
            "com.smartcampus.filter"
        );
        LOGGER.info("Smart Campus Application initialized — resources, mappers, and filters registered.");
    }

    /**
     * Starts the Grizzly embedded HTTP server.
     */
    public static void main(String[] args) throws IOException {
        final String BASE_URI = "http://localhost:8080/api/v1/";

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                new SmartCampusApplication()
        );

        LOGGER.log(Level.INFO, "\n" +
            "============================================================\n" +
            "  Smart Campus API is running!\n" +
            "  Base URI : {0}\n" +
            "  Discovery: {0}\n" +
            "  Rooms    : {0}rooms\n" +
            "  Sensors  : {0}sensors\n" +
            "============================================================\n" +
            "  Press ENTER to stop the server...\n" +
            "============================================================",
            BASE_URI
        );

        System.in.read();
        server.shutdownNow();
        LOGGER.info("Smart Campus API server stopped.");
    }
}
