package dev.assessment.waitlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static dev.assessment.waitlist.RegistrationResult.Outcome;

@Service
public class CourseWaitlistService {
    private final int capacity;
    private final Set<String> enrolled = new LinkedHashSet<>();
    private final Map<String, Boolean> waitlist = new LinkedHashMap<>();

    public CourseWaitlistService(@Value("${waitlist.course.capacity:2}") int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Course capacity must be at least 1");
        }
        this.capacity = capacity;
    }

    public synchronized RegistrationResult enroll(String learnerId, boolean prerequisiteSatisfied) {
        String normalizedId = normalize(learnerId);
        if (normalizedId == null) {
            return result(Outcome.INVALID_LEARNER_ID, learnerId, null, null, "Learner ID must not be blank");
        }
        if (enrolled.contains(normalizedId)) {
            return result(Outcome.ALREADY_ENROLLED, normalizedId, null, null, "Learner is already enrolled");
        }
        if (waitlist.containsKey(normalizedId)) {
            return result(Outcome.ALREADY_WAITLISTED, normalizedId, positionOf(normalizedId), null,
                    "Learner is already waitlisted");
        }
        if (enrolled.size() < capacity) {
            if (!prerequisiteSatisfied) {
                return result(Outcome.PREREQUISITE_NOT_MET, normalizedId, null, null,
                        "Learner does not satisfy the prerequisite");
            }
            enrolled.add(normalizedId);
            return result(Outcome.ENROLLED, normalizedId, null, null, "Enrollment confirmed");
        }

        waitlist.put(normalizedId, prerequisiteSatisfied);
        return result(Outcome.WAITLISTED, normalizedId, positionOf(normalizedId), null,
                "Course is full; learner added to the waitlist");
    }

    public synchronized RegistrationResult drop(String learnerId) {
        String normalizedId = normalize(learnerId);
        if (normalizedId == null) {
            return result(Outcome.INVALID_LEARNER_ID, learnerId, null, null, "Learner ID must not be blank");
        }
        if (!enrolled.remove(normalizedId)) {
            return result(Outcome.NOT_ENROLLED, normalizedId, null, null, "Learner is not enrolled");
        }

        String promotedLearnerId = promoteNextEligible();
        return result(Outcome.DROPPED, normalizedId, null, promotedLearnerId,
                promotedLearnerId == null
                        ? "Enrollment dropped; no eligible learner was available to promote"
                        : "Enrollment dropped; next eligible learner promoted");
    }

    public synchronized CourseSnapshot snapshot() {
        List<CourseSnapshot.WaitlistedLearner> waitlisted = new ArrayList<>(waitlist.size());
        int position = 1;
        for (Map.Entry<String, Boolean> entry : waitlist.entrySet()) {
            waitlisted.add(new CourseSnapshot.WaitlistedLearner(position++, entry.getKey(), entry.getValue()));
        }
        return new CourseSnapshot(capacity, new ArrayList<>(enrolled), waitlisted);
    }

    private String promoteNextEligible() {
        Iterator<Map.Entry<String, Boolean>> iterator = waitlist.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Boolean> candidate = iterator.next();
            iterator.remove();
            if (candidate.getValue()) {
                enrolled.add(candidate.getKey());
                return candidate.getKey();
            }
        }
        return null;
    }

    private Integer positionOf(String learnerId) {
        int position = 1;
        for (String queuedLearner : waitlist.keySet()) {
            if (queuedLearner.equals(learnerId)) {
                return position;
            }
            position++;
        }
        return null;
    }

    private RegistrationResult result(
            Outcome outcome, String learnerId, Integer position, String promotedLearnerId, String message) {
        return new RegistrationResult(outcome, learnerId, enrolled.size(), capacity, position, promotedLearnerId,
                message);

    }
    private String normalize(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            return null;
        }
        return learnerId.strip();
    }
}
