# Smart Campus — Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey)** for managing campus rooms, sensors, and their historical readings as part of the university's "Smart Campus" initiative.

## API Design Overview

The API follows RESTful architectural principles with a clear resource hierarchy:

```
/api/v1                          → Discovery endpoint (API metadata + HATEOAS links)
/api/v1/rooms                    → Room collection (GET all, POST new)
/api/v1/rooms/{roomId}           → Individual room (GET, DELETE)
/api/v1/sensors                  → Sensor collection (GET all with ?type filter, POST new)
/api/v1/sensors/{sensorId}       → Individual sensor (GET)
/api/v1/sensors/{sensorId}/readings → Sensor readings sub-resource (GET history, POST new)
```

### Core Data Models

| Model          | Key Fields                                         |
|----------------|---------------------------------------------------|
| **Room**       | `id`, `name`, `capacity`, `sensorIds`             |
| **Sensor**     | `id`, `type`, `status`, `currentValue`, `roomId`  |
| **SensorReading** | `id` (UUID), `timestamp` (epoch ms), `value`   |

### Error Handling Strategy

| Exception                        | HTTP Status            | Scenario                                    |
|----------------------------------|------------------------|---------------------------------------------|
| `RoomNotEmptyException`         | 409 Conflict           | Deleting a room with active sensors         |
| `LinkedResourceNotFoundException` | 422 Unprocessable Entity | Sensor references non-existent roomId       |
| `SensorUnavailableException`    | 403 Forbidden          | Posting readings to a MAINTENANCE sensor    |
| Generic `Throwable`             | 500 Internal Server Error | Catch-all safety net (no stack traces leaked) |

---

## Technology Stack

- **Language:** Java 17+
- **Framework:** JAX-RS (Jersey 2.41)
- **Server:** Grizzly Embedded HTTP Server
- **JSON Binding:** Jackson (via jersey-media-json-jackson)
- **Build Tool:** Apache Maven
- **Data Storage:** In-memory (`ConcurrentHashMap` + `ArrayList`)

---

## How to Build & Run

### Prerequisites

- **Java 17** or higher installed (`java -version`)
- **Apache Maven 3.6+** installed (`mvn -version`)

### Step 1: Clone the Repository

```bash
git clone https://github.com/<your-username>/CW1-Client-Server.git
cd CW1-Client-Server
```

### Step 2: Build the Project

```bash
mvn clean compile
```

### Step 3: Launch the Server

```bash
mvn exec:java
```

The server will start on `http://localhost:8080/api/v1/`.  
Press **Enter** in the terminal to stop the server.

---

## Sample `curl` Commands

### 1. Discovery Endpoint

```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### 2. List All Rooms

```bash
curl -s http://localhost:8080/api/v1/rooms | python3 -m json.tool
```

### 3. Create a New Room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "MED-101", "name": "Medical Wing Lab", "capacity": 25}' \
  | python3 -m json.tool
```

### 4. Get a Specific Room

```bash
curl -s http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 5. Register a New Sensor (with valid roomId)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "HUM-001", "type": "Humidity", "status": "ACTIVE", "currentValue": 45.0, "roomId": "ENG-102"}' \
  | python3 -m json.tool
```

### 6. List Sensors Filtered by Type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | python3 -m json.tool
```

### 7. Post a New Sensor Reading

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.7}' \
  | python3 -m json.tool
```

### 8. Get Sensor Reading History

```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings | python3 -m json.tool
```

### 9. Delete an Empty Room (Success — 204)

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/MED-101 -w "\nHTTP Status: %{http_code}\n"
```

### 10. Delete a Room with Sensors (Error — 409 Conflict)

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 11. Register Sensor with Invalid roomId (Error — 422)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "FAKE-001", "type": "Temperature", "status": "ACTIVE", "currentValue": 0, "roomId": "NONEXISTENT"}' \
  | python3 -m json.tool
```

### 12. Post Reading to MAINTENANCE Sensor (Error — 403)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 19.5}' \
  | python3 -m json.tool
```

---

## Project Structure

```
src/main/java/com/smartcampus/
├── SmartCampusApplication.java          # @ApplicationPath + Grizzly server
├── model/
│   ├── Room.java                        # Room POJO
│   ├── Sensor.java                      # Sensor POJO
│   └── SensorReading.java              # SensorReading POJO
├── repository/
│   └── DataStore.java                   # Thread-safe singleton (ConcurrentHashMap)
├── resource/
│   ├── DiscoveryResource.java           # GET /api/v1
│   ├── RoomResource.java               # /api/v1/rooms
│   ├── SensorResource.java             # /api/v1/sensors
│   └── SensorReadingResource.java      # Sub-resource for readings
├── exception/
│   ├── RoomNotEmptyException.java
│   ├── LinkedResourceNotFoundException.java
│   ├── SensorUnavailableException.java
│   └── mapper/
│       ├── RoomNotEmptyExceptionMapper.java         # → 409
│       ├── LinkedResourceNotFoundExceptionMapper.java  # → 422
│       ├── SensorUnavailableExceptionMapper.java    # → 403
│       └── GenericExceptionMapper.java              # → 500
└── filter/
    └── LoggingFilter.java               # Request + Response logging
```
