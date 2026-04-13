package io.timesheets.dto;

import java.time.LocalDate;

public record OverviewWeekRow(
        int weekNumber,
        LocalDate monday,
        DaySummary mondayCell,
        DaySummary tuesdayCell,
        DaySummary wednesdayCell,
        DaySummary thursdayCell,
        DaySummary fridayCell
) {}
