# Design specification

## Boundary and API

This service manages one configured course offering. `CourseWaitlistController` exposes:

- `POST /api/enrollments` with `{ "learnerId": "...", "prerequisiteSatisfied": true|false }`
- `DELETE /api/enrollments/{learnerId}`
- `GET /api/course` for a read-only snapshot of capacity, enrolled learners, and the ordered waitlist

The controller validates request fields and maps invalid IDs, unmet prerequisites, and missing enrollments to clear HTTP status codes. The service owns all state transitions and returns explicit result outcomes.

## Assumptions and eligibility policy

- There is one offering, one prerequisite rule, and a positive configured capacity (default 2).
- `prerequisiteSatisfied` is a caller-provided assertion. This prototype does not connect to a learner/course history system or independently verify it.
- If a seat is available, a learner without the prerequisite is rejected and not recorded.
- If the offering is full, the request is recorded once in the waitlist even if its prerequisite assertion is false. A waitlist entry records that assertion as a snapshot; it is not refreshed later.
- When a seat opens, the service walks the queue in insertion order, removes ineligible entries it encounters, and promotes the first eligible entry. Thus, an ineligible learner at the front is skipped and removed; later eligible learners retain their relative FIFO order. If no eligible learner exists, the seat remains open.
- Learner IDs are trimmed and compared case-sensitively. A learner may be enrolled or waitlisted, never both.

This policy makes the waitlist an interest queue while still requiring eligibility at both enrollment and promotion. There is deliberately no configurable policy engine or prerequisite-update workflow.

## State and invariants

`CourseWaitlistService` holds enrolled learner IDs in insertion order and waitlisted learner IDs with their eligibility snapshots in insertion order.

The service enforces these invariants after every operation:

1. Enrolled count never exceeds configured capacity.
2. No normalized learner ID occurs more than once, across either collection.
3. Only a learner with a satisfied prerequisite can be enrolled.
4. A drop only changes state if that learner is currently enrolled.
5. A successful drop creates at most one promotion; it selects the earliest currently eligible entry.
6. Duplicate enrollment attempts and invalid/repeated drops do not mutate state.

## Concurrency and persistence

All state reads and transitions are synchronized on the service instance. This serializes enrollment, drop/promotion, and snapshot reads for the single process. It is suitable for the bounded in-memory prototype and is covered by a concurrent enrollment test.

There is no database or cross-process locking. State is lost on restart, and multiple application instances would not share state. Production persistence would require a transactional store and a uniqueness constraint on learner/offering membership, with promotion performed in the same transaction as the drop.

## Known limitations

- Prerequisite data is trusted from the API caller; authentication, authorization, and authoritative academic records are out of scope.
- State is volatile and single-process only.
- The service intentionally does not implement catalogs, multiple offerings/terms, cancellation history, or waitlist notifications.
