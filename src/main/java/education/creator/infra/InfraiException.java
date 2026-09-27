package education.creator.infra;

import java.util.Map;

public final class InfraiException extends RuntimeException {
    private final String code;
    private final int statusCode;
    private final Map<String, Object> details;

    public InfraiException(String code, Map<String, Object> details, int statusCode) {
        super(code + " (HTTP " + statusCode + ")");
        this.code = code;
        this.statusCode = statusCode;
        this.details = Map.copyOf(details);
    }

    public String code() { return code; }
    public int statusCode() { return statusCode; }
    public Map<String, Object> details() { return details; }
}
