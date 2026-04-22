package com.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single historical reading captured by a sensor.
 * Each reading has a unique event ID (UUID), a timestamp in epoch milliseconds,
 * and the actual metric value recorded by the hardware.
 */
public class SensorReading {

    private String id;              // Unique reading event ID (UUID recommended)
    private long timestamp;         // Epoch time (ms) when the reading was captured
    private double value;           // The actual metric value recorded by the hardware

    // No-arg constructor required for JSON deserialization
    public SensorReading() {
    }

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
