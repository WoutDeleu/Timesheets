package io.timesheets.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class HourFormatter {

    /**
     * Parses hour input in either H:MM format (e.g. "7:36") or decimal format (e.g. "7.6" or "7,6").
     * Returns a BigDecimal representing decimal hours.
     */
    public static BigDecimal parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        var trimmed = input.trim();

        if (trimmed.contains(":")) {
            var parts = trimmed.split(":");
            var hours = Integer.parseInt(parts[0]);
            var minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return BigDecimal.valueOf(hours)
                    .add(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        }

        return new BigDecimal(trimmed.replace(",", "."));
    }

    /**
     * Formats decimal hours as H:MM string (e.g. 7.6 → "7:36").
     */
    public static String format(BigDecimal decimalHours) {
        if (decimalHours == null) {
            return "";
        }
        var totalMinutes = decimalHours.multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        var sign = totalMinutes < 0 ? "-" : "";
        var absTotalMinutes = Math.abs(totalMinutes);
        var hours = absTotalMinutes / 60;
        var minutes = absTotalMinutes % 60;
        return String.format("%s%d:%02d", sign, hours, minutes);
    }
}
