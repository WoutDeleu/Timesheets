package be.axxes.timesheets.service;

import be.axxes.timesheets.model.LeaveType;
import be.axxes.timesheets.repository.LeaveEntryRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates holiday (verlof) balance and predictions.
 *
 * Logic:
 * - 20 vacation days per year (configurable), resets January 1
 * - Each VACATION leave entry consumes 1 day
 * - Predictions based on usage rate
 */
@Service
public class HolidayBalanceService {

    private final LeaveEntryRepository leaveEntryRepository;
    private final SettingsService settingsService;

    public HolidayBalanceService(LeaveEntryRepository leaveEntryRepository,
                                 SettingsService settingsService) {
        this.leaveEntryRepository = leaveEntryRepository;
        this.settingsService = settingsService;
    }

    /**
     * Total vacation days allowed per year.
     */
    public int getTotalDaysPerYear() {
        return settingsService.getVacationDaysPerYear();
    }

    /**
     * Vacation days used in the given year.
     */
    public long getUsedDays(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(LeaveType.VACATION, start, end);
    }

    /**
     * Remaining vacation days for the year.
     */
    public long getRemainingDays(int year) {
        return getTotalDaysPerYear() - getUsedDays(year);
    }

    /**
     * Count remaining workdays in the year from today.
     */
    public long getRemainingWorkdaysInYear(int year) {
        var today = LocalDate.now();
        if (today.getYear() != year) {
            // If asking about a different year, count all workdays
            today = LocalDate.of(year, 1, 1);
        }
        var endOfYear = LocalDate.of(year, 12, 31);

        long workdays = 0;
        var date = today;
        while (!date.isAfter(endOfYear)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workdays++;
            }
            date = date.plusDays(1);
        }
        return workdays;
    }

    /**
     * Predict when vacation days will run out based on current usage rate.
     * Returns null if no days have been used yet or if days won't run out.
     */
    public LocalDate predictExhaustionDate(int year) {
        var used = getUsedDays(year);
        var total = getTotalDaysPerYear();

        if (used == 0 || used >= total) {
            return null;
        }

        var startOfYear = LocalDate.of(year, 1, 1);
        var today = LocalDate.now();
        var daysPassed = ChronoUnit.DAYS.between(startOfYear, today);

        if (daysPassed <= 0) {
            return null;
        }

        // Rate = days used per calendar day
        double rate = (double) used / daysPassed;
        long daysRemaining = total - used;

        // Days until exhaustion at current rate
        long daysUntilExhaustion = (long) (daysRemaining / rate);
        var exhaustionDate = today.plusDays(daysUntilExhaustion);

        // Cap at end of year
        var endOfYear = LocalDate.of(year, 12, 31);
        return exhaustionDate.isAfter(endOfYear) ? null : exhaustionDate;
    }
}
