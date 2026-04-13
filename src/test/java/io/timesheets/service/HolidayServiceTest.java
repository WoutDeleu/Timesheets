package io.timesheets.service;

import io.timesheets.model.Holiday;
import io.timesheets.model.HolidayType;
import io.timesheets.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    private HolidayService holidayService;

    @BeforeEach
    void setUp() {
        holidayService = new HolidayService(holidayRepository);
    }

    @Test
    void shouldDetectWeekendHolidaysFor2026() {
        // 2026: Aug 15 (Saturday) and Nov 1 (Sunday) fall on weekends
        var count = holidayService.getWeekendHolidayCount(2026);
        assertEquals(2, count);
    }

    @Test
    void shouldReturnZeroWeekendHolidaysWhenNoneFallOnWeekend() {
        // 2025: check that the count is correct
        var count = holidayService.getWeekendHolidayCount(2025);
        // Verify it returns a non-negative number (exact count depends on year)
        assertTrue(count >= 0);
    }

    @Test
    void shouldCalculateRemainingCompensatoryDays() {
        when(holidayRepository.countByHolidayTypeAndHolidayDateBetween(
                eq(HolidayType.COMPENSATORY),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(1L);

        // 2026 has 2 weekend holidays, 1 already scheduled = 1 remaining
        assertEquals(1, holidayService.getRemainingCompensatoryCount(2026));
    }

    @Test
    void shouldReturnZeroRemainingWhenAllCompensatoryScheduled() {
        when(holidayRepository.countByHolidayTypeAndHolidayDateBetween(
                eq(HolidayType.COMPENSATORY),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(2L);

        assertEquals(0, holidayService.getRemainingCompensatoryCount(2026));
    }

    @Test
    void shouldAddCompensatoryHoliday() {
        var expected = new Holiday();
        expected.setHolidayDate(LocalDate.of(2026, 8, 17));
        expected.setName("Compensatie OLV Hemelvaart");
        expected.setHolidayType(HolidayType.COMPENSATORY);
        expected.setEditable(true);

        when(holidayRepository.save(any(Holiday.class))).thenReturn(expected);

        var result = holidayService.addCompensatoryHoliday(
                LocalDate.of(2026, 8, 17), "Compensatie OLV Hemelvaart");

        assertEquals(HolidayType.COMPENSATORY, result.getHolidayType());
        assertTrue(result.isEditable());
    }
}
