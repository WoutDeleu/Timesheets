package be.axxes.timesheets.controller;

import be.axxes.timesheets.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final HolidayBalanceService holidayBalanceService;
    private final AdvCalculationService advCalculationService;
    private final OvertimeService overtimeService;
    private final ProjectService projectService;

    public ReportController(ReportService reportService,
                            HolidayBalanceService holidayBalanceService,
                            AdvCalculationService advCalculationService,
                            OvertimeService overtimeService,
                            ProjectService projectService) {
        this.reportService = reportService;
        this.holidayBalanceService = holidayBalanceService;
        this.advCalculationService = advCalculationService;
        this.overtimeService = overtimeService;
        this.projectService = projectService;
    }

    @GetMapping
    public String reports(@RequestParam(required = false) Integer year, Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();

        model.addAttribute("currentPage", "reports");
        model.addAttribute("selectedYear", selectedYear);

        return "reports/index";
    }

    @GetMapping("/monthly")
    public String monthly(@RequestParam(required = false) Integer year,
                          @RequestParam(required = false) Integer month,
                          Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();
        var selectedMonth = month != null ? month : LocalDate.now().getMonthValue();

        var summary = reportService.getMonthlySummary(selectedYear, selectedMonth);

        model.addAttribute("currentPage", "reports");
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("summary", summary);

        return "reports/monthly";
    }

    @GetMapping("/yearly")
    public String yearly(@RequestParam(required = false) Integer year, Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();

        var monthlySummaries = reportService.getYearlySummary(selectedYear);

        // Saldi
        model.addAttribute("currentPage", "reports");
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("monthlySummaries", monthlySummaries);
        model.addAttribute("vacationUsed", holidayBalanceService.getUsedDays(selectedYear));
        model.addAttribute("vacationRemaining", holidayBalanceService.getRemainingDays(selectedYear));
        model.addAttribute("advEarned", advCalculationService.calculateEarnedAdvDays(selectedYear));
        model.addAttribute("advUsed", advCalculationService.calculateUsedAdvDays(selectedYear));
        model.addAttribute("advBalance", advCalculationService.calculateAdvBalance(selectedYear));
        model.addAttribute("totalOvertime", overtimeService.calculateTotalOvertime(selectedYear));

        return "reports/yearly";
    }

    @GetMapping("/export")
    public String exportForm(Model model) {
        model.addAttribute("currentPage", "reports");
        model.addAttribute("projects", projectService.getAllProjects());

        return "reports/export";
    }
}
