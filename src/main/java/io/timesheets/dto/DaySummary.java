package io.timesheets.dto;

import io.timesheets.model.Holiday;
import io.timesheets.model.LeaveEntry;
import io.timesheets.model.TimeEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DaySummary(
        LocalDate date,
        String dayName,
        String formattedDate,
        List<TimeEntry> timeEntries,
        LeaveEntry leaveEntry,
        Holiday holiday,
        BigDecimal totalGrossHours,
        BigDecimal dailyBalance,
        boolean isWeekend,
        boolean isHoliday,
        boolean isLeave,
        boolean isToday
) {}
