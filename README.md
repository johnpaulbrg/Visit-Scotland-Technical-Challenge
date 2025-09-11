# Visit Scotland Wish List Technical Challenge

**Author:** John Paul Brogan
<br>**Date:** September 12, 2025 

---

Welcome to the technical challenge documentation for the Visit Scotland Wish List Service.

---

## Project Overview

The **Visit Scotland Wish List Service** is a stateless, in‑memory REST API that enables users to create, manage, and retrieve personalised wish lists of tourism experiences from across Scotland.
Built for spec compliance and domain‑safe identity mapping, it places a strong emphasis on **thread safety** and **test coverage**.

### Purpose

This service provides a backend for storing and manipulating user‑specific wish lists containing items such as accommodation, attractions, and events. It is intended for demonstration, prototyping, and integration testing, and can be extended to use persistent storage as required.

### Scope

- **In‑memory only** — no database or external persistence layer  
- **RESTful interface** — JSON over HTTP  
- **Domain‑safe identity** — deterministic UUID mapping from user IDs  
- **Thread‑safe operations** — per‑user isolation and synchronised mutations  
- **Spec‑driven design** — all endpoints, DTOs, and behaviours align with the documented requirements  

### Key Features

- Create and delete wish lists per user  
- Add and remove items by ID or full payload  
- Filter items by category (`ACCOMMODATION`, `ATTRACTION`, `EVENT`)  
- Deduplicate items using full‑field equality  
- Structured logging for auditability  
- Declarative validation for all inbound data  
- Comprehensive unit and integration tests  

### Intended Audience

This documentation is aimed at developers, reviewers, and integrators who need to understand the system’s purpose, capabilities, and constraints before working with the codebase or API.

---

## Architecture Summary

The Visit Scotland Wish List Service follows a clean, layered architecture that separates concerns, enforces domain safety, and ensures thread‑safe operations. This structure makes the codebase easier to maintain, extend, and test.

### Layered Structure

<img width="341" height="512" alt="WishListLayeredArchitetureDiagram" src="https://github.com/user-attachments/assets/94ec1cf4-9a73-4a3a-a941-5dada076315d" />

Handles HTTP requests and responses. Validates incoming data, maps between DTOs and domain objects, and applies HTTP‑semantic error handling. All requests are logged with method name, URI, remote IP, and resolved user ID.

#### Service Layer
Stateless orchestration of business logic. Manages the lifecycle of wish lists, enforces deduplication rules, and coordinates thread‑safe mutations. Uses `ConcurrentHashMap` for per‑user isolation.

#### Domain Model
Core business entities:
- `User` — Immutable, domain‑safe identity derived from `userId` string.
- `Item` — Immutable, full‑field equality to prevent duplicates.
- `Category` — Enum: `ACCOMMODATION`, `ATTRACTION`, `EVENT`.
- `WishList` — Holds a `User` and a set of `Item` objects; synchronised mutation methods.

#### In‑Memory Store
`ConcurrentHashMap<UUID, WishList>` keyed by the user’s domain‑safe UUID. No persistence layer; all data is lost on application restart.

#### DTO Layer
Transport‑only objects for JSON input/output:
- `ItemRequest` — Inbound payload with validation annotations.
- `ItemResponse` — Outbound representation of an `Item`.
- `WishListResponse` — Outbound wrapper containing `userId` and items.
- `ResponseStatusException` — Spring managed structured error response payload.

### Concurrency Model

- **Per‑User Isolation** — Each wish list is stored separately in the map.  
- **Synchronized Mutations** — All changes to a `WishList` are synchronised at the instance level.  
- **No Global Locks** — Avoids blocking across users, ensuring scalability for concurrent requests.  

### Design Principles

- **Domain‑Safe Identity** — Deterministic UUID mapping from `userId` ensures consistent lookups.  
- **Full‑Field Equality** — Prevents subtle duplicates by comparing all item fields.  
- **DTO/Domain Separation** — Prevents transport concerns from leaking into business logic.  
- **Spec Compliance** — All endpoints, DTOs, and behaviours match the documented requirements.  
- **Auditability** — Structured logging at every layer for traceability.

---

## Technology Stack

The Visit Scotland Wish List Service is built on a stable, LTS‑friendly technology stack chosen for **runtime portability**, **spec compliance**, and **long‑term maintainability**. All components are aligned with the requirement to run cleanly on Java 11 and Java 21 without introducing breaking changes or unnecessary dependencies.

| Component            | Version        | Purpose / Rationale                                                                 |
|----------------------|---------------|-------------------------------------------------------------------------------------|
| **Java**             | 21 (compiled to 11 bytecode) | Development uses JDK 21 for tooling; `--release 11` ensures compatibility with Java 11+ runtimes. |
| **Spring Boot**      | 2.7.18         | Final 2.x release; stable across Java 11–21; avoids Jakarta EE migration in 3.x.    |
| **Spring Framework** | 5.3.32         | Final 5.x release; compatible with Java 11–21; no namespace changes.                |
| **Maven**            | 3.9.x+         | Build automation, dependency management, and reproducible builds.                   |
| **JUnit 5**          | 5.x            | Modern testing framework; supports parameterised tests and improved assertions.     |
| **Mockito**          | 5.12.0         | Mocking framework for isolated unit testing of service logic.                        |
| **Spring Boot Starter Web** | 2.7.18 | Provides embedded Tomcat, REST controller support, and JSON serialization.          |
| **Spring Boot Starter Validation** | 2.7.18 | Enables declarative validation via `javax.validation` annotations.                  |

