package gg.jos.jossentials.admin;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class SeenFormatter {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
        .withZone(ZoneId.systemDefault());

    private SeenFormatter() {
    }

    public static String formatTimestamp(long millis) {
        return TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(millis));
    }

    public static String formatElapsed(long millis) {
        Duration duration = Duration.between(Instant.ofEpochMilli(millis), Instant.now());
        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }

        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        long seconds = duration.getSeconds();

        if (days > 0) {
            if (hours > 0) {
                return days + "d " + hours + "h";
            }
            return days + "d";
        }
        if (hours > 0) {
            if (minutes > 0) {
                return hours + "h " + minutes + "m";
            }
            return hours + "h";
        }
        if (minutes > 0) {
            if (seconds > 0) {
                return minutes + "m " + seconds + "s";
            }
            return minutes + "m";
        }
        return Math.max(0, seconds) + "s";
    }
}
