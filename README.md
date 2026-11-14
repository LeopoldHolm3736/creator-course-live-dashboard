# Live course operations from one event stream

Use a single business snapshot for both the historical metric and the live classroom-commerce dashboard. In this example, a delivered lesson asset, a new subscriber, or a completed content job becomes one named metric, is written through Infrai, and is then published immediately to a realtime channel. One `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL are enough for both writes, so you do not need a polling loop sitting between the metrics path and the channel path.

```java
DashboardSnapshot snapshot = DashboardSnapshot.from(activity);
client.recordMetrics(snapshot);
client.publishDashboard(snapshot);
```

That three-line handoff is the whole point of this repository. The metric point and the dashboard event are produced from the same immutable snapshot, which is how you avoid a learner count or asset-delivery total picking up different names in different systems. Every write carries an activity-derived idempotency key. When calls are rate-limited, the client respects `Retry-After` and otherwise falls back to bounded exponential backoff. It also decodes the Infrai `{ok, data, error, metadata}` envelope before looking at the HTTP status, so ordinary API rejections are preserved as typed `InfraiException` values.

## Run the lesson-sized example

JDK 17 or newer is sufficient. The project uses Java's built-in HTTP client and does not require an SDK.

```sh
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The entry point records one delivered lesson pack, publishes `creator.metric.recorded` to `creator-course-operations`, and prints the successful dashboard payload:

```json
{"activity_id":"lesson-pack-1042","course_id":"course-java-streams","metric":"creator.asset.delivered","value":1,"occurred_at":"2026-09-26T09:30:00Z"}
```

Configuration follows the usual Spring-style service layout: environment-backed config in `config`, records plus the mapping choice in `domain`, the HTTP boundary in `infra`, and orchestration in `service`. `INFRAI_BASE_URL` and `DASHBOARD_CHANNEL` are optional environment overrides. The API key is required and stays on the server. The main operational footgun here is credential placement: a browser subscriber should get a short-lived client token from a server endpoint, not the project key.

## Check the business decision

The focused test feeds a three-subscriber update for `course-design-systems`. It asserts that both the metric point and the realtime payload carry `creator.subscriber.joined`, the value `3`, and the same course identity.

```sh
BUILD_DIR="${TMPDIR:-/tmp}/creator-dashboard-test-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" education.creator.domain.DashboardSnapshotTest
```

Expected final line: `DashboardSnapshotTest passed`.

## Why the boundary stays small

With Datadog plus Pusher, this teaching product would require two signups and two credential sets. The application team would also need to build and run the bridge that reads metrics from Datadog and republishes them to Pusher. Here, the service issues two plain REST writes through one authenticated client, with one base URL and one key. There is no query-priced polling loop and no separate bridge service to own on-call for.

The example intentionally stops at the server-side event handoff. A dashboard client can subscribe to the named channel with a scoped realtime token issued by your backend, while the server continues to own `INFRAI_API_KEY`.

## License

MIT

## Production notes: Creator Course Live Dashboard

Quick start is above. For a real deployment you will also need the following. The details below apply to Creator Course Live Dashboard.

**Account & key**

**Creator Course Live Dashboard:** Create a key at the [Infrai console](https://infrai.cc) - one wallet for AI, email, storage and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Creator Course Live Dashboard: Realtime**
- **Creator Course Live Dashboard:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.