### Design Constraints

- **No persistence layer** — all data is stored in‑memory using `ConcurrentHashMap`.
- **No Lombok** — explicit constructors and getters for transparency and traceability.
- **No Docker or containerisation** — runs as a standalone Spring Boot application.
- **No Jakarta EE migration** — avoids breaking changes introduced in Spring 6.x / Boot 3.x.
- **No reflection‑based mapping** — DTO ↔ domain conversion is explicit and predictable.

### Runtime Compatibility

| Java Version | Status           | Notes                                                                 |
|--------------|------------------|-----------------------------------------------------------------------|
| Java 11      | ✅ Fully supported | Target bytecode; runs natively without warnings.                      |
| Java 21      | ✅ Fully supported | Development JDK; no Java 21‑specific features used.|

### Why This Stack?

- **Portability** — Runs unchanged across multiple LTS JVMs.
- **Maintainability** — Avoids premature adoption of breaking platform changes.
- **Testability** — Full unit and integration test coverage with modern tooling.
- **Spec Alignment** — Meets all technical requirements in the Visit Scotland Wish List Service specification.

---
## 4. Endpoints

| Method | Endpoint                                      | Description               |
|--------|-----------------------------------------------|---------------------------|
| POST   | /wishlist/{userId}                            | Create wish list (idempotent) |
| DELETE | /wishlist/{userId}                            | Delete wish list          |
| GET    | /wishlist/{userId}                            | Retrieve all items        |
| GET    | /wishlist/{userId}?category=EVENT             | Filter items by category  |
| POST   | /wishlist/{userId}/item                       | Add item                  |
| DELETE | /wishlist/{userId}/item/{itemId}              | Remove item by ID         |
| DELETE | /wishlist/{userId}/item                       | Remove item by payload    |

### Request: `ItemRequest` (JSON)

```json
{
  "id": "optional-uuid",
  "title": "Edinburgh Castle Tour",
  "category": "ATTRACTION",
  "description": "Guided tour with skip-the-line access",
  "image": "https://example.com/castle.jpg",
  "date": "2025-09-10",
  "metadata": {
    "language": "English"
  }
}
```
- Required for add: title, category
- Required for remove by payload: id, title, category
- id is generated if absent during add

### Response: 'ItemResponse' (JSON)
- Same fields as ItemRequest
- id always present
- Optional fields omitted if null

### Response: `WishListResponse`

```json
{
  "userId": "test.user",
  "items": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "title": "Edinburgh Castle Tour",
      "category": "ATTRACTION",
      "description": "Guided tour with skip-the-line access",
      "image": "https://example.com/castle.jpg",
      "date": "2025-09-10",
      "metadata": {
        "language": "English"
      }
    },
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-1234567890ef",
      "title": "Fringe Festival Pass",
      "category": "EVENT"
    }
  ]
}
```
- userId: Original string identifier (e.g. test.user)
- items: Array of ItemResponse objects
- Each item includes all relevant fields
- Optional fields like description, image, date, and metadata are omitted if null


### Error Handling
Uses ResponseStatusException for precise HTTP status codes. All errors return structured JSON:
```json
{
  "timestamp": "2025-09-10T19:23:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/wishlist/john/item"
}

```

### Postman Setup Tips
- Set Content-Type: application/json in headers
- Use raw JSON body for POST / DELETE with payload
- Save common requests as Postman collections for reuse

---
## Getting Started

This section provides a quick, reliable path to running the VisitScotland Wish List Service locally.  
Follow these steps to clone the repository, build the project, start the server, and verify the API.


### Prerequisites

Before you begin, ensure you have:

- **Java Development Kit (JDK)** — Version 11 or later (JDK 21 recommended for development; compiled to Java 11 bytecode for compatibility).
- **Apache Maven** — Version 3.9.x or later.
- **Git** — For cloning the repository.
- **cURL** or an API client (e.g. Postman, HTTPie) — For testing endpoints.


### Clone the Repository or Download the code base

```bash
git clone https://github.com/your-org/wishlist-service.git
cd wishlist-service
```

or download the zip

### Build the Project

```bash
mvn clean install
```
This will:
- Compile the code to Java 11 bytecode.
- Run unit tests (WishListServiceTest) and integration tests (WishListControllerTest).
- Package the application as a runnable JAR.

### Run the Application

Start the Spring Boot application:
```bash
mvn spring-boot:run
```
By default, the service will start on:
http://localhost:8080

### Verify the Service

```bash
curl -i -X POST http://localhost:8080/wishlist/test-user
```

### Run the Test Suite

To run all tests:
```bash
mvn test
```
- Unit tests validate service logic, concurrency safety, and deduplication.
- Integration tests verify endpoint behaviour, HTTP status codes, and JSON payloads.

### Project Structure
```bash
src/
 ├── main/
 │   ├── java/com/visitscotland/wishlist
 │   │    ├── controller/       # REST endpoints
 │   │    ├── service/          # Business logic
 │   │    ├── domain/model/     # User, Item, Category, WishList
 │   │    └── dto/              # Request/response DTOs
 │   └── resources/             # Application config
 └── test/
     ├── java/com/visitscotland/wishlist
     │    ├── service/          # Unit tests
     │    └── controller/       # Integration tests
```

### 5.8 Default Configuration

- **Port:** `8080`  
- **Context path:** `/wishlist`  
- **Persistence:** In‑memory only (data cleared on restart)  
- **Logging:** Console and file output with structured request/mutation logs 

---
