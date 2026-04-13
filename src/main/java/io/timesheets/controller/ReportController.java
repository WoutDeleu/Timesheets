package io.timesheets.controller;

import io.timesheets.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

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

    @GetMapping("/overtime")
    public String overtime(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Long projectId,
                           Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();
        var projects = projectService.getAllActiveProjects();

        // Default to first active project if none selected
        var selectedProjectId = projectId;
        if (selectedProjectId == null && !projects.isEmpty()) {
            selectedProjectId = projects.get(0).getId();
        }

        var chartLabelsJson = "[]";
        var chartDataJson = "[]";
        String selectedProjectName = null;

        if (selectedProjectId != null) {
            var start = LocalDate.of(selectedYear, 1, 1);
            var end = LocalDate.of(selectedYear, 12, 31);
            var evolution = overtimeService.calculateOvertimeEvolution(selectedProjectId, start, end);

            var dateFmt = DateTimeFormatter.ofPattern("dd/MM");
            chartLabelsJson = evolution.keySet().stream()
                    .map(d -> "\"" + d.format(dateFmt) + "\"")
                    .collect(Collectors.joining(",", "[", "]"));
            chartDataJson = evolution.values().stream()
                    .map(BigDecimal::toPlainString)
                    .collect(Collectors.joining(",", "[", "]"));

            // Find project name
            for (var p : projects) {
                if (p.getId().equals(selectedProjectId)) {
                    selectedProjectName = p.getName();
                    break;
                }
            }
        }

        model.addAttribute("currentPage", "reports");
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("projects", projects);
        model.addAttribute("selectedProjectId", selectedProjectId);
        model.addAttribute("selectedProjectName", selectedProjectName);
        model.addAttribute("chartLabelsJson", chartLabelsJson);
        model.addAttribute("chartDataJson", chartDataJson);

        return "reports/overtime";
    }

    @GetMapping("/export")
    public String exportForm(Model model) {
        model.addAttribute("currentPage", "reports");
        model.addAttribute("projects", projectService.getAllProjects());

        return "reports/export";
    }
}
