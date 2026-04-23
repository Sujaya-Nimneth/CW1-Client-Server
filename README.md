**Author:** P.R.S.N.P. De Silva
**IIT ID:** 20240744

---

# Smart Campus — Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey)** for managing campus rooms, sensors, and their historical readings as part of the university's "Smart Campus" initiative.

---

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

---

---

# Conceptual Report

This section contains written answers to the conceptual questions posed in each task of the coursework.

---

## Part 1: Service Architecture & Setup

### Task 1.1 — JAX-RS Resource Class Lifecycle and In-Memory Synchronisation

**Question:** *In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronise your in-memory data structures (maps/lists) to prevent data loss or race conditions.*

**Answer:**

By default, JAX-RS follows a **per-request lifecycle** for resource classes. This means that every time an HTTP request arrives at an endpoint (e.g., `GET /api/v1/rooms`), the JAX-RS runtime (Jersey, in our case) creates a **brand-new instance** of the corresponding resource class (e.g., `RoomResource`), uses it to handle that single request, and then discards it. The resource class is never reused across requests; each request gets its own isolated instance. This is fundamentally different from a Servlet, which is typically a singleton. The per-request model exists because JAX-RS resource classes are designed to be lightweight — they hold no conversational state between requests, which aligns perfectly with REST's stateless constraint.

This default lifecycle has a **critical implication** for data management: since each resource instance is short-lived and destroyed after the request completes, **no data can be stored as instance fields of the resource class itself**, because that data would be lost the moment the object is garbage-collected. If we naively declared a `HashMap<String, Room> rooms` as an instance field inside `RoomResource`, every incoming request would see a fresh, empty map — a POST that adds a room would succeed, but a subsequent GET would return nothing because it operates on a completely different `RoomResource` instance with its own empty map.

To solve this, our implementation uses the **Singleton pattern** for the `DataStore` class. The `DataStore.getInstance()` method returns the same shared instance across all resource objects, ensuring that every request — regardless of which resource instance handles it — reads from and writes to the **same underlying data**. The data structures inside `DataStore` (`rooms`, `sensors`, `sensorReadings`) are therefore shared state.

However, shared state introduces **thread-safety concerns**. In a production JAX-RS environment, the Grizzly HTTP server processes requests concurrently using multiple threads. If two requests simultaneously try to read and write to a standard `HashMap`, they could corrupt the internal structure of the map (e.g., infinite loops in the hash bucket chain, lost entries, or `ConcurrentModificationException`). To prevent data loss and race conditions, we use `ConcurrentHashMap` instead of `HashMap`. `ConcurrentHashMap` achieves thread-safety through **lock striping** — rather than locking the entire map, it divides the internal hash table into segments and locks only the segment being modified. This allows multiple threads to read and write to different segments simultaneously, providing both safety and high concurrency. The `getInstance()` method on `DataStore` is also marked `synchronized` to ensure that only one thread can create the singleton instance, preventing a race condition where two threads could each create their own `DataStore`.

In summary: the per-request lifecycle keeps resource classes stateless and simple, while externalising all shared state into a thread-safe singleton ensures data integrity under concurrent access.

---

### Task 1.2 — Hypermedia (HATEOAS) and Its Benefits Over Static Documentation

**Question:** *Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?*

**Answer:**

HATEOAS (**H**ypermedia **A**s **T**he **E**ngine **O**f **A**pplication **S**tate) is the highest maturity level in the Richardson Maturity Model (Level 3) and is considered a hallmark of truly RESTful APIs. The core principle is that the server should embed navigational links directly within its responses, guiding the client on what actions are available and how to reach related resources — just as hyperlinks in an HTML page guide a human user through a website. In our implementation, the Discovery endpoint (`GET /api/v1`) returns a JSON object containing a `resources` map with links like `"rooms" → "/api/v1/rooms"` and `"sensors" → "/api/v1/sensors"`, allowing any client to programmatically discover the API's entry points.

**Benefits over static documentation:**

1. **Self-Discoverability:** A client can start from a single root URL (`/api/v1`) and dynamically discover all available resources by following the embedded links. This eliminates the need to hardcode URLs or consult external documentation to find endpoints. If a URL structure changes, the server simply updates the links it returns, and well-behaved clients follow the new links automatically.

