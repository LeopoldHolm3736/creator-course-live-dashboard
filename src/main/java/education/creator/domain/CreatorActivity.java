package education.creator.domain;

import java.time.Instant;
import java.util.Objects;

public record CreatorActivity(
        String activityId,
        String courseId,
        Kind kind,
        long quantity,
        Instant occurredAt) {

    public CreatorActivity {
        if (activityId == null || activityId.isBlank()) throw new IllegalArgumentException("activityId is required");
        if (courseId == null || courseId.isBlank()) throw new IllegalArgumentException("courseId is required");
        Objects.requireNonNull(kind, "kind is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
    }

    public enum Kind {
        ASSET_DELIVERED("creator.asset.delivered"),
        SUBSCRIBER_JOINED("creator.subscriber.joined"),
        CONTENT_PROCESSED("creator.content.processed");

        private final String metricName;

        Kind(String metricName) {
            this.metricName = metricName;
        }

        public String metricName() {
            return metricName;
        }
    }
}
