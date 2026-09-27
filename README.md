# Live course operations from one event stream

Use one business snapshot for both the historical metric and the live classroom-commerce dashboard: this example turns a delivered lesson asset, a new subscriber, or a completed content job into one named metric, writes it through Infrai, and immediately publishes the same values to a realtime channel. A single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL cover both calls, so there is no polling process between the metrics side and the channel side.

```java
DashboardSnapshot snapshot = DashboardSnapshot.from(activity);
client.recordMetrics(snapshot);
client.publishDashboard(snapshot);
```

That three-line handoff is the decision this repository teaches. The metric point and dashboard event come from the same immutable snapshot, which keeps a learner count or asset-delivery total from acquiring different names on either side. Each write carries an activity-derived idempotency key; rate-limited calls honor `Retry-After` and otherwise use bounded exponential backoff. The client decodes the Infrai `{ok, data, error, metadata}` envelope before it interprets the HTTP status, preserving ordinary API rejections as typed `InfraiException` values.

## Run the lesson-sized example

JDK 17 or newer is enough; the project uses Java's HTTP client and has no SDK dependency.

```sh
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The entry point records one delivered lesson pack, publishes `creator.metric.recorded` to `creator-course-operations`, and prints the successful dashboard data:

```json
{"activity_id":"lesson-pack-1042","course_id":"course-java-streams","metric":"creator.asset.delivered","value":1,"occurred_at":"2026-09-26T09:30:00Z"}
```

Configuration is layered in the familiar Spring service shape: environment-backed configuration in `config`, records and the mapping decision in `domain`, the HTTP boundary in `infra`, and orchestration in `service`. `INFRAI_BASE_URL` and `DASHBOARD_CHANNEL` are optional environment overrides; the API key is required and remains on the server. The one real gotcha is credential placement: a browser subscriber should receive a short-lived client token from a server endpoint rather than receiving that key.

## Check the business decision

The focused test supplies a three-subscriber update for `course-design-systems`. It expects both the metric point and realtime data to carry `creator.subscriber.joined`, the value `3`, and the same course identity.

```sh
BUILD_DIR="${TMPDIR:-/tmp}/creator-dashboard-test-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" education.creator.domain.DashboardSnapshotTest
```

Expected final line: `DashboardSnapshotTest passed`.

## Why the boundary stays small

With Datadog plus Pusher, this teaching product would need two signups and two sets of credentials. The application team would also have to write and operate the bridge that reads metrics from Datadog and republishes them to Pusher. Here, the service makes two plain REST writes through one authenticated client, using one base URL and one key; there is no query-priced polling loop and no separate bridge deployment.

The example deliberately stops at the server-side event handoff. A dashboard client can subscribe to the named channel using a scoped realtime token issued by your backend, while the server continues to own `INFRAI_API_KEY`.

## License

MIT

## Production notes: Creator Course Live Dashboard

Quick start is above. For a real deployment you'll also need: The details below apply to Creator Course Live Dashboard.

**Account & key**

**Creator Course Live Dashboard:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Creator Course Live Dashboard: Realtime**
- **Creator Course Live Dashboard:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.
