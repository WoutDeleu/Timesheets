package io.timesheets.dto;

import io.timesheets.model.WorkLocation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectDashboardWeekRow(
        int weekNumber,
        LocalDate monday,
        DayCell mondayCell,
        DayCell tuesdayCell,
        DayCell wednesdayCell,
        DayCell thursdayCell,
        DayCell fridayCell,
        BigDecimal weekTotal
) {
    public record DayCell(
            LocalDate date,
            BigDecimal hours,
            BigDecimal breakDuration,
            WorkLocation location,
            BigDecimal balance
    ) {}
}
