package com.smartcampus.repository;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe singleton that serves as the in-memory data store for the Smart Campus API.
 * Uses ConcurrentHashMap to prevent data loss and race conditions across concurrent requests.
 *
 * This replaces a traditional database — no SQL Server or similar technology is used.
 */
public class DataStore {

    private static DataStore instance;

    // Primary data structures
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        initSampleData();
    }

    /**
     * Returns the singleton instance of the DataStore.
     * Synchronized to prevent multiple instances from being created in a multi-threaded environment.
     */
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // ==================== Room Operations ====================

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // ==================== Sensor Operations ====================

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }

    public List<Sensor> getSensorsByType(String type) {
        return sensors.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Also initialize an empty readings list for this sensor
        sensorReadings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public Sensor removeSensor(String id) {
        sensorReadings.remove(id);
        return sensors.remove(id);
    }

    // ==================== Sensor Reading Operations ====================

    public List<SensorReading> getReadings(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }

    // ==================== Sample Data Initialization ====================

    private void initSampleData() {
        // --- Rooms ---
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        Room eng102 = new Room("ENG-102", "Engineering Lab A", 30);
        Room sci201 = new Room("SCI-201", "Science Lecture Hall", 120);

        addRoom(lib301);
        addRoom(eng102);
        addRoom(sci201);

        // --- Sensors ---
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor co2001 = new Sensor("CO2-001", "CO2", "ACTIVE", 415.0, "LIB-301");
        Sensor occ001 = new Sensor("OCC-001", "Occupancy", "ACTIVE", 15.0, "ENG-102");
        Sensor temp002 = new Sensor("TEMP-002", "Temperature", "MAINTENANCE", 0.0, "SCI-201");

        addSensor(temp001);
        addSensor(co2001);
        addSensor(occ001);
        addSensor(temp002);

        // Link sensors to rooms
        lib301.getSensorIds().add("TEMP-001");
        lib301.getSensorIds().add("CO2-001");
        eng102.getSensorIds().add("OCC-001");
        sci201.getSensorIds().add("TEMP-002");

        // --- Sample Readings for TEMP-001 ---
        SensorReading r1 = new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 60000, 21.8);
        SensorReading r2 = new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 30000, 22.1);
        SensorReading r3 = new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis(), 22.5);

        addReading("TEMP-001", r1);
        addReading("TEMP-001", r2);
        addReading("TEMP-001", r3);
    }
}
