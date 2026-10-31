package dev.assessment.waitlist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import static dev.assessment.waitlist.RegistrationResult.Outcome;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseWaitlistServiceTest {
    @Test
    void enrollsWhenSpaceIsAvailableAndRejectsMissingPrerequisite() {
        CourseWaitlistService service = new CourseWaitlistService(2);

        assertEquals(Outcome.PREREQUISITE_NOT_MET, service.enroll("not-ready", false).outcome());
        assertEquals(Outcome.ENROLLED, service.enroll(" learner-1 ", true).outcome());
        assertEquals(List.of("learner-1"), service.snapshot().enrolled());
        assertEquals(2, service.snapshot().capacity());
    }

    @Test
    void queuesOnceWhenFullAndReportsDuplicateRequests() {
        CourseWaitlistService service = new CourseWaitlistService(1);
        service.enroll("learner-1", true);

        assertEquals(Outcome.WAITLISTED, service.enroll("learner-2", true).outcome());
        assertEquals(Outcome.ALREADY_WAITLISTED, service.enroll("learner-2", true).outcome());
        assertEquals(Outcome.ALREADY_ENROLLED, service.enroll("learner-1", true).outcome());
        assertEquals(1, service.snapshot().waitlist().size());
    }

    @Test
    void promotesFifoEligibleLearnerAndDiscardsIneligibleFrontEntries() {
        CourseWaitlistService service = new CourseWaitlistService(1);
        service.enroll("first", true);
        service.enroll("ineligible", false);
        service.enroll("second", true);
        service.enroll("third", true);

        RegistrationResult drop = service.drop("first");

        assertEquals(Outcome.DROPPED, drop.outcome());
        assertEquals("second", drop.promotedLearnerId());
        assertEquals(List.of("second"), service.snapshot().enrolled());
        assertEquals(List.of("third"), service.snapshot().waitlist().stream()
                .map(CourseSnapshot.WaitlistedLearner::learnerId).toList());
    }

    @Test
    void invalidAndRepeatedDropsDoNotChangeState() {
        CourseWaitlistService service = new CourseWaitlistService(1);
        service.enroll("first", true);
        service.enroll("queued", true);
        CourseSnapshot before = service.snapshot();

        assertEquals(Outcome.INVALID_LEARNER_ID, service.drop("  ").outcome());
        assertEquals(Outcome.NOT_ENROLLED, service.drop("queued").outcome());
        assertEquals(Outcome.DROPPED, service.drop("first").outcome());
        CourseSnapshot afterFirstDrop = service.snapshot();
        assertEquals(Outcome.NOT_ENROLLED, service.drop("first").outcome());
        assertEquals(afterFirstDrop, service.snapshot());
        assertEquals(1, before.waitlist().size());
        assertEquals(List.of("queued"), afterFirstDrop.enrolled());
    }

    @Test
    void concurrentRequestsNeverExceedCapacityOrDuplicateLearners() throws Exception {
        int capacity = 3;
        int requestCount = 100;
        CourseWaitlistService service = new CourseWaitlistService(capacity);
        ExecutorService executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegistrationResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                String learnerId = "learner-" + i;
                futures.add(executor.submit(() -> {
                    start.await();
                    return service.enroll(learnerId, true);
                }));
            }
            start.countDown();
            for (Future<RegistrationResult> future : futures) {
                assertTrue(List.of(Outcome.ENROLLED, Outcome.WAITLISTED).contains(future.get().outcome()));
            }
        } finally {
            executor.shutdownNow();
        }

        CourseSnapshot snapshot = service.snapshot();
        assertEquals(capacity, snapshot.enrolled().size());
        assertEquals(requestCount - capacity, snapshot.waitlist().size());
        HashSet<String> learnerIds = new HashSet<>(snapshot.enrolled());
        for (CourseSnapshot.WaitlistedLearner learner : snapshot.waitlist()) {
            assertTrue(learnerIds.add(learner.learnerId()));
        }
        assertEquals(requestCount, learnerIds.size());
        assertNull(service.drop("missing").promotedLearnerId());
    }
}
