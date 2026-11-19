# Enrollment Service

Owns the **student-course link**. It is the only service that talks to both the student-service and
the course-service. Those two never call each other.

| | |
|---|---|
| Port | `8083` |
| Eureka name | `enrollment-service` |
| Stack | Spring Boot 3.2.3, Spring Cloud 2023.0.1 (OpenFeign, Resilience4j, Eureka), JPA, H2 |

## What it stores

One table, `enrollments`, with `UNIQUE (student_id, course_id)`. `student_id` and `course_id` are plain ids
that refer to rows in other services' databases (logical references, no foreign keys).
It stores no student names and no course details. Those are fetched when needed.

## Enroll flow

1. `GET student-service /api/students/{id}`: the student must exist and be `ACTIVE`.
2. Look in the local table: an `ENROLLED` or `COMPLETED` row means duplicate (409). A `DROPPED` row is re-enrolled.
3. `POST course-service /api/courses/{id}/reserve-seat`: atomic, 409 if full or not open.
4. Save the row (or flip the dropped one back with a conditional `UPDATE`).
5. Publish `STUDENT_ENROLLED`.

If step 4 fails for any reason, including losing a race on the unique constraint, the seat from step 3
is released. `enroll()` runs without a database transaction on purpose, because it makes remote calls.

## Run it

Needs the student-service and course-service running.

```bash
# With Eureka (all services registered):
mvn spring-boot:run

# Without Eureka. Fixed addresses for the other two services:
EUREKA_ENABLED=false \
STUDENT_SERVICE_URL=http://localhost:8080 \
COURSE_SERVICE_URL=http://localhost:8082 \
mvn spring-boot:run

mvn test
```

On Windows PowerShell, set the variables first: `$env:EUREKA_ENABLED="false"`, and so on. In IntelliJ, put them in the
run configuration's Environment variables field.

- Swagger UI: http://localhost:8083/swagger-ui.html
- Usage guide with curl examples: [`API-USAGE.md`](API-USAGE.md)
- Runnable requests for IntelliJ: [`api-requests.http`](api-requests.http)
- H2 console: http://localhost:8083/h2-console (JDBC URL `jdbc:h2:mem:enrollmentdb`, user `sa`)

## What the other two services need

**Course service:** nothing. It already has `reserve-seat`, `release-seat` and `batch`.

**Student service (your original project):** it must answer `GET /api/students/{id}` with the
`{ success, message, data }` envelope and a `status` field. It already does. To join Eureka later:
1. Set `spring.application.name=student-service`.
2. Move it off port 8080 to `8081`, so the gateway can take 8080.
3. Add the Eureka client dependency and the Spring Cloud BOM (same as in this project's pom).

## Failure behaviour

| What happens downstream | What the caller gets |
|---|---|
| Student or course returns 404 | 404 with "Student/Course not found with id: N" |
| Course returns 409 (full, not open) | 409 with the course's own message |
| Student is not ACTIVE | 409 |
| Service down, timing out, or circuit open | 503 "... is currently unavailable" |
| Unique constraint fires (lost a race) | 409 duplicate, and the reserved seat is released |

The circuit breaker ignores 404 and 409, because those are normal business answers. Only real failures
count toward opening it. Feign does not retry, so `reserve-seat` is never sent twice.

## Known limits (deliberate, documented)

- **Seat counter drift.** The course counter and this table are updated by two different services with no
  shared transaction. If the app crashes between reserving a seat and saving the row, or a `release-seat`
  call fails, the counter is off by one. Failed releases are logged at ERROR with the course id. Compare
  `SELECT course_id, COUNT(*) FROM enrollments WHERE status='ENROLLED' GROUP BY course_id` against the
  course service's `enrolledCount` to find drift. A periodic reconciliation job is the next step.
- **A timeout on `reserve-seat` is ambiguous.** The course service may have reserved the seat even though the
  response never arrived. The code does not release in that case, because releasing a seat that was never
  taken would be worse. Reconciliation fixes this too.
- **Events are logged, not delivered.** `LoggingEnrollmentEventPublisher` only logs. For real delivery, write a
  Kafka or RabbitMQ implementation of `EnrollmentEventPublisher` and set `app.events.publisher` to its name.
  If events must never be lost, use the outbox pattern (write the event in the same transaction as the row).
- **Roster has ids only.** `GET /api/enrollments?courseId=` returns student ids. Showing names needs a batch
  endpoint on the student-service (`GET /api/students/batch?ids=`).
- **H2 is in-memory.** Data is lost on restart. See the course-service README for switching to MySQL or PostgreSQL,
  and use Flyway instead of `ddl-auto` there.
