package io.timesheets.service;

import io.timesheets.dto.MonthlyProjectSummary;
import io.timesheets.dto.MonthlySummary;
import io.timesheets.model.LeaveType;
import io.timesheets.model.TimeEntry;
import io.timesheets.repository.LeaveEntryRepository;
import io.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TimeEntryRepository timeEntryRepository;
    private final LeaveEntryRepository leaveEntryRepository;
    private final OvertimeService overtimeService;

    public ReportService(TimeEntryRepository timeEntryRepository,
                         LeaveEntryRepository leaveEntryRepository,
                         OvertimeService overtimeService) {
        this.timeEntryRepository = timeEntryRepository;
        this.leaveEntryRepository = leaveEntryRepository;
        this.overtimeService = overtimeService;
    }

    public MonthlySummary getMonthlySummary(int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.with(TemporalAdjusters.lastDayOfMonth());
        var monthName = Month.of(month).getDisplayName(TextStyle.FULL, new Locale("nl", "BE"));

        var timeEntries = timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
        var leaveEntries = leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);

        // Total hours (net)
        var totalHours = timeEntries.stream()
                .map(TimeEntry::getHoursWorked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by project
        var byProject = timeEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getProject()));

        var projectSummaries = byProject.entrySet().stream()
                .map(entry -> {
                    var project = entry.getKey();
                    var entries = entry.getValue();
                    var hours = entries.stream()
                            .map(TimeEntry::getHoursWorked)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var days = entries.stream()
                            .map(TimeEntry::getEntryDate)
                            .distinct()
                            .count();
                    var overtime = overtimeService.calculateOvertimeForProject(project.getId(), start, end);

                    return new MonthlyProjectSummary(
                            project.getId(),
                            project.getName(),
                            project.isBillable(),
                            hours,
                            overtime,
                            days
                    );
                })
                .sorted(Comparator.comparing(MonthlyProjectSummary::projectName))
                .toList();

        // Billable vs non-billable
        var billableHours = projectSummaries.stream()
                .filter(MonthlyProjectSummary::billable)
                .map(MonthlyProjectSummary::totalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var nonBillableHours = totalHours.subtract(billableHours);

        // Overtime total
        var overtimeHours = projectSummaries.stream()
                .map(MonthlyProjectSummary::overtimeHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Work days (distinct dates with time entries)
        var workDays = timeEntries.stream()
                .map(TimeEntry::getEntryDate)
                .distinct()
                .count();

        // Leave counts
        var leaveByType = leaveEntries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getLeaveType().getDisplayName(),
                        Collectors.counting()
                ));

        var vacationDays = leaveEntries.stream()
                .filter(e -> e.getLeaveType() == LeaveType.VACATION)
                .count();
        var sickDays = leaveEntries.stream()
                .filter(e -> e.getLeaveType() == LeaveType.SICK)
                .count();
        var advDays = leaveEntries.stream()
                .filter(e -> e.getLeaveType() == LeaveType.ADV)
                .count();
        var otherLeaveDays = leaveEntries.size() - vacationDays - sickDays - advDays;

        return new MonthlySummary(
                year, month, monthName,
                totalHours, billableHours, nonBillableHours, overtimeHours,
                workDays, vacationDays, sickDays, advDays, otherLeaveDays,
                projectSummaries, leaveByType
        );
    }

    public List<MonthlySummary> getYearlySummary(int year) {
        var summaries = new ArrayList<MonthlySummary>();
        for (int month = 1; month <= 12; month++) {
            summaries.add(getMonthlySummary(year, month));
        }
        return summaries;
    }

    /**
     * Get all time entries for a date range, used by export.
     */
    public List<TimeEntry> getTimeEntriesForExport(LocalDate start, LocalDate end, Long projectId) {
        if (projectId != null) {
            return timeEntryRepository.findByProjectIdAndEntryDateBetween(projectId, start, end);
        }
        return timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
    }
}
