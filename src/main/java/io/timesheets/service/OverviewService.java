package io.timesheets.service;

import io.timesheets.dto.DaySummary;
import io.timesheets.dto.OverviewPeriod;
import io.timesheets.dto.OverviewWeekRow;
import io.timesheets.model.Holiday;
import io.timesheets.model.LeaveEntry;
import io.timesheets.model.TimeEntry;
import io.timesheets.repository.HolidayRepository;
import io.timesheets.repository.LeaveEntryRepository;
import io.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OverviewService {

    private static final DateTimeFormatter DAY_NAME_FMT = DateTimeFormatter.ofPattern("EE", new Locale("nl", "BE"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] MONTH_NAMES = {
            "", "januari", "februari", "maart", "april", "mei", "juni",
            "juli", "augustus", "september", "oktober", "november", "december"
    };

    private final TimeEntryRepository timeEntryRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final HolidayRepository holidayRepository;
    private final SettingsService settingsService;

    public OverviewService(TimeEntryRepository timeEntryRepository,
                           LeaveEntryRepository leaveEntryRepository,
                           HolidayRepository holidayRepository,
                           SettingsService settingsService) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.holidayRepository = holidayRepository;
        this.settingsService = settingsService;
    }

    public OverviewPeriod getWeeklyOverview(LocalDate anyDate) {
        var monday = anyDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var friday = monday.plusDays(4);
        var weekNumber = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        var label = "Week " + weekNumber + " — " + MONTH_NAMES[monday.getMonthValue()] + " " + monday.getYear();

        return buildOverview("week", monday, friday, label, weekNumber,
                monday.minusWeeks(1), monday.plusWeeks(1));
    }

    public OverviewPeriod getMonthlyOverview(int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.with(TemporalAdjusters.lastDayOfMonth());
        var weekNumber = start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        var label = MONTH_NAMES[month].substring(0, 1).toUpperCase() + MONTH_NAMES[month].substring(1) + " " + year;

        var prevMonth = start.minusMonths(1);
        var nextMonth = start.plusMonths(1);

        return buildOverview("month", start, end, label, weekNumber, prevMonth, nextMonth);
    }

    private OverviewPeriod buildOverview(String mode, LocalDate start, LocalDate end,
                                          String label, int weekNumber,
                                          LocalDate prevParam, LocalDate nextParam) {
        var timeEntries = timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
        var leaveEntries = leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
        var holidays = holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end);

        var entriesByDate = timeEntries.stream()
                .collect(Collectors.groupingBy(TimeEntry::getEntryDate));
        var leaveByDate = leaveEntries.stream()
                .collect(Collectors.toMap(LeaveEntry::getEntryDate, e -> e, (a, b) -> a));
        var holidayByDate = holidays.stream()
                .collect(Collectors.toMap(Holiday::getHolidayDate, h -> h, (a, b) -> a));

        var today = LocalDate.now();
        var days = new ArrayList<DaySummary>();
        var periodTotalGross = BigDecimal.ZERO;
        var periodBalance = BigDecimal.ZERO;

        var date = start;
        while (!date.isAfter(end)) {
            var dayEntries = entriesByDate.getOrDefault(date, List.of());
            var leave = leaveByDate.get(date);
            var holiday = holidayByDate.get(date);
            var isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            var grossHours = computeGross(dayEntries);
            var balance = isWeekend ? BigDecimal.ZERO : computeDailyBalance(dayEntries);

            days.add(new DaySummary(
                    date,
                    DAY_NAME_FMT.format(date),
                    DATE_FMT.format(date),
                    dayEntries,
                    leave,
                    holiday,
                    grossHours,
                    balance,
                    isWeekend,
                    holiday != null,
                    leave != null,
                    date.equals(today)
            ));

            periodTotalGross = periodTotalGross.add(grossHours);
            if (!isWeekend) {
                periodBalance = periodBalance.add(balance);
            }

            date = date.plusDays(1);
        }

        var weekRows = "month".equals(mode) ? buildWeekRows(days) : List.<OverviewWeekRow>of();

        return new OverviewPeriod(mode, label, weekNumber, start, end,
                prevParam, nextParam, days, weekRows, periodTotalGross, periodBalance);
    }

    private List<OverviewWeekRow> buildWeekRows(List<DaySummary> days) {
        // Index days by date
        var daysByDate = new LinkedHashMap<LocalDate, DaySummary>();
        for (var day : days) {
            daysByDate.put(day.date(), day);
        }

        // Find the Monday of the first week and Friday of the last week
        var firstDate = days.get(0).date();
        var lastDate = days.get(days.size() - 1).date();
        var firstMonday = firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var lastFriday = lastDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        var rows = new ArrayList<OverviewWeekRow>();
        var monday = firstMonday;
        while (!monday.isAfter(lastFriday)) {
            var cells = new DaySummary[5];
            for (int i = 0; i < 5; i++) {
                var d = monday.plusDays(i);
                cells[i] = daysByDate.getOrDefault(d, emptyDay(d));
            }
            rows.add(new OverviewWeekRow(
                    monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                    monday,
                    cells[0], cells[1], cells[2], cells[3], cells[4]
            ));
            monday = monday.plusWeeks(1);
        }
        return rows;
    }

    private DaySummary emptyDay(LocalDate date) {
        var isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return new DaySummary(date, DAY_NAME_FMT.format(date), DATE_FMT.format(date),
                List.of(), null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                isWeekend, false, false, date.equals(LocalDate.now()));
    }

    private BigDecimal computeGross(List<TimeEntry> entries) {
        return entries.stream()
                .map(e -> e.getHoursWorked()
                        .add(e.getBreakDuration() != null ? e.getBreakDuration() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeDailyBalance(List<TimeEntry> entries) {
        var defaultTarget = settingsService.getDefaultDailyHours();
        var projectNetHours = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getProject().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                e -> e.getHoursWorked() != null ? e.getHoursWorked() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));

        var balance = BigDecimal.ZERO;
        for (var entry : projectNetHours.entrySet()) {
            var pid = entry.getKey();
            var netHours = entry.getValue();
            var project = entries.stream()
                    .filter(e -> e.getProject().getId().equals(pid))
                    .findFirst().get().getProject();
            var target = project.getDailyHourTarget() != null
                    ? project.getDailyHourTarget()
                    : defaultTarget;
            balance = balance.add(netHours.subtract(target));
        }
        return balance;
    }
}
