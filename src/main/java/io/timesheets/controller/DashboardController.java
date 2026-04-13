package io.timesheets.controller;

import io.timesheets.service.AdvCalculationService;
import io.timesheets.service.HolidayBalanceService;
import io.timesheets.service.OvertimeService;
import io.timesheets.service.TimeEntryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class DashboardController {

    private final HolidayBalanceService holidayBalanceService;
    private final AdvCalculationService advCalculationService;
    private final OvertimeService overtimeService;
    private final TimeEntryService timeEntryService;

    public DashboardController(HolidayBalanceService holidayBalanceService,
                               AdvCalculationService advCalculationService,
                               OvertimeService overtimeService,
                               TimeEntryService timeEntryService) {
        this.holidayBalanceService = holidayBalanceService;
        this.advCalculationService = advCalculationService;
        this.overtimeService = overtimeService;
        this.timeEntryService = timeEntryService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        var year = LocalDate.now().getYear();

        // Holiday balance
        model.addAttribute("vacationTotal", holidayBalanceService.getTotalDaysPerYear());
        model.addAttribute("vacationUsed", holidayBalanceService.getUsedDays(year));
        model.addAttribute("vacationRemaining", holidayBalanceService.getRemainingDays(year));
        model.addAttribute("vacationExhaustionDate", holidayBalanceService.predictExhaustionDate(year));

        // ADV balance
        model.addAttribute("advSurplusHours", advCalculationService.getSurplusHours(year));
        model.addAttribute("advEarned", advCalculationService.calculateEarnedAdvDays(year));
        model.addAttribute("advUsed", advCalculationService.calculateUsedAdvDays(year));
        model.addAttribute("advBalance", advCalculationService.calculateAdvBalance(year));
        model.addAttribute("advPredicted", advCalculationService.predictYearEndAdvDays(year));

        // Overtime per project
        var overtimeMap = overtimeService.calculateOvertimeForAllProjects(year);
        model.addAttribute("overtimePerProject", overtimeMap);
        model.addAttribute("totalOvertime", overtimeService.calculateTotalOvertime(year));

        // Today's entries
        var today = LocalDate.now();
        var todayEntries = timeEntryService.getEntriesForDate(today);
        var todayTotal = todayEntries.stream()
                .map(e -> e.getHoursWorked().add(e.getBreakDuration() != null ? e.getBreakDuration() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("todayEntries", todayEntries);
        model.addAttribute("todayTotal", todayTotal);

        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("currentYear", year);

        return "dashboard";
    }
}
