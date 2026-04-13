package be.axxes.timesheets.service;

import be.axxes.timesheets.model.LeaveType;
import be.axxes.timesheets.repository.LeaveEntryRepository;
import be.axxes.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;

import be.axxes.timesheets.model.LeaveEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;
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
    private final SettingsService settingsService;
    private final HolidayService holidayService;

    public AdvCalculationService(TimeEntryRepository timeEntryRepository,
                                 LeaveEntryRepository leaveEntryRepository,
                                 SettingsService settingsService,
                                 HolidayService holidayService) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.settingsService = settingsService;
        this.holidayService = holidayService;
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

    /**
     * Predict total ADV days earned by year-end.
     *
     * Assumes all remaining vacation days, ALL ADV days (including newly earned ones),
     * and sick days without doctor's note will be taken. These are 7.6h days and
     * do NOT contribute to ADV surplus.
     *
     * Holidays (national, company, compensatory) also do not contribute.
     *
     * All other remaining workdays are assumed to be 8h days contributing 0.4h surplus each.
     *
     * The calculation is circular: future workdays earn ADV, but those ADV days will also
     * be taken (reducing contributing workdays). Solved algebraically:
     *
     *   Let D = total predicted ADV days, C = contributing workdays
     *   C = W - V - S - (D - A_used)     [W=future workdays, V=vacation, S=sick, A_used=ADV already taken]
     *   D = (S_current + C * surplus) / advDayHours
     *
     *   Solving: D = (S_current + (W - V - S + A_used) * surplus) / (advDayHours + surplus)
     *   With defaults: D = (S_current + (W - V - S + A_used) * 0.4) / 8.0
     */
    public BigDecimal predictYearEndAdvDays(int year) {
        var today = LocalDate.now();
        var endOfYear = LocalDate.of(year, 12, 31);
        var surplusPerDay = settingsService.getAdvDailySurplusHours();
        var advDayHours = settingsService.getAdvDayHours();

        // Current surplus already accumulated
        var currentSurplus = calculateAccumulatedSurplusHours(year);

        // Collect all holiday dates
        var holidays = holidayService.getHolidaysForYear(year);
        var holidayDates = holidays.stream()
                .map(h -> h.getHolidayDate())
                .collect(Collectors.toSet());

        // Count future workdays (tomorrow onwards, excluding weekends and holidays)
        long futureWorkdays = 0;
        var date = today.plusDays(1);
        while (!date.isAfter(endOfYear)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !holidayDates.contains(date)) {
                futureWorkdays++;
            }
            date = date.plusDays(1);
        }

        // Remaining vacation days
        long vacationTotal = settingsService.getVacationDaysPerYear();
        long vacationUsed = leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(
                LeaveType.VACATION, LocalDate.of(year, 1, 1), endOfYear);
        long vacationRemaining = Math.max(0, vacationTotal - vacationUsed);

        // ADV days already taken this year
        long advUsed = calculateUsedAdvDays(year);

        // Remaining sick days without doctor's note
        long sickWithoutNoteTotal = settingsService.getSickDaysWithoutNotePerYear();
        long sickWithoutNoteUsed = leaveEntryRepository.countByLeaveTypeAndDoctorsNoteAndEntryDateBetween(
                LeaveType.SICK, false, LocalDate.of(year, 1, 1), endOfYear);
        long sickWithoutNoteRemaining = Math.max(0, sickWithoutNoteTotal - sickWithoutNoteUsed);

        // Algebraic solution accounting for the circular dependency:
        // newly earned ADV days will also be taken, reducing contributing workdays
        long availableDays = futureWorkdays - vacationRemaining - sickWithoutNoteRemaining + advUsed;
        var numerator = currentSurplus.add(surplusPerDay.multiply(BigDecimal.valueOf(Math.max(0, availableDays))));
        var denominator = advDayHours.add(surplusPerDay);

        return numerator.divide(denominator, 2, RoundingMode.FLOOR);
    }
}
