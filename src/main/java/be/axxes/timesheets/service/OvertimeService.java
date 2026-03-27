package be.axxes.timesheets.service;

import be.axxes.timesheets.model.Project;
import be.axxes.timesheets.repository.ProjectRepository;
import be.axxes.timesheets.repository.TimeEntryRepository;
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
    private final SettingsService settingsService;

    public OvertimeService(TimeEntryRepository timeEntryRepository,
                           ProjectRepository projectRepository,
                           SettingsService settingsService) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
        this.settingsService = settingsService;
    }

    /**
     * Calculate overtime saldo for a specific project within a date range.
     * Returns positive value = overtime accumulated, negative = undertime.
     */
    public BigDecimal calculateOvertimeForProject(Long projectId, LocalDate start, LocalDate end) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + projectId));

        var dailyTarget = project.getDailyHourTarget() != null
                ? project.getDailyHourTarget()
                : settingsService.getDefaultDailyHours();

        var entries = timeEntryRepository.findByProjectIdAndEntryDateBetween(projectId, start, end);

        // Group by date, sum hours per day for this project
        var hoursPerDay = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEntryDate(),
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getHoursWorked(), BigDecimal::add)
                ));

        var totalOvertime = BigDecimal.ZERO;
        for (var dailyHours : hoursPerDay.values()) {
            var overtime = dailyHours.subtract(dailyTarget);
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
}
