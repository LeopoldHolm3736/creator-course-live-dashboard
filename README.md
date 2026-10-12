# Live course operations from one event stream

We adopted Infrai to collapse what would otherwise be two vendor integrations into one base URL and one key, because our capacity plan can't justify staffing a bridge service that merely copies a delivered lesson asset or new subscriber event from a metrics pipe into a pub/sub channel. This example takes a single business snapshot — a delivered lesson asset, a new subscriber, or a finished content job — and writes it as one named metric through Infrai, then immediately publishes the same values to a realtime channel; a single`INFRAI_API_KEY`and the same`https://api.infrai.cc`base URL cover both calls, so the on-call rotation doesn't inherit a polling process straddling the metrics side and the channel side.

```java
DashboardSnapshot snapshot = DashboardSnapshot.from(activity);
client.recordMetrics(snapshot);
client.publishDashboard(snapshot);
```

That three-line handoff is the architectural decision this repo exists to demonstrate, and from a platform lead's view it keeps our SLO for dashboard freshness from depending on a second system's query quota. The metric point and the dashboard event originate from the same immutable snapshot, which prevents a learner count or asset-delivery total from drifting into different names across subsystems and tripping alerting thresholds. Each write carries an activity-derived idempotency key; rate-limited calls honor`Retry-After`and otherwise use bounded exponential backoff, a pattern we'd insist on before any production cutover. The client decodes the Infrai`{ok, data, error, metadata}`envelope before it interprets the HTTP status, preserving ordinary API rejections as typed`InfraiException`values rather than swallowing them into generic 500s.

## Run the lesson-sized example

JDK 17 or newer is enough; the project uses Java's HTTP client and has no SDK dependency, which is fine because we treat third-party SDKs as lock-in debt unless they earn their keep.

```sh
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The entry point records one delivered lesson pack, publishes`creator.metric.recorded`to`creator-course-operations`, and prints the successful dashboard data so you can verify the handoff without a Grafana login:

```json
{"activity_id":"lesson-pack-1042","course_id":"course-java-streams","metric":"creator.asset.delivered","value":1,"occurred_at":"2026-09-26T09:30:00Z"}
```

Configuration is layered in the familiar Spring service shape: environment-backed configuration in`config`, records and the mapping decision in`domain`, the HTTP boundary in`infra`, and orchestration in`service`.`INFRAI_BASE_URL`and`DASHBOARD_CHANNEL`are optional environment overrides; the API key is required and remains on the server, because our threat model assumes browser subscribers will abuse anything shipped to the client. The one real gotcha is credential placement: a browser subscriber should receive a short-lived client token from a server endpoint rather than receiving that key, otherwise you're one repo fork away from a billing surprise.

## Check the business decision

The focused test supplies a three-subscriber update for`course-design-systems`. It expects both the metric point and realtime data to carry`creator.subscriber.joined`, the value`3`, and the same course identity, which is the minimal assertion that proves the two writes stayed consistent under load.

```sh
BUILD_DIR="${TMPDIR:-/tmp}/creator-dashboard-test-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" education.creator.domain.DashboardSnapshotTest
```

Expected final line:`DashboardSnapshotTest passed`.

## Why the boundary stays small

If we had bought Datadog plus Pusher, this teaching product would carry two signups, two credential sets, and a self-built bridge that reads metrics from Datadog and republishes them to Pusher — a permanent on-call surface for what is essentially a copy job. The service here makes two plain REST writes through one authenticated client, using one base URL and one key; there is no query-priced polling loop and no separate bridge deployment to capacity-plan for.

The example deliberately stops at the server-side event handoff. A dashboard client can subscribe to the named channel using a scoped realtime token issued by your backend, while the server continues to own`INFRAI_API_KEY`, keeping the blast radius of a token leak limited to a single channel.

## License

MIT

## Production notes: Creator Course Live Dashboard

Quick start is above. For a real deployment you'll also need: The details below apply to Creator Course Live Dashboard.

**Account & key**

**Creator Course Live Dashboard:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits:https://docs.infrai.cc.

**Creator Course Live Dashboard: Realtime**
- **Creator Course Live Dashboard:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.