2. **Reduced Client-Server Coupling:** Without HATEOAS, clients must hardcode specific URL patterns (e.g., `"/api/v1/rooms/" + roomId`), creating tight coupling to the server's URL structure. With HATEOAS, the client only needs to know the root URL and the link relation names (e.g., `"rooms"`, `"sensors"`). The server can restructure its URL hierarchy (e.g., from `/api/v1/rooms` to `/api/v2/campus-rooms`) without breaking clients, as long as the link relation names remain the same.

3. **Eliminates Stale Documentation:** Static documentation (e.g., a Swagger/OpenAPI spec hosted on a wiki) can become outdated when the API evolves. If a developer adds a new endpoint but forgets to update the documentation, clients will not know it exists. HATEOAS eliminates this problem because the API's capabilities are always communicated in real-time through the responses themselves — the documentation *is* the API.

4. **Dynamic Workflow Guidance:** Beyond simple navigation, HATEOAS can communicate which operations are contextually valid. For example, a sensor in `MAINTENANCE` status could omit the link to `POST /readings`, signalling to the client that posting readings is not currently available. This is far more powerful than static documentation, which cannot represent dynamic, state-dependent behaviour.

5. **Simplified Onboarding:** A new developer integrating with the API can simply `curl` the root endpoint and immediately see all available resources, their URIs, and even administrative contact information — everything needed to start exploring the API without reading a single page of external documentation.

In essence, HATEOAS transforms the API from a rigid contract defined by static docs into a **navigable, self-describing system** that adapts to change and guides clients through its capabilities at runtime.

---

## Part 2: Room Management

### Task 2.1 — Returning Full Objects vs. IDs: Trade-offs in API Design

**Question:** *When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.*

**Answer:**

When designing the `GET /api/v1/rooms` endpoint, there are two main approaches: returning a list of room IDs (e.g., `["LIB-301", "ENG-102", "SCI-201"]`) or returning the full room objects with all their fields. Each approach involves trade-offs around network bandwidth, client-side processing, and developer experience.

**Returning Only IDs:**

- **Lower Bandwidth:** The response payload is significantly smaller. For a campus with thousands of rooms, returning `["LIB-301", "ENG-102", ...]` is far more compact than including every room's name, capacity, and sensor list. This is beneficial in bandwidth-constrained environments (e.g., mobile networks).
- **Higher Client-Side Complexity (The N+1 Problem):** If the client needs details about each room (which it almost always does to display a useful list), it must make a separate `GET /api/v1/rooms/{id}` request for every ID returned. For 100 rooms, this means 1 initial request + 100 follow-up requests = **101 HTTP round trips**. This is the classic "N+1 request problem," which dramatically increases total latency and server load.
- **Increased Client-Side Code:** The client must implement pagination, batching, or parallel request logic to efficiently fetch the details, adding significant complexity to the client codebase.

**Returning Full Objects (Our Approach):**

- **Higher Bandwidth:** The response payload is larger because it includes all fields (name, capacity, sensorIds) for every room. However, for most APIs this is acceptable — a list of even 1,000 room objects with a few fields each is still only a few hundred kilobytes, which is trivial for modern networks.
- **Zero Follow-Up Requests:** The client receives all the information it needs in a single HTTP round trip. It can immediately render a list of rooms with their names, capacities, and sensor counts without making any additional requests. This minimises latency and simplifies the client code.
- **Better User Experience:** The client application can display meaningful information (room names, occupancy counts) instantly, rather than showing placeholder "Loading..." indicators while it fetches details for each room individually.

**Our design decision:** We chose to return full room objects because the Smart Campus API manages a moderate number of rooms, and the per-room payload is small (a few fields each). The bandwidth savings of returning only IDs would be negligible, while the N+1 problem would significantly degrade client performance and developer experience. For APIs with extremely large datasets, supporting pagination (e.g., `?page=2&size=50`) or offering a "sparse fieldset" feature (e.g., `?fields=id,name`) would be more appropriate than forcing clients into the N+1 pattern.

---

### Task 2.2 — DELETE Idempotency

**Question:** *Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.*

**Answer:**

Yes, the `DELETE /{roomId}` operation in our implementation **is idempotent**, and this is a fundamental requirement of RESTful HTTP semantics as defined in RFC 7231.

