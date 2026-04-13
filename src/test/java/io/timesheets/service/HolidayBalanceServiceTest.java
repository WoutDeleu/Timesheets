package io.timesheets.service;

import io.timesheets.model.LeaveType;
import io.timesheets.repository.LeaveEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayBalanceServiceTest {

    @Mock
    private LeaveEntryRepository leaveEntryRepository;

    @Mock
    private SettingsService settingsService;

    private HolidayBalanceService holidayBalanceService;

    @BeforeEach
    void setUp() {
        holidayBalanceService = new HolidayBalanceService(leaveEntryRepository, settingsService);
        lenient().when(settingsService.getVacationDaysPerYear()).thenReturn(20);
    }

    @Test
    void shouldReturnFullBalanceWhenNoDaysUsed() {
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any()))
                .thenReturn(0L);

        assertEquals(20, holidayBalanceService.getRemainingDays(2026));
    }

    @Test
    void shouldDecrementBalanceWhenDaysUsed() {
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any()))
                .thenReturn(5L);

        assertEquals(15, holidayBalanceService.getRemainingDays(2026));
        assertEquals(5, holidayBalanceService.getUsedDays(2026));
    }

    @Test
    void shouldReturnZeroWhenAllDaysUsed() {
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any()))
                .thenReturn(20L);

        assertEquals(0, holidayBalanceService.getRemainingDays(2026));
    }

    @Test
    void shouldReturnNegativeWhenOverdrawn() {
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any()))
                .thenReturn(22L);

        assertEquals(-2, holidayBalanceService.getRemainingDays(2026));
    }

    @Test
    void shouldReturnNullExhaustionDateWhenNoDaysUsed() {
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any()))
                .thenReturn(0L);

        assertNull(holidayBalanceService.predictExhaustionDate(2026));
    }

    @Test
    void shouldReturnNullExhaustionDateWhenAllDaysUsed() {
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any()))
                .thenReturn(20L);

        assertNull(holidayBalanceService.predictExhaustionDate(2026));
    }

    @Test
    void shouldCountRemainingWorkdaysExcludingWeekends() {
        var workdays = holidayBalanceService.getRemainingWorkdaysInYear(LocalDate.now().getYear());
        assertTrue(workdays > 0);
        assertTrue(workdays <= 261);
    }
}
