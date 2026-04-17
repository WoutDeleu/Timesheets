package io.timesheets.service;

import io.timesheets.model.LeaveEntry;
import io.timesheets.model.Project;
import io.timesheets.model.TimeEntry;
import io.timesheets.repository.LeaveEntryRepository;
import io.timesheets.repository.ProjectRepository;
import io.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates overtime saldo per project.
 *
 * Logic:
 * - Each project has an optional daily hour target
 * - If no target is set, the global default (7.6h) is used
 * - Hours exceeding the target on a given day = overtime for that day
 * - Overtime accumulates as a running saldo per project
 */
@Service
public class OvertimeService {

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final SettingsService settingsService;

    public OvertimeService(TimeEntryRepository timeEntryRepository,
                           ProjectRepository projectRepository,
                           LeaveEntryRepository leaveEntryRepository,
                           SettingsService settingsService) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.settingsService = settingsService;
    }

    /**
     * Calculate overtime saldo for a specific project within a date range.
     * Returns positive value = overtime accumulated, negative = undertime.
     * Returns ZERO for internal projects (their hours are saldo-neutral).
     */
    public BigDecimal calculateOvertimeForProject(Long projectId, LocalDate start, LocalDate end) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + projectId));

        if (project.isInternalProject()) {
            return BigDecimal.ZERO;
        }

        var dailyTarget = project.getDailyHourTarget() != null
                ? project.getDailyHourTarget()
                : settingsService.getDefaultDailyHours();

        var defaultDailyHours = settingsService.getDefaultDailyHours();
        var entries = timeEntryRepository.findByProjectIdAndEntryDateBetween(projectId, start, end);

        // Group by date, sum hours per day for this project
        var hoursPerDay = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEntryDate(),
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getHoursWorked(), BigDecimal::add)
                ));

        // Get leave hours per day to reduce the daily target
        var leaveHoursPerDay = getLeaveHoursPerDay(start, end);

        // Get internal project hours per day (they fill client deficits)
        var internalHoursPerDay = getInternalHoursPerDay(start, end);

        var totalOvertime = BigDecimal.ZERO;
        for (var entry : hoursPerDay.entrySet()) {
            var date = entry.getKey();
            var dailyHours = entry.getValue();
            var leaveHours = leaveHoursPerDay.getOrDefault(date, BigDecimal.ZERO);
            var dayTarget = leaveHours.compareTo(BigDecimal.ZERO) > 0 ? defaultDailyHours : dailyTarget;
            var effectiveTarget = dayTarget.subtract(leaveHours).max(BigDecimal.ZERO);

            BigDecimal overtime;
            if (dailyHours.compareTo(effectiveTarget) >= 0) {
                // Client hours cover or exceed target — internal hours irrelevant
                overtime = dailyHours.subtract(effectiveTarget);
            } else {
                // Internal hours fill the deficit but cannot create overtime
                var internalHours = internalHoursPerDay.getOrDefault(date, BigDecimal.ZERO);
                overtime = dailyHours.add(internalHours).subtract(effectiveTarget).min(BigDecimal.ZERO);
            }
            totalOvertime = totalOvertime.add(overtime);
        }

        return totalOvertime;
    }

    /**
     * Calculate overtime saldo for a project in the given year.
     */
    public BigDecimal calculateOvertimeForProjectInYear(Long projectId, int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return calculateOvertimeForProject(projectId, start, end);
    }

    /**
     * Calculate overtime saldo for all active projects in the given year.
     * Returns a map of project -> overtime hours.
     */
    public Map<Project, BigDecimal> calculateOvertimeForAllProjects(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        var projects = projectRepository.findByActiveTrueOrderByNameAsc();

        var result = new LinkedHashMap<Project, BigDecimal>();
        for (var project : projects) {
            var overtime = calculateOvertimeForProject(project.getId(), start, end);
            result.put(project, overtime);
        }
        return result;
    }

    /**
     * Calculate total overtime across all projects in the year.
     */
    public BigDecimal calculateTotalOvertime(int year) {
        return calculateOvertimeForAllProjects(year).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total leave hours per day for a date range.
     */
    private Map<LocalDate, BigDecimal> getLeaveHoursPerDay(LocalDate start, LocalDate end) {
        return leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end).stream()
                .collect(Collectors.groupingBy(
                        LeaveEntry::getEntryDate,
                        Collectors.reducing(BigDecimal.ZERO, LeaveEntry::getHours, BigDecimal::add)
                ));
    }

    /**
     * Get total internal project hours per day for a date range.
     * Internal hours fill client project deficits but cannot create client overtime.
     */
    private Map<LocalDate, BigDecimal> getInternalHoursPerDay(LocalDate start, LocalDate end) {
        return timeEntryRepository.findByProjectInternalProjectTrueAndEntryDateBetween(start, end).stream()
                .collect(Collectors.groupingBy(
                        TimeEntry::getEntryDate,
                        Collectors.reducing(BigDecimal.ZERO,
                                e -> e.getHoursWorked() != null ? e.getHoursWorked() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));
    }

    /**
     * Calculate running overtime evolution for a project within a date range.
     * Returns a map of date -> cumulative overtime up to that date.
     * Only dates that have time entries are included.
     * Returns empty map for internal projects.
     */
    public LinkedHashMap<LocalDate, BigDecimal> calculateOvertimeEvolution(Long projectId, LocalDate start, LocalDate end) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + projectId));

        if (project.isInternalProject()) {
            return new LinkedHashMap<>();
        }

        var dailyTarget = project.getDailyHourTarget() != null
                ? project.getDailyHourTarget()
                : settingsService.getDefaultDailyHours();

        var defaultDailyHours = settingsService.getDefaultDailyHours();
        var entries = timeEntryRepository.findByProjectIdAndEntryDateBetween(projectId, start, end);

        var hoursPerDay = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEntryDate(),
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getHoursWorked(), BigDecimal::add)
                ));

        // Get leave hours per day to reduce the daily target
        var leaveHoursPerDay = getLeaveHoursPerDay(start, end);

        // Get internal project hours per day
        var internalHoursPerDay = getInternalHoursPerDay(start, end);

        // Sort dates and build running total
        var sortedDates = hoursPerDay.keySet().stream().sorted().toList();
        var result = new LinkedHashMap<LocalDate, BigDecimal>();
        var runningTotal = BigDecimal.ZERO;
        for (var date : sortedDates) {
            var leaveHours = leaveHoursPerDay.getOrDefault(date, BigDecimal.ZERO);
            var dayTarget = leaveHours.compareTo(BigDecimal.ZERO) > 0 ? defaultDailyHours : dailyTarget;
            var effectiveTarget = dayTarget.subtract(leaveHours).max(BigDecimal.ZERO);
            var dailyHours = hoursPerDay.get(date);

            BigDecimal dailyOvertime;
            if (dailyHours.compareTo(effectiveTarget) >= 0) {
                dailyOvertime = dailyHours.subtract(effectiveTarget);
            } else {
                var internalHours = internalHoursPerDay.getOrDefault(date, BigDecimal.ZERO);
                dailyOvertime = dailyHours.add(internalHours).subtract(effectiveTarget).min(BigDecimal.ZERO);
            }
            runningTotal = runningTotal.add(dailyOvertime);
            result.put(date, runningTotal);
        }
        return result;
    }
}