**Definition of Idempotency:** An HTTP method is idempotent if the **intended effect on the server** of making a single request is the same as the effect of making that request multiple times. Crucially, idempotency is about the *server-side state*, not necessarily about the response status code — the response may differ between calls, but the final state of the resource on the server must be the same.

**What happens when a client sends multiple DELETE requests for the same room:**

1. **First `DELETE /api/v1/rooms/MED-101`:**
   - The server looks up room `MED-101` in the `DataStore`.
   - The room exists and has no sensors assigned (empty `sensorIds` list).
   - The room is removed from the `ConcurrentHashMap`.
   - Response: **HTTP 204 No Content** — the room has been successfully deleted.
   - **Server state after this call:** Room `MED-101` no longer exists in the data store.

2. **Second `DELETE /api/v1/rooms/MED-101`** (sent by mistake, or due to a network retry):
   - The server looks up room `MED-101` in the `DataStore`.
   - The room does **not** exist (it was already deleted in step 1).
   - Response: **HTTP 404 Not Found** — the room cannot be found.
   - **Server state after this call:** Room `MED-101` still does not exist. The server state is **unchanged** from step 1.

3. **Third, fourth, fifth `DELETE /api/v1/rooms/MED-101`:**
   - Same as step 2. The response will always be 404, and the server state will never change.

**Why this is idempotent:** After the first DELETE, the room is gone. Every subsequent DELETE attempt leaves the server in exactly the same state — the room remains absent. The fact that the response code changes from 204 to 404 does not violate idempotency, because idempotency is defined by the effect on server-side state, not by the response code. The HTTP specification explicitly permits this behaviour.

**Why this matters in practice:** In distributed systems, network failures are common. A client might send a DELETE request, experience a timeout (no response received), and then retry the same request, not knowing whether the first attempt succeeded or failed. If DELETE were not idempotent, the retry could cause unintended side effects (e.g., deleting a *different* room that was subsequently assigned the same ID). With idempotent DELETE, the client can safely retry without risk — the worst case is receiving a 404, which simply confirms the room is already gone.

**Business rule interaction:** Note that if the room still has sensors assigned, a `RoomNotEmptyException` is thrown (mapped to HTTP 409 Conflict), and the room is *not* deleted. This does not break idempotency — the precondition (sensors present) prevents any state change, and repeated requests with the same precondition will consistently produce the same 409 response with no state change.

---

## Part 3: Sensor Operations & Linking

### Task 3.1 — @Consumes and Content-Type Mismatch Handling

**Question:** *We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?*

**Answer:**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on our `POST` methods (e.g., `createSensor()`) declares a **content-type contract**: this endpoint will only accept and process request bodies that are formatted as JSON (i.e., requests with the `Content-Type: application/json` header). This annotation plays a critical role in JAX-RS's content negotiation mechanism.

**What happens when a client sends a non-JSON content type:**

