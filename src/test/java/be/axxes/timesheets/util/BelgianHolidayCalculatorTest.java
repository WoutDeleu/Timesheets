package be.axxes.timesheets.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BelgianHolidayCalculatorTest {

    @Test
    void shouldCalculateEasterCorrectlyFor2026() {
        var easter = BelgianHolidayCalculator.calculateEasterSunday(2026);
        assertEquals(LocalDate.of(2026, 4, 5), easter);
    }

    @Test
    void shouldCalculateEasterCorrectlyFor2025() {
        var easter = BelgianHolidayCalculator.calculateEasterSunday(2025);
        assertEquals(LocalDate.of(2025, 4, 20), easter);
    }

    @Test
    void shouldReturnTenNationalHolidays() {
        var holidays = BelgianHolidayCalculator.getHolidays(2026);
        assertEquals(10, holidays.size());
    }

    @Test
    void shouldContainAllFixedHolidays() {
        var holidays = BelgianHolidayCalculator.getHolidays(2026);

        assertTrue(holidays.containsKey(LocalDate.of(2026, 1, 1)), "Nieuwjaar");
        assertTrue(holidays.containsKey(LocalDate.of(2026, 5, 1)), "Dag van de Arbeid");
        assertTrue(holidays.containsKey(LocalDate.of(2026, 7, 21)), "Nationale Feestdag");
        assertTrue(holidays.containsKey(LocalDate.of(2026, 8, 15)), "OLV Hemelvaart");
        assertTrue(holidays.containsKey(LocalDate.of(2026, 11, 1)), "Allerheiligen");
        assertTrue(holidays.containsKey(LocalDate.of(2026, 11, 11)), "Wapenstilstand");
        assertTrue(holidays.containsKey(LocalDate.of(2026, 12, 25)), "Kerstmis");
    }

    @Test
    void shouldCalculateMovableHolidaysFor2026() {
        var holidays = BelgianHolidayCalculator.getHolidays(2026);
        var easter = LocalDate.of(2026, 4, 5);

        // Paasmaandag = Easter + 1
        assertTrue(holidays.containsKey(easter.plusDays(1)), "Paasmaandag");
        // Hemelvaart = Easter + 39
        assertTrue(holidays.containsKey(easter.plusDays(39)), "Hemelvaart");
        // Pinkstermaandag = Easter + 50
        assertTrue(holidays.containsKey(easter.plusDays(50)), "Pinkstermaandag");
    }
}
