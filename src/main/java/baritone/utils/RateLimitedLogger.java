package baritone.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimitedLogger {

    private static final Map<String, Long> lastLogTime = new ConcurrentHashMap<>();
    private static final long DEFAULT_INTERVAL_MS = 5000;

    private RateLimitedLogger() {}

    public static void println(String message) {
        println(message, DEFAULT_INTERVAL_MS);
    }

    public static void println(String message, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = lastLogTime.get(message);
        if (last == null || now - last >= intervalMs) {
            lastLogTime.put(message, now);
            System.out.println(message);
        }
    }

    public static void println(String key, String message) {
        println(key, message, DEFAULT_INTERVAL_MS);
    }

    public static void println(String key, String message, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = lastLogTime.get(key);
        if (last == null || now - last >= intervalMs) {
            lastLogTime.put(key, now);
            System.out.println(message);
        }
    }
}
