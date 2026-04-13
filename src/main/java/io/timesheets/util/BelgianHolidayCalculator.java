package io.timesheets.util;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calculates all Belgian national holidays for a given year.
 * Uses the Computus algorithm (Anonymous Gregorian) for Easter-based movable holidays.
 */
public final class BelgianHolidayCalculator {

    private BelgianHolidayCalculator() {}

    /**
     * Returns all 10 Belgian national holidays for the given year.
     * Map key = date, value = holiday name.
     */
    public static Map<LocalDate, String> getHolidays(int year) {
        var easter = calculateEasterSunday(year);
        var holidays = new LinkedHashMap<LocalDate, String>();

        holidays.put(LocalDate.of(year, 1, 1), "Nieuwjaar");
        holidays.put(easter.plusDays(1), "Paasmaandag");
        holidays.put(LocalDate.of(year, 5, 1), "Dag van de Arbeid");
        holidays.put(easter.plusDays(39), "Onze-Lieve-Heer-Hemelvaart");
        holidays.put(easter.plusDays(50), "Pinkstermaandag");
        holidays.put(LocalDate.of(year, 7, 21), "Nationale Feestdag");
        holidays.put(LocalDate.of(year, 8, 15), "Onze-Lieve-Vrouw-Hemelvaart");
        holidays.put(LocalDate.of(year, 11, 1), "Allerheiligen");
        holidays.put(LocalDate.of(year, 11, 11), "Wapenstilstand");
        holidays.put(LocalDate.of(year, 12, 25), "Kerstmis");

        return holidays;
    }

    /**
     * Computus algorithm — Anonymous Gregorian method.
     * Returns Easter Sunday for the given year.
     */
    public static LocalDate calculateEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }
}
