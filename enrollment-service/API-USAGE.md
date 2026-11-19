# Enrollment Service: API Usage Guide

Base URL (direct): `http://localhost:8083`. Through the gateway: same paths on `http://localhost:8080`.
Interactive docs: http://localhost:8083/swagger-ui.html  |  OpenAPI JSON: http://localhost:8083/v3/api-docs

Every response uses one envelope: `{ "success": true, "message": "...", "data": {}, "timestamp": "..." }`.
On errors `success` is `false` and `data` is absent. The exception is validation errors (400), where `data`
maps each bad field to its message.

| Status | Meaning |
|---|---|
| 200 / 201 | Success / created |
| 400 | Validation failed, malformed JSON, bad number or enum, missing parameter |
| 404 | Enrollment, student, course or endpoint not found |
| 405 / 415 | Wrong HTTP method / body is not JSON |
| 409 | Business rule: duplicate, student not active, course full or not open, enrollment not active |
| 503 | The student-service or course-service is unavailable |

Examples assume the seed data of the other two services:
students `1` to `4`, `7`, `8`, `10` are ACTIVE, `5` is INACTIVE, `6` GRADUATED, `9` SUSPENDED.
Courses `1` to `6` are open with seats, `7` (CS499) is full, `8` is DRAFT, `9` is CLOSED.

## Enrollment object

| Field | Notes |
|---|---|
| id | Generated |
| studentId, courseId | Ids owned by the other services |
| status | `ENROLLED`, `DROPPED`, `COMPLETED` |
| enrolledAt | Set on enroll, reset on re-enroll |
| droppedAt, completedAt | Null until that happens |

---

## 1. Enroll: `POST /api/enrollments`

```bash
curl -X POST http://localhost:8083/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{ "studentId": 1, "courseId": 1 }'
```

**201 Created**
```json
{ "success": true, "message": "Enrollment created",
  "data": { "id": 1, "studentId": 1, "courseId": 1, "status": "ENROLLED",
            "enrolledAt": "2026-11-07T10:30:00.123", "droppedAt": null, "completedAt": null } }
```

The service checks the student, rejects duplicates, reserves a seat, then stores the row.
A pair that was previously `DROPPED` is re-enrolled (same row, new `enrolledAt`).

| Try | Result |
|---|---|
| `{}` | **400** `data`: `studentId`, `courseId` field messages |
| same request twice | **409** `Student 1 is already enrolled in course 1` |
| `studentId` 5 | **409** `Student 5 is not active (status: INACTIVE)` |
| `studentId` 99 | **404** `Student not found with id: 99` |
| `courseId` 99 | **404** `Course not found with id: 99` |
| `courseId` 7 | **409** `Course CS499 is full` |
| `courseId` 8 | **409** `Course PH310 is not open for enrollment (status: DRAFT)` |
| course-service stopped | **503** `Course service is currently unavailable. Please try again shortly` |

---

## 2. Is this student enrolled? `GET /api/enrollments/check?studentId=1&courseId=1`

```bash
curl "http://localhost:8083/api/enrollments/check?studentId=1&courseId=1"
```
```json
{ "success": true, "message": "Check complete",
  "data": { "enrolled": true, "enrollmentId": 1, "status": "ENROLLED" } }
```
`enrolled` is true only for status `ENROLLED`. A dropped or completed pair returns `enrolled: false` but still shows
`enrollmentId` and `status`. No row at all returns `{ "enrolled": false, "enrollmentId": null, "status": null }`.
A missing parameter returns **400** `Required parameter 'courseId' is missing`.

## 3. List and filter: `GET /api/enrollments`

| Parameter | Default | Description |
|---|---|---|
| studentId | | Only this student |
| courseId | | Only this course (the roster, as student ids) |
| status | | `ENROLLED`, `DROPPED`, `COMPLETED` |
| page, size | 0, 10 | Size is capped at 100 |
| sortBy, direction | id, asc | `id, studentId, courseId, status, enrolledAt`. Unknown values fall back to `id` |

```bash
curl "http://localhost:8083/api/enrollments?courseId=1&status=ENROLLED"
curl "http://localhost:8083/api/enrollments?studentId=1&sortBy=enrolledAt&direction=desc"
```
**200**: `data` is a Spring page (`content`, `totalElements`, `totalPages`, `number`, `size`, ...). Empty results are still 200.
**400**: invalid `status`, for example `Invalid value 'BOGUS' for parameter 'status'`.

## 4. A student's courses, with details: `GET /api/enrollments/student/{studentId}/courses`

Joins this student's `ENROLLED` rows with course details from the course-service in one batch call.

```bash
curl http://localhost:8083/api/enrollments/student/1/courses
```
```json
{ "success": true, "message": "Courses retrieved",
  "data": [ { "enrollmentId": 1, "status": "ENROLLED", "enrolledAt": "2026-11-07T10:30:00.123",
              "course": { "id": 1, "code": "CS101", "title": "Introduction to Programming",
                          "department": "Engineering", "semester": "2026-FALL", "credits": 4,
                          "capacity": 60, "enrolledCount": 1, "availableSeats": 59,
                          "full": false, "status": "OPEN" } } ] }
```
No enrollments returns `"data": []`. `course` is `null` if the course-service no longer knows that id.
**503** if the course-service is unavailable.

## 5. Get one: `GET /api/enrollments/{id}`

```bash
curl http://localhost:8083/api/enrollments/1
```
**200**: enrollment object. **404**: `Enrollment not found with id: 99`. **400**: id is not a number.

## 6. Drop: `POST /api/enrollments/{id}/drop`

A soft drop. The row stays with status `DROPPED` and the seat is released in the course-service.
Only an `ENROLLED` row can be dropped, and concurrent drops release exactly one seat.

```bash
curl -X POST http://localhost:8083/api/enrollments/1/drop
```
**200**: enrollment with `"status": "DROPPED"` and `droppedAt` set.
**404**: not found. **409**: `Enrollment 1 is not active (status: DROPPED)` (dropping twice).
If the seat release fails, the drop still succeeds, the failure is logged at ERROR, and the course counter
needs reconciliation (see README).

## 7. Complete: `POST /api/enrollments/{id}/complete`

`ENROLLED` to `COMPLETED`. The seat stays counted.
```bash
curl -X POST http://localhost:8083/api/enrollments/2/complete
```
**200**: enrollment with `"status": "COMPLETED"` and `completedAt` set. **404**, **409** as for drop.

---

## Walkthrough that exercises everything

```
POST /api/enrollments  {1, 1}          201   course 1 enrolledCount becomes 1
POST /api/enrollments  {1, 1}          409   duplicate
GET  /api/enrollments/check?1&1        enrolled: true
GET  /api/enrollments/student/1/courses     CS101 with enrolledCount 1
POST /api/enrollments/1/drop           200   course 1 enrolledCount back to 0
POST /api/enrollments/1/drop           409   already dropped, no second seat released
GET  /api/enrollments/check?1&1        enrolled: false, status DROPPED
POST /api/enrollments  {1, 1}          201   same row id 1 re-enrolled, enrolledCount 1 again
```
Verify the counts on the course side with `GET http://localhost:8082/api/courses/1`.
