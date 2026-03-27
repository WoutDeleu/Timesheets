package be.axxes.timesheets.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MonthlySummary(
        int year,
        int month,
        String monthName,
        BigDecimal totalHours,
        BigDecimal billableHours,
        BigDecimal nonBillableHours,
        BigDecimal overtimeHours,
        BigDecimal internalActivityHours,
        long workDays,
        long vacationDays,
        long sickDays,
        long advDays,
        long otherLeaveDays,
        List<MonthlyProjectSummary> projectSummaries,
        Map<String, Long> leaveByType
) {}
