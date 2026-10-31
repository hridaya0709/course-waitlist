package dev.assessment.waitlist;

public record RegistrationResult(
        Outcome outcome,
        String learnerId,
        int enrolledCount,
        int capacity,
        Integer waitlistPosition,
        String promotedLearnerId,
        String message) {

    public enum Outcome {
        ENROLLED,
        WAITLISTED,
        ALREADY_ENROLLED,
        ALREADY_WAITLISTED,
        PREREQUISITE_NOT_MET,
        DROPPED,
        NOT_ENROLLED,
        INVALID_LEARNER_ID
    }
}
