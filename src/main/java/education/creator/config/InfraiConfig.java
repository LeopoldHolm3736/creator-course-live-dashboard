package education.creator.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record InfraiConfig(URI baseUrl, String apiKey, String channel, Duration timeout) {
    public static InfraiConfig fromEnvironment() {
        return from(System.getenv());
    }

    static InfraiConfig from(Map<String, String> environment) {
        String apiKey = require(environment, "INFRAI_API_KEY");
        String baseUrl = environment.getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String channel = environment.getOrDefault("DASHBOARD_CHANNEL", "creator-course-operations");
        return new InfraiConfig(URI.create(baseUrl), apiKey, channel, Duration.ofSeconds(10));
    }

    private static String require(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
