package io.timesheets.dto;

import java.math.BigDecimal;

public record MonthlyProjectSummary(
        Long projectId,
        String projectName,
        boolean billable,
        BigDecimal totalHours,
        BigDecimal overtimeHours,
        long daysWorked
) {}