When a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml` to an endpoint annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS automatically returns an **HTTP 415 Unsupported Media Type** error response. This happens *before* the resource method is even invoked — the JAX-RS runtime intercepts the request during the routing phase, determines that no resource method is capable of consuming the submitted media type, and immediately rejects the request.

**The technical mechanism behind this:**

1. **Request Routing:** When an HTTP request arrives, the JAX-RS runtime uses a multi-step algorithm to select the appropriate resource method. It first matches the URL path (`@Path`), then the HTTP method (`@GET`, `@POST`, etc.), and then the content type (`@Consumes`) and acceptable response types (`@Produces`).

2. **Media Type Matching:** If the `Content-Type` header in the request does not match any of the media types declared in `@Consumes` on the candidate methods, the runtime considers this a media type mismatch. Since no method can handle the request body, the runtime returns HTTP 415.

3. **Automatic Deserialization (MessageBodyReader):** When the content type *does* match (`application/json`), JAX-RS delegates the actual parsing of the request body to a `MessageBodyReader` — in our case, the Jackson JSON provider (`jersey-media-json-jackson`). Jackson reads the raw JSON bytes, deserialises them into the corresponding Java POJO (e.g., `Sensor`), and passes the populated object to the resource method as a parameter. If the client sends `text/plain`, there is no `MessageBodyReader` registered for that media type that can produce a `Sensor` object, so the request fails.

**Why this is a good practice:**

- **Fail-fast behaviour:** Instead of allowing malformed data into the resource method and having it fail unpredictably, the request is rejected cleanly at the framework level with a well-defined HTTP status code.
- **Security:** It prevents clients from submitting unexpected payloads (e.g., XML with embedded XXE attacks) to endpoints that are only designed to process JSON.
- **Clear API contract:** The `@Consumes` annotation serves as self-documenting code, making it immediately clear to any developer reading the source or generated OpenAPI specification what format is expected.

For example, if a client sends: `curl -X POST /api/v1/sensors -H "Content-Type: text/plain" -d "some text"`, they will receive an HTTP 415 response with no resource method code being executed.

---

### Task 3.2 — Query Parameters vs. Path Parameters for Filtering

**Question:** *You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?*

**Answer:**

In our implementation, sensor filtering by type is done using a query parameter: `GET /api/v1/sensors?type=Temperature`. An alternative would be to embed the filter directly in the URL path: `GET /api/v1/sensors/type/Temperature`. While both approaches are technically functional, the query parameter approach is generally considered superior for filtering and searching collections for several important reasons:

**1. RESTful Semantics — Resources vs. Filters:**

In REST, each unique URL path segment should represent a **distinct resource** or resource collection. The path `/api/v1/sensors` represents the sensors collection, and `/api/v1/sensors/TEMP-001` represents a specific sensor resource identified by its ID. If we used `/api/v1/sensors/type/CO2`, we would be implying that `type` is a sub-resource of `sensors` and that `CO2` is a sub-resource of `type`, which is **semantically incorrect** — `type` is not a resource; it is a filter criterion applied to the existing sensors collection. Query parameters, on the other hand, are specifically designed by the URI specification (RFC 3986) to carry **non-hierarchical, optional parameters** — exactly what filtering is.

**2. Optionality and Composability:**

Query parameters are inherently optional. With `GET /api/v1/sensors`, the client gets all sensors. With `GET /api/v1/sensors?type=CO2`, they get a filtered subset. This optionality is natural and requires no additional routing configuration. If we later add more filters (e.g., `status`, `roomId`, `minValue`), the client can freely combine them: `GET /api/v1/sensors?type=CO2&status=ACTIVE&roomId=LIB-301`. With path parameters, each combination would require a separate path definition (e.g., `/sensors/type/CO2/status/ACTIVE`), leading to a **combinatorial explosion** of routes that is completely unmanageable.

**3. Caching and Proxy Behaviour:**

HTTP caches (CDNs, reverse proxies, browser caches) treat the URL path and query string differently. The full URL including query parameters is used as the cache key, so `GET /api/v1/sensors?type=CO2` and `GET /api/v1/sensors?type=Temperature` are correctly cached as separate responses. This works identically for path-based approaches, but query parameters have the additional benefit of being more easily stripped or normalised by cache infrastructure.

**4. Client Developer Experience:**

For API consumers, query parameters are immediately recognisable as filters because they follow a universal web convention (`?key=value`). A client developer seeing `?type=CO2` immediately understands this is a filter, not a resource hierarchy. The path-based approach (`/sensors/type/CO2`) could be confused with a sub-resource lookup, leading to misinterpretation of the API's resource model.

**5. URL Cleanliness:**

The base URL (`/api/v1/sensors`) remains clean and consistently represents the sensors collection, regardless of whether filters are applied. With path parameters, the URL changes structurally (`/sensors/type/CO2`), which can break client expectations about URL patterns and make it harder to generate consistent HATEOAS links.

In summary, query parameters correctly express the semantics of filtering (optional, non-hierarchical, composable), while path segments are reserved for identifying specific resources in the hierarchy. This distinction is a core principle of RESTful API design.

---

## Part 4: Deep Nesting with Sub-Resources

### Task 4.1 — Architectural Benefits of the Sub-Resource Locator Pattern

**Question:** *Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?*

**Answer:**

The Sub-Resource Locator pattern is a JAX-RS design pattern where a resource method has a `@Path` annotation but **no HTTP method annotation** (`@GET`, `@POST`, etc.). Instead of handling the request directly, it returns an instance of another resource class, and JAX-RS delegates the remaining path resolution and request handling to that class. In our implementation, `SensorResource` contains a sub-resource locator method:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getSensorReadings(@PathParam("sensorId") String sensorId) {
    Sensor sensor = dataStore.getSensor(sensorId);
    if (sensor == null) {
        throw new NotFoundException("Sensor '" + sensorId + "' does not exist.");
    }
    return new SensorReadingResource(sensorId);
}
```

