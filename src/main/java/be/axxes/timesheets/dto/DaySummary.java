package be.axxes.timesheets.dto;

import be.axxes.timesheets.model.Holiday;
import be.axxes.timesheets.model.LeaveEntry;
import be.axxes.timesheets.model.TimeEntry;

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
