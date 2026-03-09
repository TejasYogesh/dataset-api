# Dataset API — Spring Boot

A RESTful Spring Boot application that stores schema-free JSON records and supports dynamic **Group-By** and **Sort-By** query operations on any field.

---

## 💡 Problem & Approach

The challenge here is that JSON records are **schema-free** — an `employee_dataset` record might have `name`, `age`, `department`, while a `sales_dataset` record might have `product`, `price`, `region`. There's no fixed structure upfront.

**My approach:** Store each JSON record as a `TEXT` string in a single `dataset_records` table, tagged with a `dataset_name` column. At query time, deserialize each record into a `Map<String, Object>`, then apply Group-By or Sort-By dynamically using Java Streams.

This avoids needing a separate table per dataset, and no schema migrations are needed when a new dataset type is introduced.

One non-obvious problem I had to solve: **sorting numbers stored in JSON**. Jackson deserializes `{"age": 30}` as an `Integer`, but if you sort using string comparison, `"9"` sorts after `"30"` alphabetically. I wrote a type-aware `Comparator` that checks if values are `instanceof Number` and compares them numerically, falling back to string comparison otherwise.

---

## 🏗️ Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│  DatasetController  │  ← Routing, HTTP status codes, request parsing only
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│  DatasetService     │  ← All business logic (interface + impl)
│  (interface)        │    Group-By via Collectors.groupingBy()
│  DatasetServiceImpl │    Sort-By via type-aware Comparator
└─────────┬───────────┘
          │
          ▼
┌──────────────────────────┐
│  DatasetRecordRepository │  ← Spring Data JPA (zero SQL written)
└─────────┬────────────────┘
          │
          ▼
┌─────────────────┐
│   H2 Database   │  ← In-memory, auto-configured, no setup needed
└─────────────────┘
```

**Layers are intentionally thin.** The controller doesn't know about the database. The repository doesn't know about business rules. Each layer has one job.

---

## 📦 Project Structure

```
src/
├── main/java/com/assignment/datasetapi/
│   ├── DatasetApiApplication.java          ← Entry point
│   ├── config/
│   │   └── AppConfig.java                  ← ObjectMapper bean, Swagger config
│   ├── controller/
│   │   └── DatasetController.java          ← REST endpoints (thin layer)
│   ├── dto/
│   │   ├── InsertRecordRequest.java        ← Accepts any JSON via @JsonAnySetter
│   │   ├── InsertRecordResponse.java       ← Insert confirmation response
│   │   ├── QueryParams.java                ← Encapsulates groupBy/sortBy/order
│   │   └── QueryResponse.java             ← Returns groupedRecords OR sortedRecords
│   ├── exception/
│   │   ├── DatasetNotFoundException.java   ← 404
│   │   ├── InvalidQueryException.java      ← 400 for bad query params
│   │   ├── InvalidRecordException.java     ← 400 for bad request body
│   │   └── GlobalExceptionHandler.java    ← @RestControllerAdvice catches all exceptions
│   ├── model/
│   │   └── DatasetRecord.java              ← JPA entity → dataset_records table
│   ├── repository/
│   │   └── DatasetRecordRepository.java    ← Spring Data JPA interface
│   └── service/
│       ├── DatasetService.java             ← Interface (contract)
│       └── DatasetServiceImpl.java         ← Implementation (business logic)
└── test/java/com/assignment/datasetapi/
    ├── controller/
    │   └── DatasetControllerTest.java      ← @WebMvcTest + MockMvc (HTTP layer)
    └── service/
        └── DatasetServiceImplTest.java     ← Mockito unit tests (business logic)
```

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.6+

```bash
# Verify your setup
java -version   # should show 17+
mvn -version    # should show 3.6+
```

### Clone & Run

```bash
git clone https://github.com/TejasYogesh/dataset-api.git
cd dataset-api

# Build and run tests
mvn clean install

# Start the application
mvn spring-boot:run
```

App starts on **`http://localhost:8080`**

### URLs Available After Startup

| URL | What it is |
|-----|-----------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs — try endpoints directly here |
| `http://localhost:8080/h2-console` | Browser DB viewer to inspect stored records live |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON spec |

> **H2 Console login:** JDBC URL = `jdbc:h2:mem:datasetdb`, Username = `sa`, Password = *(leave blank)*

---

## 🧪 Running Tests

```bash
mvn test
```

Tests are split into two files intentionally:

- **`DatasetServiceImplTest`** — Pure unit tests. The repository is mocked with Mockito. Tests business logic in complete isolation from the database.
- **`DatasetControllerTest`** — Uses `@WebMvcTest` + `MockMvc` to simulate real HTTP requests. Tests routing, HTTP status codes, and JSON response structure. The service is mocked so only the HTTP layer is under test.

---

## 📋 API Reference

### POST `/api/dataset/{datasetName}/record`

Inserts a JSON record into the named dataset. Accepts **any** JSON object — no fixed schema required.

**Request**
```http
POST /api/dataset/employee_dataset/record
Content-Type: application/json

{
  "id": 1,
  "name": "John Doe",
  "age": 30,
  "department": "Engineering"
}
```

**Response — `201 Created`**
```json
{
  "message": "Record added successfully",
  "dataset": "employee_dataset",
  "recordId": 1
}
```

> If your JSON has an `"id"` field, that value is returned as `recordId`. If not, the auto-generated database ID is used.

**Error cases**

| Status | Reason |
|--------|--------|
| `400` | Empty request body `{}` |
| `400` | Body is not valid JSON |
| `400` | Dataset name is blank |

