package dev.assessment.waitlist;

import java.util.List;

public record CourseSnapshot(int capacity, List<String> enrolled, List<WaitlistedLearner> waitlist) {
    public CourseSnapshot {
        enrolled = List.copyOf(enrolled);
        waitlist = List.copyOf(waitlist);
    }

    public record WaitlistedLearner(int position, String learnerId, boolean prerequisiteSatisfied) {
    }
}
