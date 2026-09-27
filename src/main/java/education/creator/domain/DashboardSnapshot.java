package education.creator.domain;

import java.util.List;
import java.util.Map;

public record DashboardSnapshot(
        String activityId,
        String event,
        Map<String, Object> dashboardData,
        List<Map<String, Object>> metricPoints) {

    public static DashboardSnapshot from(CreatorActivity activity) {
        Map<String, Object> data = Map.of(
                "activity_id", activity.activityId(),
                "course_id", activity.courseId(),
                "metric", activity.kind().metricName(),
                "value", activity.quantity(),
                "occurred_at", activity.occurredAt().toString());
        Map<String, Object> point = Map.of(
                "name", activity.kind().metricName(),
                "value", activity.quantity(),
                "timestamp", activity.occurredAt().toString(),
                "tags", Map.of("course_id", activity.courseId()));
        return new DashboardSnapshot(activity.activityId(), "creator.metric.recorded", data, List.of(point));
    }
}