This delegates all `/sensors/{sensorId}/readings` operations to a dedicated `SensorReadingResource` class. The architectural benefits of this pattern are substantial:

**1. Separation of Concerns (Single Responsibility Principle):**

Without the sub-resource locator pattern, all operations for sensors *and* their readings would be crammed into one `SensorResource` class:

```java
// Without sub-resource locator — monolithic approach
@Path("/sensors")
public class SensorResource {
    @GET                              public Response getAllSensors() { ... }
    @POST                             public Response createSensor() { ... }
    @GET @Path("/{id}")               public Response getSensor() { ... }
    @GET @Path("/{id}/readings")      public Response getReadings() { ... }
    @POST @Path("/{id}/readings")     public Response addReading() { ... }
    @GET @Path("/{id}/readings/{rid}") public Response getReading() { ... }
    @DELETE @Path("/{id}/readings/{rid}") public Response deleteReading() { ... }
    // ... potentially dozens more methods
}
```

As the API grows (readings analytics, reading aggregations, reading exports), this class becomes a "God class" — hundreds of lines of code with mixed responsibilities. The sub-resource locator pattern solves this by having `SensorResource` focus **only** on sensor CRUD operations, while `SensorReadingResource` handles **only** reading operations. Each class has a single, well-defined responsibility.

**2. Scoped Context via Constructor Injection:**

The sub-resource locator passes the `sensorId` directly to the sub-resource's constructor: `new SensorReadingResource(sensorId)`. This means every method inside `SensorReadingResource` automatically has access to the parent sensor's ID without needing to repeat `@PathParam("sensorId")` on every method. The class is inherently *scoped* to a specific sensor's readings, which simplifies the code and makes it less error-prone.

**3. Validation at the Boundary:**

The sub-resource locator method performs validation (checking that the sensor exists) *before* delegating. This means that every operation on sensor readings — regardless of HTTP method — is automatically protected by this validation. Without this pattern, each individual method in the monolithic class would need to independently verify the sensor's existence, leading to duplicated validation logic and the risk of forgetting it in one method.

**4. Reusability:**

A sub-resource class like `SensorReadingResource` could potentially be reused in different contexts. For example, if the API later introduces a `rooms/{roomId}/readings` endpoint for room-level aggregated readings, a similar sub-resource class could be used without duplicating logic.

**5. Independent Testing:**

Each sub-resource class can be unit-tested in isolation with a mock `sensorId`, without needing to set up the entire `SensorResource` and its routing. This makes tests simpler, faster, and more focused.

**6. API Evolution Without Refactoring:**

If readings grow to require their own complex operations (analytics, exports, aggregations), the `SensorReadingResource` class can be expanded independently without touching `SensorResource` at all. In a monolithic class, every change to the readings logic risks accidentally breaking sensor logic.

In summary, the Sub-Resource Locator pattern applies the time-tested software engineering principles of **modularity, separation of concerns, and encapsulation** to REST API design, making the codebase more maintainable, testable, and scalable as the API grows in complexity.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging

### Task 5.2 — Why HTTP 422 Is More Semantically Accurate Than 404 for Invalid References

**Question:** *Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?*

**Answer:**

HTTP 404 (Not Found) and HTTP 422 (Unprocessable Entity) serve fundamentally different semantic purposes, and conflating them can mislead API consumers during debugging and integration.

**HTTP 404 (Not Found)** means that the **URL the client requested does not exist**. For example, if a client sends `GET /api/v1/rooms/NONEXISTENT`, the 404 correctly indicates that the resource at that URL path was not found. The problem is with the URL itself.

**HTTP 422 (Unprocessable Entity)** means that the server **understood the request** (the URL is valid, the HTTP method is correct, the JSON syntax is well-formed), but it **cannot process the semantic content** of the request body. The problem is not with the URL or the JSON structure — it is with the *meaning* of the data inside the body.

