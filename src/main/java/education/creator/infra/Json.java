package education.creator.infra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String string) return quote(string);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(quote(entry.getKey().toString())).append(':').append(stringify(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                first = false;
                out.append(stringify(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Cannot encode " + value.getClass().getName());
    }

    public static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.done()) throw new IllegalArgumentException("Unexpected JSON suffix");
        return value;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) { this.source = source; }
        private boolean done() { return index == source.length(); }
        private void whitespace() { while (!done() && Character.isWhitespace(source.charAt(index))) index++; }

        private Object value() {
            whitespace();
            if (done()) throw new IllegalArgumentException("Expected JSON value");
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return result;
            do {
                whitespace();
                String key = string();
                whitespace();
                expect(':');
                result.put(key, value());
                whitespace();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            index++;
            List<Object> result = new ArrayList<>();
            whitespace();
            if (take(']')) return result;
            do {
                result.add(value());
                whitespace();
            } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (!done()) {
                char c = source.charAt(index++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (done()) throw new IllegalArgumentException("Incomplete JSON escape");
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (index + 4 > source.length()) throw new IllegalArgumentException("Incomplete unicode escape");
                        result.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> throw new IllegalArgumentException("Unknown JSON escape");
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private Object number() {
            int start = index;
            while (!done() && "-+0123456789.eE".indexOf(source.charAt(index)) >= 0) index++;
            String token = source.substring(start, index);
            try {
                if (token.contains(".") || token.contains("e") || token.contains("E")) return Double.parseDouble(token);
                return Long.parseLong(token);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid JSON number", exception);
            }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, index)) throw new IllegalArgumentException("Invalid JSON literal");
            index += token.length();
            return value;
        }

        private boolean take(char expected) {
            if (!done() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected " + expected);
        }
    }
}
