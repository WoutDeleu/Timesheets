package be.axxes.timesheets.service;

import be.axxes.timesheets.dto.ProjectDashboardSummary;
import be.axxes.timesheets.dto.ProjectDashboardWeekRow;
import be.axxes.timesheets.dto.ProjectDashboardWeekRow.DayCell;
import be.axxes.timesheets.model.LeaveEntry;
import be.axxes.timesheets.model.TimeEntry;
import be.axxes.timesheets.model.WorkLocation;
import be.axxes.timesheets.repository.LeaveEntryRepository;
import be.axxes.timesheets.repository.ProjectRepository;
import be.axxes.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectDashboardService {

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final OvertimeService overtimeService;
    private final SettingsService settingsService;

    public ProjectDashboardService(TimeEntryRepository timeEntryRepository,
                                   ProjectRepository projectRepository,
                                   LeaveEntryRepository leaveEntryRepository,
                                   OvertimeService overtimeService,
                                   SettingsService settingsService) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.overtimeService = overtimeService;
        this.settingsService = settingsService;
    }

    public ProjectDashboardSummary getDashboard(Long projectId, LocalDate startDate, LocalDate endDate) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + projectId));

        var entries = timeEntryRepository.findByProjectIdAndEntryDateBetween(projectId, startDate, endDate);

        // Aggregate per day: sum hours, pick dominant location
        var dailyAggregates = aggregateByDay(entries);

        // Compute statistics (net hours = gross - break)
        var totalHours = dailyAggregates.values().stream()
                .map(a -> a.hours().subtract(a.breakDuration() != null ? a.breakDuration() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalDays = dailyAggregates.size();

        var overtimeSaldo = overtimeService.calculateOvertimeForProject(projectId, startDate, endDate);

        var averageHoursPerDay = totalDays > 0
                ? totalHours.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long homeDays = dailyAggregates.values().stream()
                .filter(a -> a.location() == WorkLocation.HOME)
                .count();

        long officeDays = dailyAggregates.values().stream()
                .filter(a -> a.location() == WorkLocation.OFFICE)
                .count();

        var officePercentage = totalDays > 0
                ? BigDecimal.valueOf(officeDays * 100).divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Build weekly rows with per-day balance
        var dailyTarget = project.getDailyHourTarget() != null
                ? project.getDailyHourTarget()
                : settingsService.getDefaultDailyHours();
        var leaveHoursPerDay = leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(startDate, endDate).stream()
                .collect(Collectors.groupingBy(
                        LeaveEntry::getEntryDate,
                        Collectors.reducing(BigDecimal.ZERO, LeaveEntry::getHours, BigDecimal::add)
                ));
        var defaultDailyHours = settingsService.getDefaultDailyHours();
        var weekRows = buildWeekRows(startDate, endDate, dailyAggregates, dailyTarget, defaultDailyHours, leaveHoursPerDay);

        return new ProjectDashboardSummary(
                project.getId(),
                project.getName(),
                project.isBillable(),
                totalHours,
                totalDays,
                overtimeSaldo,
                averageHoursPerDay,
                homeDays,
                officeDays,
                officePercentage,
                weekRows
        );
    }

    private Map<LocalDate, DayAggregate> aggregateByDay(List<TimeEntry> entries) {
        var grouped = entries.stream()
                .collect(Collectors.groupingBy(TimeEntry::getEntryDate));

        var result = new LinkedHashMap<LocalDate, DayAggregate>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    var date = e.getKey();
                    var dayEntries = e.getValue();

                    // Use gross hours (net + break)
                    var totalHours = dayEntries.stream()
                            .map(te -> te.getHoursWorked().add(te.getBreakDuration() != null ? te.getBreakDuration() : BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    var totalBreak = dayEntries.stream()
                            .map(te -> te.getBreakDuration() != null ? te.getBreakDuration() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Pick location of the entry with the most hours
                    var dominantLocation = dayEntries.stream()
                            .max(Comparator.comparing(TimeEntry::getHoursWorked))
                            .map(TimeEntry::getWorkLocation)
                            .orElse(WorkLocation.OFFICE);

                    result.put(date, new DayAggregate(totalHours, totalBreak, dominantLocation));
                });
        return result;
    }

    private List<ProjectDashboardWeekRow> buildWeekRows(LocalDate startDate, LocalDate endDate,
                                                         Map<LocalDate, DayAggregate> dailyAggregates,
                                                         BigDecimal dailyTarget,
                                                         BigDecimal defaultDailyHours,
                                                         Map<LocalDate, BigDecimal> leaveHoursPerDay) {
        // Align to Monday boundaries
        var firstMonday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var lastFriday = endDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        var rows = new ArrayList<ProjectDashboardWeekRow>();
        var currentMonday = firstMonday;

        while (!currentMonday.isAfter(lastFriday)) {
            var cells = new DayCell[5];
            var weekTotal = BigDecimal.ZERO;
            var hasData = false;

            for (int i = 0; i < 5; i++) {
                var day = currentMonday.plusDays(i);
                var aggregate = dailyAggregates.get(day);
                if (aggregate != null) {
                    var netHours = aggregate.hours().subtract(aggregate.breakDuration() != null ? aggregate.breakDuration() : BigDecimal.ZERO);
                    var leaveHours = leaveHoursPerDay.getOrDefault(day, BigDecimal.ZERO);
                    var dayTarget = leaveHours.compareTo(BigDecimal.ZERO) > 0 ? defaultDailyHours : dailyTarget;
                    var effectiveTarget = dayTarget.subtract(leaveHours).max(BigDecimal.ZERO);
                    var balance = netHours.subtract(effectiveTarget);
                    cells[i] = new DayCell(day, aggregate.hours(), aggregate.breakDuration(), aggregate.location(), balance);
                    weekTotal = weekTotal.add(aggregate.hours());
                    hasData = true;
                } else {
                    cells[i] = new DayCell(day, null, null, null, null);
                }
            }

            if (hasData) {
                var weekNumber = currentMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                rows.add(new ProjectDashboardWeekRow(
                        weekNumber, currentMonday,
                        cells[0], cells[1], cells[2], cells[3], cells[4],
                        weekTotal
                ));
            }

            currentMonday = currentMonday.plusWeeks(1);
        }

        return rows;
    }

    private record DayAggregate(BigDecimal hours, BigDecimal breakDuration, WorkLocation location) {}
}