Consider our scenario: a client sends `POST /api/v1/sensors` with a valid JSON body containing `"roomId": "NONEXISTENT"`. In this case:

- The URL `/api/v1/sensors` **is perfectly valid** — it is the correct endpoint for creating sensors.
- The JSON body **is syntactically valid** — it has all the required fields with correct types.
- The issue is that the *value* of `roomId` references a room that does not exist in the system.

If we returned 404 here, the client might reasonably think: "The URL `/api/v1/sensors` was not found. Is the server down? Is the API path wrong? Did I misspell the endpoint?" This is misleading because the URL is fine — the problem is entirely within the request payload.

HTTP 422 accurately communicates: "I received your request, I understand it, but I cannot process it because the data you sent is semantically invalid — specifically, the room you referenced does not exist." This gives the client a clear diagnosis: the fix is not to change the URL, but to fix the `roomId` value in the request body to reference an existing room.

In our implementation, `LinkedResourceNotFoundException` is mapped to HTTP 422 to precisely convey this distinction. The structured JSON error body further clarifies the problem:

```json
{
    "error": "UNPROCESSABLE_ENTITY",
    "message": "Cannot register sensor: the referenced Room 'NONEXISTENT' does not exist. Please create the room first or use a valid roomId.",
    "status": 422
}
```

This semantic precision is especially valuable in enterprise integrations where automated systems parse error codes to determine retry strategies. A 404 might trigger a "resource moved" fallback, while a 422 correctly triggers a "fix the input data" workflow.

---

### Task 5.4 — Cybersecurity Risks of Exposing Internal Stack Traces

**Question:** *From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?*

**Answer:**

Exposing raw Java stack traces to external API consumers is a significant cybersecurity vulnerability classified under **CWE-209 (Generation of Error Message Containing Sensitive Information)** and is explicitly listed in the **OWASP Top 10** under "Security Misconfiguration." A stack trace is essentially a detailed roadmap of the application's internal architecture, and an attacker can extract the following categories of sensitive information:

**1. Internal Package Structure and Class Names:**

A stack trace reveals the full qualified class names (e.g., `com.smartcampus.repository.DataStore`, `com.smartcampus.resource.SensorResource.createSensor()`). An attacker now knows the application's package hierarchy, class naming conventions, and which classes handle which operations. This enables them to craft highly targeted attacks against specific components.

**2. Third-Party Libraries and Their Versions:**

Stack traces typically include frames from third-party libraries (e.g., `org.glassfish.jersey.server.ContainerResponse:2.41`, `com.fasterxml.jackson.databind:2.15.2`). Knowing the exact library versions allows an attacker to search public vulnerability databases (CVE/NVD) for known exploits. For example, certain versions of Jackson have had critical **deserialization vulnerabilities** (CVE-2019-12384, CVE-2020-36518) that allow Remote Code Execution (RCE) if the attacker knows the exact version in use.

**3. File Paths and Server Configuration:**

Stack traces may include absolute file paths (e.g., `/opt/smartcampus/lib/jersey-server-2.41.jar` or `/home/deploy/CW1-Client-Server/src/main/java/...`). This reveals the operating system, deployment directory structure, and user account under which the application runs — all valuable for planning privilege escalation attacks.

**4. Database Connection Details:**

If a database-related exception propagates to the client (e.g., `java.sql.SQLException`), the stack trace and error message may include the database hostname, port, schema name, or even connection string parameters. While our application uses in-memory storage, in production APIs this is a common and catastrophic leak.

**5. Business Logic Flow and Injection Points:**

By intentionally triggering different errors and reading the resulting stack traces, an attacker can map out the application's internal control flow — understanding which methods call which, where input validation occurs, and where it doesn't. This is invaluable for finding injection points (SQL injection, NoSQL injection, JNDI injection). For example, seeing `DataStore.getSensor()` in the trace tells the attacker exactly where data retrieval happens and how to craft malformed input to exploit it.

**6. Server and JVM Information:**

The stack trace header often includes the JVM version and server implementation details (e.g., `Grizzly/2.4.4`), providing even more version-specific attack vectors.

**Our mitigation:** The `GenericExceptionMapper` class catches all unhandled `Throwable` exceptions, logs the full stack trace **server-side only** (for debugging), and returns a clean, generic JSON response to the client:

