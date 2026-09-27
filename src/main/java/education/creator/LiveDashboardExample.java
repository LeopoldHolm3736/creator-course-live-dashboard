package education.creator;

import education.creator.config.InfraiConfig;
import education.creator.domain.CreatorActivity;
import education.creator.domain.DashboardSnapshot;
import education.creator.infra.InfraiClient;
import education.creator.infra.Json;
import education.creator.service.CreatorCommerceStream;

import java.time.Instant;

public final class LiveDashboardExample {
    private LiveDashboardExample() {}

    public static void main(String[] args) {
        InfraiConfig config = InfraiConfig.fromEnvironment();
        InfraiClient client = new InfraiClient(config);
        CreatorCommerceStream stream = new CreatorCommerceStream(client);

        CreatorActivity activity = new CreatorActivity(
                "lesson-pack-1042",
                "course-java-streams",
                CreatorActivity.Kind.ASSET_DELIVERED,
                1,
                Instant.parse("2026-09-26T09:30:00Z"));
        DashboardSnapshot delivered = stream.handle(activity);
        System.out.println(Json.stringify(delivered.dashboardData()));
    }
}
