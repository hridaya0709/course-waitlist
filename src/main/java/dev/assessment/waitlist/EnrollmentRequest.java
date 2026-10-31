package dev.assessment.waitlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentRequest(
        @NotBlank @Size(max = 100) String learnerId,
        @NotNull Boolean prerequisiteSatisfied) {
}