---

### GET `/api/dataset/{datasetName}/query`

Queries records from a dataset. Provide **exactly one** of `groupBy` or `sortBy`.

#### Group By

```http
GET /api/dataset/employee_dataset/query?groupBy=department
```

**Response — `200 OK`**
```json
{
  "groupedRecords": {
    "Engineering": [
      { "id": 1, "name": "John Doe",    "age": 30, "department": "Engineering" },
      { "id": 2, "name": "Jane Smith",  "age": 25, "department": "Engineering" }
    ],
    "Marketing": [
      { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing" }
    ]
  }
}
```

#### Sort By

```http
GET /api/dataset/employee_dataset/query?sortBy=age&order=asc
```

**Response — `200 OK`**
```json
{
  "sortedRecords": [
    { "id": 2, "name": "Jane Smith",  "age": 25, "department": "Engineering" },
    { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing"   },
    { "id": 1, "name": "John Doe",    "age": 30, "department": "Engineering" }
  ]
}
```

**Query Parameters**

| Parameter | Required | Values | Description |
|-----------|----------|--------|-------------|
| `groupBy` | One of these two | Any field name | Groups records by that field's value |
| `sortBy`  | One of these two | Any field name | Sorts records by that field |
| `order`   | No (default: `asc`) | `asc` / `desc` | Sort direction |

**Error cases**

| Status | Reason |
|--------|--------|
| `400` | Neither `groupBy` nor `sortBy` provided |
| `400` | Both `groupBy` and `sortBy` provided together |
| `400` | `order` is not `asc` or `desc` |
| `400` | Specified field doesn't exist — error message lists available fields |
| `404` | No records found for this dataset name |

---

### Error Response Format

All errors return a consistent structure regardless of where they occur:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Dataset not found: 'unknown_dataset'. No records exist for this dataset.",
  "timestamp": "2024-01-15T10:30:00.123"
}
```

---

## 🔬 Postman Testing — Full Flow

### Step 1 — Insert records

```
POST http://localhost:8080/api/dataset/employee_dataset/record
Body: { "id": 1, "name": "John Doe",    "age": 30, "department": "Engineering" }

POST http://localhost:8080/api/dataset/employee_dataset/record
Body: { "id": 2, "name": "Jane Smith",  "age": 25, "department": "Engineering" }

POST http://localhost:8080/api/dataset/employee_dataset/record
Body: { "id": 3, "name": "Alice Brown", "age": 28, "department": "Marketing"   }
```

### Step 2 — Group by department
```
GET http://localhost:8080/api/dataset/employee_dataset/query?groupBy=department
```
Expected: two groups — `Engineering` (2 records) and `Marketing` (1 record)

### Step 3 — Sort by age ascending
```
GET http://localhost:8080/api/dataset/employee_dataset/query?sortBy=age&order=asc
```
Expected order: Jane (25) → Alice (28) → John (30)

### Step 4 — Sort by name descending
```
GET http://localhost:8080/api/dataset/employee_dataset/query?sortBy=name&order=desc
```
Expected order: John → Jane → Alice

### Step 5 — Test error handling
```
# 404 — dataset doesn't exist
GET http://localhost:8080/api/dataset/unknown/query?groupBy=department

# 400 — field doesn't exist in the dataset
GET http://localhost:8080/api/dataset/employee_dataset/query?groupBy=salary

# 400 — no query params at all
GET http://localhost:8080/api/dataset/employee_dataset/query

# 400 — both params provided at once
GET http://localhost:8080/api/dataset/employee_dataset/query?groupBy=department&sortBy=age
```

---

## 🏗️ Key Design Decisions

**Schema-free storage as JSON TEXT**
Each record is stored as a raw JSON string in a single `TEXT` column. This means any dataset shape is supported without creating new tables or writing migrations. The trade-off is that group/sort operations happen in-memory after fetching — acceptable here, and honest about the limitation.

**Service Interface + Implementation**
`DatasetService` is an interface; `DatasetServiceImpl` is the only implementation. The controller depends on the abstraction, not the concrete class (Dependency Inversion). This also makes the service trivially mockable in tests with `@MockBean`.

**Type-aware Sort Comparator**
Jackson deserializes JSON numbers as `Integer`/`Long`/`Double`. The comparator detects `instanceof Number` and compares numerically, falling back to case-insensitive string comparison. Without this, `[30, 9, 25]` would sort lexicographically to `[25, 30, 9]` — incorrect.

**`@JsonInclude(NON_NULL)` on QueryResponse**
`QueryResponse` has two fields: `groupedRecords` and `sortedRecords`. Only one is populated per request. `NON_NULL` keeps the unused field completely out of the JSON response rather than returning `"sortedRecords": null`.

**Centralized Exception Handling**
`GlobalExceptionHandler` with `@RestControllerAdvice` catches all exceptions application-wide and formats them into a consistent error body. No try-catch blocks in controllers or services — failures bubble up and are handled in one place.

**Constructor Injection over `@Autowired` fields**
All dependencies injected via constructor using Lombok's `@RequiredArgsConstructor`. Makes dependencies explicit, prevents null injection in tests, and avoids accidental circular dependencies.

---

## 🗄️ Database Schema

```sql
CREATE TABLE dataset_records (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_name VARCHAR(255) NOT NULL,
    record_data  TEXT         NOT NULL,
    created_at   TIMESTAMP
);

CREATE INDEX idx_dataset_name ON dataset_records(dataset_name);
```