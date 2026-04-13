package io.timesheets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OverviewPeriod(
        String mode,
        String periodLabel,
        int weekNumber,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate prevParam,
        LocalDate nextParam,
        List<DaySummary> days,
        List<OverviewWeekRow> weekRows,
        BigDecimal periodTotalGross,
        BigDecimal periodBalance
) {}
