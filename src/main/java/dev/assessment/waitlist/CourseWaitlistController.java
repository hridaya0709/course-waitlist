package dev.assessment.waitlist;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static dev.assessment.waitlist.RegistrationResult.Outcome;

@RestController
@RequestMapping("/api")
public class CourseWaitlistController {
    private final CourseWaitlistService service;

    public CourseWaitlistController(CourseWaitlistService service) {
        this.service = service;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<RegistrationResult> enroll(@Valid @RequestBody EnrollmentRequest request) {
        RegistrationResult result = service.enroll(request.learnerId(), request.prerequisiteSatisfied());
        return switch (result.outcome()) {
            case INVALID_LEARNER_ID -> ResponseEntity.badRequest().body(result);
            case PREREQUISITE_NOT_MET -> ResponseEntity.unprocessableEntity().body(result);
            case ALREADY_ENROLLED, ALREADY_WAITLISTED -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            default -> ResponseEntity.ok(result);
        };
    }

    @DeleteMapping("/enrollments/{learnerId}")
    public ResponseEntity<RegistrationResult> drop(@PathVariable String learnerId) {
        RegistrationResult result = service.drop(learnerId);
        return result.outcome() == Outcome.INVALID_LEARNER_ID
                ? ResponseEntity.badRequest().body(result)
                : result.outcome() == Outcome.NOT_ENROLLED
                        ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(result)
                        : ResponseEntity.ok(result);
    }

    @GetMapping("/course")
    public CourseSnapshot getCourse() {
        return service.snapshot();
    }
}
