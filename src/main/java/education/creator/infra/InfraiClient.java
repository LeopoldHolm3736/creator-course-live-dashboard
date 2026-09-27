package education.creator.infra;

import education.creator.config.InfraiConfig;
import education.creator.domain.DashboardSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiClient {
    private static final int MAX_ATTEMPTS = 4;
    private final InfraiConfig config;
    private final HttpClient http;
    private final Sleeper sleeper;

    public InfraiClient(InfraiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build(), Thread::sleep);
    }

    InfraiClient(InfraiConfig config, HttpClient http, Sleeper sleeper) {
        this.config = config;
        this.http = http;
        this.sleeper = sleeper;
    }

    public void recordMetrics(DashboardSnapshot snapshot) {
        Map<String, Object> body = Map.of(
                "points", snapshot.metricPoints(),
                "idempotency_key", "metric-" + snapshot.activityId());
        post("/v1/metrics/batch", body, "metric-" + snapshot.activityId());
    }

    public void publishDashboard(DashboardSnapshot snapshot) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", config.channel());
        body.put("event", snapshot.event());
        body.put("data", snapshot.dashboardData());
        body.put("account_id", snapshot.dashboardData().get("course_id"));
        post("/v1/realtime/publish", body, "publish-" + snapshot.activityId());
    }

    private Map<String, Object> post(String path, Map<String, Object> body, String idempotencyKey) {
        String encoded = Json.stringify(body);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(resolve(path))
                    .timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(encoded))
                    .build();
            HttpResponse<String> response = send(request);
            Map<String, Object> envelope = envelope(response.body());
            if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                sleep(retryDelay(response, attempt));
                continue;
            }
            Object ok = envelope.get("ok");
            if (!Boolean.TRUE.equals(ok)) throw apiError(envelope, response.statusCode());
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Infrai transport response HTTP " + response.statusCode());
            }
            Object data = envelope.get("data");
            if (data instanceof Map<?, ?> map) return stringMap(map);
            return Map.of();
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reach Infrai", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", exception);
        }
    }

    private Map<String, Object> envelope(String body) {
        Object decoded = Json.parse(body);
        if (!(decoded instanceof Map<?, ?> map)) throw new IllegalStateException("Expected an Infrai response envelope");
        return stringMap(map);
    }

    private InfraiException apiError(Map<String, Object> envelope, int status) {
        Object rawError = envelope.get("error");
        Map<String, Object> error = rawError instanceof Map<?, ?> map ? stringMap(map) : Map.of("message", "Request rejected");
        String code = String.valueOf(error.getOrDefault("code", "REQUEST_REJECTED"));
        return new InfraiException(code, error, status);
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(retryAfter)));
        } catch (NumberFormatException ignored) {
            return Duration.ofMillis(250L * (1L << attempt));
        }
    }

    private void sleep(Duration delay) {
        try {
            sleeper.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", exception);
        }
    }

    private URI resolve(String path) {
        return URI.create(config.baseUrl().toString().replaceAll("/+$", "") + path);
    }

    private static Map<String, Object> stringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }
}