```json
{
    "error": "INTERNAL_SERVER_ERROR",
    "message": "An unexpected error occurred on the server. Please try again later.",
    "status": 500
}
```

This ensures the API is "leak-proof" — no internal implementation details are ever exposed to external consumers, regardless of what error occurs. The full stack trace remains available in the server logs for developers to diagnose issues, but it is never transmitted over the network.

---

### Task 5.5 — JAX-RS Filters vs. Manual Logging for Cross-Cutting Concerns

**Question:** *Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?*

**Answer:**

Using JAX-RS filters (`ContainerRequestFilter` and `ContainerResponseFilter`) for cross-cutting concerns like logging is a fundamental application of the **Separation of Concerns** principle and the **Aspect-Oriented Programming (AOP)** paradigm. It is superior to manual logging in nearly every dimension:

**1. Uniform Coverage Without Omission Risk:**

A filter registered with `@Provider` automatically intercepts **every** request and response that passes through the JAX-RS runtime, including all current and future endpoints. If a developer adds a new resource class with five new endpoints next week, those endpoints automatically get request/response logging without any additional code. With manual `Logger.info()` statements, the developer must remember to add logging to every new method — and in a team environment with multiple developers, it is virtually inevitable that some methods will be missed, creating blind spots in observability.

**2. Separation of Concerns (SRP):**

Resource methods should focus exclusively on **business logic** — validating input, querying data, applying rules, and producing responses. Logging is an **infrastructure concern** that has nothing to do with the business domain. Mixing the two violates the Single Responsibility Principle:

```java
// Bad: business logic polluted with infrastructure concerns
@POST
public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
    LOGGER.info(">> REQUEST: POST /api/v1/sensors");           // Infrastructure
    Room room = dataStore.getRoom(sensor.getRoomId());          // Business logic
    if (room == null) {
        LOGGER.warn("Room not found: " + sensor.getRoomId());   // Infrastructure
        throw new LinkedResourceNotFoundException("...");        // Business logic
    }
    dataStore.addSensor(sensor);                                 // Business logic
    LOGGER.info("<< RESPONSE: 201 Created");                    // Infrastructure
    return Response.created(locationUri).entity(sensor).build(); // Business logic
}
```

With a filter, the resource method contains **only** business logic, and all logging is handled externally:

```java
// Good: clean business logic, logging handled by filter
@POST
public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
    Room room = dataStore.getRoom(sensor.getRoomId());
    if (room == null) {
        throw new LinkedResourceNotFoundException("...");
    }
    dataStore.addSensor(sensor);
    return Response.created(locationUri).entity(sensor).build();
}
```

**3. DRY (Don't Repeat Yourself):**

With manual logging, the same `LOGGER.info(">> REQUEST: " + method + " " + uri)` pattern is copy-pasted into dozens of resource methods. If the logging format needs to change (e.g., adding a timestamp, a correlation ID, or switching from INFO to DEBUG level), every single copy must be updated individually. With a filter, the logging format is defined in **one place** — a single change propagates to all endpoints instantly.

**4. Consistent Logging Format:**

A filter guarantees that every request and response is logged in **exactly the same format** (e.g., `>> REQUEST: GET http://localhost:8080/api/v1/sensors`). With manual logging, different developers might use slightly different formats, making log parsing and monitoring tools (e.g., ELK stack, Splunk) unreliable.

**5. Access to Both Request and Response Context:**

A `ContainerResponseFilter` has access to both the request context and the response context simultaneously, allowing it to log the complete lifecycle of a request (method, URI, response status) in a single log line. Achieving this with manual logging inside a resource method is difficult because the response status may not be known when logging occurs (e.g., if an exception mapper produces the final response).

**6. Easy to Enable/Disable/Replace:**

Because the filter is a standalone class annotated with `@Provider`, it can be removed or replaced (e.g., with a more sophisticated filter that adds correlation IDs or integrates with a distributed tracing system like Jaeger) without modifying a single line of business logic code.

In summary, JAX-RS filters provide a **centralised, maintainable, and automatically comprehensive** approach to cross-cutting concerns. They embody the principle that infrastructure concerns should be handled by infrastructure components, freeing resource classes to focus purely on business logic.
