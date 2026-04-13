package be.axxes.timesheets.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectDashboardSummary(
        Long projectId,
        String projectName,
        boolean billable,
        BigDecimal totalHours,
        long totalDays,
        BigDecimal overtimeSaldo,
        BigDecimal averageHoursPerDay,
        long homeDays,
        long officeDays,
        BigDecimal officePercentage,
        List<ProjectDashboardWeekRow> weekRows
) {}
