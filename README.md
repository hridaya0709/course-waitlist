__# Course Waitlist

A Java 21 / Spring Boot service for enrollment, dropping, and FIFO waitlist promotion for one course offering. The service uses in-memory state; restarting the application clears all enrollment and waitlist data.

## Requirements and run

- JDK 21
- Maven 3.9+

From this directory, run the focused automated tests:

```powershell
mvn test
```

Start the service:

```powershell
mvn spring-boot:run
```

The default capacity is 2. Override it at startup with `--waitlist.course.capacity=10`.
To build and run the executable jar instead:

```powershell
mvn package
java -jar target/course-waitlist-0.0.1-SNAPSHOT.jar
```

## HTTP API

Enroll or request a place on the waitlist:

```powershell
curl.exe -i -X POST http://localhost:8080/api/enrollments `
  -H "Content-Type: application/json" `
  -d '{"learnerId":"learner-1","prerequisiteSatisfied":true}'
```

Drop an enrolled learner:

```powershell
curl.exe -i -X DELETE http://localhost:8080/api/enrollments/learner-1
```

Inspect the offering's current state:

```powershell
curl.exe http://localhost:8080/api/course
```

Enrollment requests return a JSON result with an outcome such as `ENROLLED`, `WAITLISTED`, `ALREADY_ENROLLED`, or `ALREADY_WAITLISTED`. A missing prerequisite when a seat is available returns `422`; malformed requests return `400`. A drop of a learner who is not enrolled returns `404` and does not change state.

See [DESIGN.md](./DESIGN.md) for invariants, policy, and tradeoffs, and [AI_USE.md](./AI_USE.md) for the prompt/use record and verification evidence.
