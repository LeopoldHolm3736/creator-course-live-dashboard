package education.creator.service;

import education.creator.domain.CreatorActivity;
import education.creator.domain.DashboardSnapshot;
import education.creator.infra.InfraiClient;

public final class CreatorCommerceStream {
    private final InfraiClient client;

    public CreatorCommerceStream(InfraiClient client) {
        this.client = client;
    }

    public DashboardSnapshot handle(CreatorActivity activity) {
        DashboardSnapshot snapshot = DashboardSnapshot.from(activity);
        client.recordMetrics(snapshot);
        client.publishDashboard(snapshot);
        return snapshot;
    }
}
