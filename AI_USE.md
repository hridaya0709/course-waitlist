# AI prompt and use record

## Prompt used

The user supplied the focused course-registration assessment, including the requirements for one offering, enrollment/drop/FIFO promotion, prerequisite and waitlist assumptions, tests, design documentation, AI-use evidence, and explicitly requested Java 21 with Spring Boot.

## Relevant AI response summary

The AI reviewed the workspace and found no application starter or existing Java build/test conventions. It proposed creating a small Spring Boot REST service in a separate folder, using synchronized in-memory state, a FIFO waitlist, explicit result statuses, focused service tests, and concise design/run/use documentation.

## Review and disposition

- **Accepted:** Keep the implementation scoped to one offering, with an in-memory service and synchronized transitions. This fits the supplied fallback and avoids introducing a database.
- **Accepted with refinement:** Use an ordered map for waitlist entries and an ordered set for enrollments; make every service state transition and snapshot synchronized. The implementation and concurrency test were reviewed against the capacity and uniqueness invariants.
- **Policy decision:** Record full-course waitlist requests regardless of the prerequisite snapshot, then skip and remove ineligible entries when promotion reaches them. Reject ineligible requests when a seat is available. This explicitly handles an ineligible queue head without promoting them.
- **Rejected/out of scope:** No database, identity integration, frontend, or policy engine was added because the assessment excludes these and the fallback permits in-memory state.

## Files and decisions informed

- `src/main/java/dev/assessment/waitlist/CourseWaitlistService.java` — serialized in-memory transitions, duplicate checks, capacity, and promotion.
- `src/main/java/dev/assessment/waitlist/CourseWaitlistController.java` and `EnrollmentRequest.java` — REST boundary, validation, and response mapping.
- `src/test/java/dev/assessment/waitlist/CourseWaitlistServiceTest.java` — focused behavior and concurrency tests.
- `DESIGN.md` and `README.md` — explicit assumptions, policy, limitations, and run instructions.

## Independent verification

The AI-assisted implementation was independently checked with:

- `mvn test` — **BUILD SUCCESS**, 6 tests passed, 0 failures/errors. This includes five service tests for capacity, duplicates, FIFO promotion past an ineligible front entry, invalid/repeated drops, and concurrent enrollment, plus a MockMvc test of request validation and the HTTP enrollment/drop/promotion path.
- `mvn -B package` — **BUILD SUCCESS**; produced `target/course-waitlist-0.0.1-SNAPSHOT.jar`. The package run also repeated all 6 tests successfully and loaded the Spring application context in the HTTP test.

The test commands were run from the `course-waitlist` project directory on JDK 21.0.10.
