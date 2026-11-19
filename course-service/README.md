# Course Service

Microservice that owns the **course catalog** and **seat inventory** for the Student Management System.

| | |
|---|---|
| Port | `8082` |
| Eureka name | `course-service` |
| Stack | Spring Boot 3.2.3, Spring Cloud 2023.0.1, JPA, H2 (swap for MySQL/PostgreSQL) |

## Run

```bash
mvn spring-boot:run                     # registers with Eureka at localhost:8761 if it is running
EUREKA_ENABLED=false mvn spring-boot:run   # standalone, no Eureka needed
mvn test
```

H2 console: http://localhost:8082/h2-console (JDBC URL `jdbc:h2:mem:coursedb`, user `sa`, empty password).
Health: http://localhost:8082/actuator/health

Sample data is seeded on startup, including a full course (`CS499`), a DRAFT course (`PH310`)
and a CLOSED course (`MB330`) so you can test the error paths.

## API

All responses use the envelope `{ success, message, data, timestamp }`.

| Method | Path | Purpose | Errors |
|---|---|---|---|
| POST | `/api/courses` | Create a course | 400 validation, 409 duplicate code |
| GET | `/api/courses` | List with optional filters (below) | 400 bad parameter |
| GET | `/api/courses/{id}` | Get by id | 404 |
| GET | `/api/courses/code/{code}` | Get by course code | 404 |
| GET | `/api/courses/batch?ids=1,2,3` | Bulk lookup for other services | |
| GET | `/api/courses/summary` | Totals, occupancy, courses per department | |
| PUT | `/api/courses/{id}` | Update | 404, 409 duplicate code / capacity below enrollment |
| PATCH | `/api/courses/{id}/status?status=CLOSED` | Change status | 404 |
| DELETE | `/api/courses/{id}` | Delete | 404, 409 if students are enrolled |
| POST | `/api/courses/{id}/reserve-seat` | Atomically take one seat | 404, 409 full or not open |
| POST | `/api/courses/{id}/release-seat` | Atomically give one seat back | 404, 409 nothing reserved |

List filters (all optional): `keyword`, `department`, `semester`, `status`, `availableOnly=true`,
`page`, `size` (max 100), `sortBy`, `direction`.

```bash
curl "http://localhost:8082/api/courses?department=Engineering&availableOnly=true&sortBy=code"

curl -X POST http://localhost:8082/api/courses -H "Content-Type: application/json" -d '{
  "code": "CS450", "title": "Cloud Computing", "credits": 3,
  "department": "Engineering", "semester": "2026-FALL", "capacity": 40,
  "instructorId": 2, "startDate": "2026-08-17", "endDate": "2026-12-11"
}'

curl -X POST http://localhost:8082/api/courses/1/reserve-seat
curl -X POST http://localhost:8082/api/courses/7/reserve-seat   # CS499 is full -> 409
```

## Design notes

- **Atomic seats.** `reserveSeat` is one SQL `UPDATE ... WHERE enrolled_count < capacity AND status = 'OPEN'`.
  Two concurrent requests can never both take the last seat, with no locks and no retry loop.
  The service reads the row afterwards only to explain a failure (404, not open, or full).
- **`@DynamicUpdate` on the entity** so that editing a course never writes back a stale `enrolledCount`.
  `enrolledCount` is changed only by reserve/release.
- **No cross-service foreign keys.** `instructorId` is a plain id owned by the faculty-service.
- **No `@CrossOrigin`.** CORS lives at the API gateway.
- **Release is not idempotent.** Calling `release-seat` twice frees two seats. Enrollment should only
  release a seat it actually reserved. An enrollment-id based reservation table would make this idempotent.

## Wiring it in

Gateway route (`api-gateway` application.yml):

```yaml
- id: course-service
  uri: lb://COURSE-SERVICE
  predicates:
    - Path=/api/courses/**
```

Enrollment Feign client: the `CourseClient` from the previous step works unchanged. Its `CourseDto`
fields (`id, code, title, capacity, enrolledCount`) all exist in this service's response, and a
full course returns **409**, which Enrollment already maps to "Course is full".

Port plan: gateway `8080`, student-service `8081` (change it from 8080 when you register it), course-service `8082`.
