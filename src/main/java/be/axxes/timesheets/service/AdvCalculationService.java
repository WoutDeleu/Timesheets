package be.axxes.timesheets.service;

import be.axxes.timesheets.model.LeaveType;
import be.axxes.timesheets.repository.InternalActivityRepository;
import be.axxes.timesheets.repository.LeaveEntryRepository;
import be.axxes.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Calculates ADV (Arbeidsduurvermindering) balance.
 *
 * Logic:
 * - Standard workday = 7.6h (configurable)
 * - When a full 8h is worked on a day, 0.4h surplus goes to the ADV pot
 * - 1 ADV day = 7.6h accumulated surplus
 * - Sick days and leave days do NOT contribute to ADV
 * - Only actual workdays where total hours >= 8h count
 */
@Service
public class AdvCalculationService {

    private static final BigDecimal EIGHT_HOURS = new BigDecimal("8.0");

    private final TimeEntryRepository timeEntryRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final InternalActivityRepository internalActivityRepository;
    private final SettingsService settingsService;

    public AdvCalculationService(TimeEntryRepository timeEntryRepository,
                                 LeaveEntryRepository leaveEntryRepository,
                                 InternalActivityRepository internalActivityRepository,
                                 SettingsService settingsService) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.internalActivityRepository = internalActivityRepository;
        this.settingsService = settingsService;
    }

    /**
     * Calculate the total ADV surplus hours accumulated in the given year.
     * Only days where total hours worked across all projects >= 8h contribute.
     */
    public BigDecimal calculateAccumulatedSurplusHours(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        var surplusPerDay = settingsService.getAdvDailySurplusHours(); // 0.4h

        var entries = timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);

        // Group by date, sum hours per day
        var hoursPerDay = new HashMap<>(entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEntryDate(),
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getHoursWorked(), BigDecimal::add)
                )));

        // Include internal activity hours in daily totals
        var internalActivities = internalActivityRepository
                .findByActivityDateBetweenOrderByActivityDateAsc(start, end);
        for (var activity : internalActivities) {
            hoursPerDay.merge(activity.getActivityDate(), activity.getHours(), BigDecimal::add);
        }

        // Get leave dates — these don't contribute
        var leaveDates = leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end)
                .stream()
                .map(e -> e.getEntryDate())
                .collect(Collectors.toSet());

        var totalSurplus = BigDecimal.ZERO;

        for (var entry : hoursPerDay.entrySet()) {
            var date = entry.getKey();
            var totalHours = entry.getValue();

            // Skip weekends
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            // Skip leave days
            if (leaveDates.contains(date)) {
                continue;
            }

            // Only count days where >= 8h was worked
            if (totalHours.compareTo(EIGHT_HOURS) >= 0) {
                totalSurplus = totalSurplus.add(surplusPerDay);
            }
        }

        return totalSurplus;
    }

    /**
     * Calculate total ADV days earned (surplus hours / 7.6h per ADV day).
     */
    public BigDecimal calculateEarnedAdvDays(int year) {
        var surplusHours = calculateAccumulatedSurplusHours(year);
        var advDayHours = settingsService.getAdvDayHours(); // 7.6h
        return surplusHours.divide(advDayHours, 2, RoundingMode.FLOOR);
    }

    /**
     * Calculate ADV days taken in the year.
     */
    public long calculateUsedAdvDays(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(LeaveType.ADV, start, end);
    }

    /**
     * Calculate remaining ADV balance (earned - used).
     */
    public BigDecimal calculateAdvBalance(int year) {
        var earned = calculateEarnedAdvDays(year);
        var used = BigDecimal.valueOf(calculateUsedAdvDays(year));
        return earned.subtract(used);
    }

    /**
     * Get the raw surplus hours accumulated (for display).
     */
    public BigDecimal getSurplusHours(int year) {
        return calculateAccumulatedSurplusHours(year);
    }
}
