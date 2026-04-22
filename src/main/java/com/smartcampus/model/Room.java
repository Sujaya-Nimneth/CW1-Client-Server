package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical room on campus.
 * Each room has a unique identifier, a human-readable name,
 * a maximum occupancy capacity, and a list of sensor IDs deployed within it.
 */
public class Room {

    private String id;                                          // Unique identifier, e.g., "LIB-301"
    private String name;                                        // Human-readable name, e.g., "Library Quiet Study"
    private int capacity;                                       // Maximum occupancy for safety regulations
    private List<String> sensorIds = new ArrayList<>();         // IDs of sensors deployed in this room

    // No-arg constructor required for JSON deserialization
    public Room() {
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.sensorIds = new ArrayList<>();
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}
