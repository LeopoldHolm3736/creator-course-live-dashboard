package education.creator.domain;

import java.time.Instant;

public final class DashboardSnapshotTest {
    public static void main(String[] args) {
        CreatorActivity input = new CreatorActivity(
                "subscriber-17",
                "course-design-systems",
                CreatorActivity.Kind.SUBSCRIBER_JOINED,
                3,
                Instant.parse("2026-09-26T10:15:30Z"));

        DashboardSnapshot snapshot = DashboardSnapshot.from(input);

        assertEquals("creator.subscriber.joined", snapshot.dashboardData().get("metric"));
        assertEquals(3L, snapshot.dashboardData().get("value"));
        assertEquals("creator.subscriber.joined", snapshot.metricPoints().get(0).get("name"));
        assertEquals("course-design-systems",
                ((java.util.Map<?, ?>) snapshot.metricPoints().get(0).get("tags")).get("course_id"));
        assertEquals("creator.metric.recorded", snapshot.event());
        System.out.println("DashboardSnapshotTest passed");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }
}
